package com.cmbchina.cs.assitsvc.asr;

import com.cmbchina.cs.assitsvc.core.intent.IntentRecognitionTrigger;
import com.cmbchina.cs.assitsvc.domain.AsrSentenceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * ASR 句子固定窗口触发器（MVP 简化方案）。
 *
 * <p>触发策略：
 * <ul>
 *   <li>同一 callId 收到第一条客户句子 → setIfAbsent 开窗 + zAdd 排队延迟任务；</li>
 *   <li>窗口期内的新客户句子 → 窗口已存在，本类不做任何操作（句子已由 M03 写入历史）；</li>
 *   <li>窗口到期 → 多 Pod polling，ZREM 抢占成功的 Pod 校验 round 后 fire；</li>
 *   <li>fire 成功后将 stateKey 的 TTL 缩短至 100ms（不使用 DEL，遵守工程规范），
 *       让下一条客户句子能在毫秒级开启新窗口；</li>
 *   <li>cleanup 仅打日志，不写标记。通话结束后已入队的任务到期会 fire 一次，
 *       主链路因 session 已 cleanup 会回退到 SESSION_BIND_MISSING 失败路径，业务无副作用。</li>
 * </ul>
 *
 * <p>Redis 集群一致性：
 * <ul>
 *   <li>stateKey 的 hashtag 取 callId，按通话分散到所有 slot 避免热点；</li>
 *   <li>DUE_QUEUE_KEY 是全集群共享的单 ZSET，固有单 slot，本期容量足够；
 *       后期高并发再考虑分片。</li>
 *   <li>单 callId 的 Kafka 消息按 key 路由到同一分区；
 *       多 Pod 间通过 ZREM 抢占保证同一 round 只 fire 一次。</li>
 * </ul>
 *
 * <p>已知 trade-off（MVP 阶段可接受）：
 * <ul>
 *   <li><b>窗口哑火</b>：setIfAbsent 成功但 zAdd 失败时本轮不触发，
 *       stateKey TTL 过期后下一条句子可正常开窗；</li>
 *   <li><b>多余 fire</b>：cleanup 与 fire 竞态时极小概率多 fire 一次，
 *       但 session 已失效，主链路自然回退失败，仅多一条日志记录。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SentenceMerger {

    // hashtag 取 callId，避免集群单 slot 热点
    private static final String STATE_KEY_PREFIX = "copilot:asr_merge:state:{";
    private static final String STATE_KEY_SUFFIX = "}";
    // fire 失败冷却 key，与 stateKey 同 callId hashtag，确保同 slot 便于运维
    private static final String COOLDOWN_KEY_PREFIX = "copilot:asr_merge:cooldown:{";
    private static final String COOLDOWN_KEY_SUFFIX = "}";
    // 单 ZSET 全集群共享（多通话共用），固有单 slot；后期高并发再考虑分片
    private static final String DUE_QUEUE_KEY = "copilot:asr_merge:due";
    private static final String EVENT_DELIMITER = "\u001F";
    private static final String WINDOW_SOURCE = "WINDOW";
    /** state key 在窗口长度之外多保留的时间，作为异常路径的兜底过期。 */
    private static final long STATE_TTL_BUFFER_MS = 10 * 60 * 1000L;
    /** fire 成功后将 stateKey 改为该 TTL，让下一条客户句子可立即开新窗口。 */
    private static final Duration POST_FIRE_TTL = Duration.ofMillis(100);

    private final IntentTriggerProperties props;
    private final IntentRecognitionTrigger trigger;
    private final StringRedisTemplate redisTemplate;

    /**
     * 处理一条客户 ASR 句子。
     *
     * <p>拆两步：
     * <ol>
     *   <li>setIfAbsent stateKey：原子尝试开窗，已存在则放弃；</li>
     *   <li>仅在第 1 步成功时 zAdd 排队到期任务。</li>
     * </ol>
     * 第 1 步成功但第 2 步失败时，stateKey 仍占位至 TTL 过期，期间本通话无新窗口（哑火，可接受）。
     *
     * @param event ASR 事件，callId 不可为空
     */
    public void handleSentence(AsrSentenceEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (!StringUtils.hasText(event.getCallId())) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }

        String callId = event.getCallId();
        long windowMs = Math.max(0L, props.getWindowMs());
        long now = System.currentTimeMillis();
        String roundId = nextRoundId();

        // 检查 fire 失败冷却：上一轮 fire 抛异常时设置；冷却期内不开新窗口避免对故障下游持续打
        if (isInCooldown(callId)) {
            log.debug("[M02] Call in fire-failure cooldown, skip window opening, callId={}", callId);
            return;
        }

        // 步骤 1：原子尝试开窗
        Boolean opened;
        try {
            opened = redisTemplate.opsForValue().setIfAbsent(
                    stateKey(callId),
                    roundId,
                    Duration.ofMillis(windowMs + STATE_TTL_BUFFER_MS));
        } catch (DataAccessException e) {
            log.warn("[M02] Redis setIfAbsent failed, callId={}", callId, e);
            return;
        }

        if (!Boolean.TRUE.equals(opened)) {
            log.debug("[M02] Window already active, sentence merged into history only, callId={}",
                    callId);
            return;
        }

        // 步骤 2：排队延迟任务
        String member = eventMember(callId, roundId, WINDOW_SOURCE);
        try {
            redisTemplate.opsForZSet().add(DUE_QUEUE_KEY, member, (double) (now + windowMs));
            log.debug("[M02] Window opened, callId={}, roundId={}, windowMs={}",
                    callId, roundId, windowMs);
        } catch (DataAccessException e) {
            log.warn("[M02] Redis zAdd failed (window will silently expire), callId={}, roundId={}",
                    callId, roundId, e);
        }
    }

    /**
     * 通话结束时调用。
     *
     * <p><b>行内规范</b>：Redis 不使用 delete 等阻塞命令，临时数据完全依赖 TTL 自动过期清理。
     * 本方法仅保留日志和方法签名，作为通话生命周期事件钩子。
     *
     * <p>通话结束后若有已入队任务到期，主链路会因 session 已 cleanup 走 SESSION_BIND_MISSING
     * 失败分支，仅多一条日志，业务无副作用。
     */
    public void cleanup(String callId) {
        if (!StringUtils.hasText(callId)) {
            log.debug("[M02] Cleanup skipped on blank callId");
            return;
        }
        log.debug("[M02] Cleanup invoked, relying on TTL expiration, callId={}", callId);
    }

    /**
     * 多 Pod 共享轮询延迟队列，ZREM 原子抢占成功的 Pod 校验 round 后触发意图识别。
     */
    @Scheduled(fixedDelayString = "${copilot.intent-trigger.polling-interval-ms:200}")
    public void pollDueTasks() {
        Set<String> dueMembers;
        try {
            dueMembers = redisTemplate.opsForZSet().rangeByScore(
                    DUE_QUEUE_KEY,
                    0,
                    System.currentTimeMillis(),
                    0,
                    Math.max(1, props.getPollingBatchSize()));
        } catch (DataAccessException e) {
            log.warn("[M02] Redis intent trigger poll failed", e);
            return;
        }
        if (dueMembers == null || dueMembers.isEmpty()) {
            return;
        }

        for (String member : dueMembers) {
            processDueTask(member);
        }
    }

    private void processDueTask(String member) {
        TimerEvent event = parseEvent(member);
        if (event == null) {
            removeInvalidTask(member);
            return;
        }
        if (!claimTask(member, event)) {
            return;
        }
        try {
            log.debug("[M02] Intent recognition triggered, callId={}, roundId={}",
                    event.getCallId(), event.getRoundId());
            trigger.fire(event.getCallId());
        } catch (Exception e) {
            log.error("[M02] Intent recognition trigger failed, callId={}, roundId={}",
                    event.getCallId(), event.getRoundId(), e);
            // 进入冷却，避免对故障 AI 5 秒一次持续打
            setCooldown(event.getCallId());
        }
    }

    /**
     * 抢占到期任务：拆三步实现。
     * <ol>
     *   <li>ZREM 抢占任务：仅一个 Pod 能成功移除；</li>
     *   <li>GET stateKey 校验 round：仍是本 round 才返回 true；</li>
     *   <li>校验通过后将 stateKey TTL 缩短到 100ms：让下一条客户句子能毫秒级开新窗口。
     *       不使用 DEL 指令，遵守工程规范（Redis 临时数据依赖 TTL 自动过期）。</li>
     * </ol>
     * <p>第 3 步 EXPIRE 失败时本轮 fire 仍照常发生，stateKey 由长 TTL 兜底过期，
     * 最坏后果是本通话冷却 windowMs + buffer 才能开新窗口（10 分钟级，但生产中 EXPIRE 几乎不会失败）。
     */
    private boolean claimTask(String member, TimerEvent event) {
        Long removed;
        try {
            removed = redisTemplate.opsForZSet().remove(DUE_QUEUE_KEY, member);
        } catch (DataAccessException e) {
            log.warn("[M02] Redis ZREM failed, member={}", member, e);
            return false;
        }
        if (removed == null || removed == 0L) {
            return false;
        }

        String current;
        try {
            current = redisTemplate.opsForValue().get(stateKey(event.getCallId()));
        } catch (DataAccessException e) {
            log.warn("[M02] Redis GET stateKey failed, callId={}", event.getCallId(), e);
            return false;
        }
        if (current == null || !event.getRoundId().equals(current)) {
            // stateKey 已 TTL 过期或被其他流程改写：丢弃本任务
            log.debug("[M02] Stale round, skip fire, callId={}, roundId={}",
                    event.getCallId(), event.getRoundId());
            return false;
        }

        // 缩短 TTL 让 stateKey 快速过期，允许下一条客户句子开新窗口
        try {
            redisTemplate.expire(stateKey(event.getCallId()),
                    POST_FIRE_TTL.toMillis(), TimeUnit.MILLISECONDS);
        } catch (DataAccessException e) {
            log.warn("[M02] Redis EXPIRE stateKey failed (window will expire by long TTL), callId={}",
                    event.getCallId(), e);
        }
        return true;
    }

    private void removeInvalidTask(String member) {
        try {
            redisTemplate.opsForZSet().remove(DUE_QUEUE_KEY, member);
        } catch (DataAccessException e) {
            log.warn("[M02] Redis invalid task cleanup failed, member={}", member, e);
        }
    }

    private static String nextRoundId() {
        return System.currentTimeMillis() + "-" + UUID.randomUUID().toString().replace("-", "");
    }

    private static String stateKey(String callId) {
        return STATE_KEY_PREFIX + callId + STATE_KEY_SUFFIX;
    }

    private static String cooldownKey(String callId) {
        return COOLDOWN_KEY_PREFIX + callId + COOLDOWN_KEY_SUFFIX;
    }

    private boolean isInCooldown(String callId) {
        long cooldownMs = props.getFireFailureCooldownMs();
        if (cooldownMs <= 0L) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey(callId)));
        } catch (DataAccessException e) {
            log.warn("[M02] Cooldown check failed (fail open), callId={}", callId, e);
            return false;
        }
    }

    private void setCooldown(String callId) {
        long cooldownMs = props.getFireFailureCooldownMs();
        if (cooldownMs <= 0L) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(cooldownKey(callId), "1",
                    cooldownMs, TimeUnit.MILLISECONDS);
        } catch (DataAccessException e) {
            log.warn("[M02] Set cooldown failed, callId={}", callId, e);
        }
    }

    private static String eventMember(String callId, String roundId, String source) {
        return callId + EVENT_DELIMITER + roundId + EVENT_DELIMITER + source;
    }

    private static TimerEvent parseEvent(String member) {
        if (!StringUtils.hasText(member)) {
            return null;
        }
        String[] parts = member.split(EVENT_DELIMITER, 3);
        if (parts.length != 3
                || !StringUtils.hasText(parts[0])
                || !StringUtils.hasText(parts[1])
                || !StringUtils.hasText(parts[2])) {
            return null;
        }
        return new TimerEvent(parts[0], parts[1], parts[2]);
    }

    private static class TimerEvent {
        private final String callId;
        private final String roundId;
        private final String source;

        private TimerEvent(String callId, String roundId, String source) {
            this.callId = callId;
            this.roundId = roundId;
            this.source = source;
        }

        private String getCallId() {
            return callId;
        }

        private String getRoundId() {
            return roundId;
        }

        @SuppressWarnings("unused")
        private String getSource() {
            return source;
        }
    }
}
