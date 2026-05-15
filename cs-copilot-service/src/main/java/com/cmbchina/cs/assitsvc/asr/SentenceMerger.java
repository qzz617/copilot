package com.cmbchina.cs.assitsvc.asr;

import com.cmbchina.cs.assitsvc.core.intent.IntentRecognitionTrigger;
import com.cmbchina.cs.assitsvc.domain.AsrSentenceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * 句间合并防抖器。
 *
 * <p>同一 callId 的新客户句子到达时，会重置防抖 timer 和沉默 timer。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SentenceMerger {

    private static final String STATE_KEY_PREFIX = "copilot:{asr_merge}:state:";
    private static final String DUE_QUEUE_KEY = "copilot:{asr_merge}:due";
    private static final String EVENT_DELIMITER = "\u001F";
    private static final long STATE_TTL_BUFFER_MS = 10 * 60 * 1000L;
    private static final DefaultRedisScript<Long> CLAIM_TASK_SCRIPT = new DefaultRedisScript<>(
            "local removed = redis.call('ZREM', KEYS[1], ARGV[1]); "
                    + "if removed == 0 then return 0; end; "
                    + "if redis.call('GET', KEYS[2]) ~= ARGV[2] then return 0; end; "
                    + "redis.call('DEL', KEYS[2]); "
                    + "return 1;",
            Long.class);

    private final DebounceProperties props;
    private final SentenceContinuityDetector detector;
    private final IntentRecognitionTrigger trigger;
    private final StringRedisTemplate redisTemplate;

    @Value("${copilot.debounce.polling-batch-size:100}")
    private int pollingBatchSize;

    /**
     * 处理一条可触发意图识别的客户 ASR 句子。
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
        SentenceContinuity continuity = detector.detect(event.getContent());
        long debounceMs = debounceMs(continuity);
        long silenceMs = Math.max(0L, props.getSilenceMs());
        String roundId = nextRoundId();
        long now = System.currentTimeMillis();
        long ttlMs = Math.max(debounceMs, silenceMs) + STATE_TTL_BUFFER_MS;

        try {
            redisTemplate.opsForValue().set(stateKey(callId), roundId, ttlMs, TimeUnit.MILLISECONDS);
            redisTemplate.opsForZSet().add(DUE_QUEUE_KEY, eventMember(callId, roundId, "DEBOUNCE"), now + debounceMs);
            redisTemplate.opsForZSet().add(DUE_QUEUE_KEY, eventMember(callId, roundId, "SILENCE"), now + silenceMs);
        } catch (DataAccessException e) {
            log.warn("[M02] Redis sentence timer reset failed, callId={}", callId, e);
            return;
        }

        log.debug("[M02] Distributed sentence timer reset, callId={}, roundId={}, continuity={}, debounceMs={}, silenceMs={}",
                callId, roundId, continuity, debounceMs, silenceMs);
    }

    /**
     * 通话结束时调用。
     *
     * <p><b>行内规范</b>：Redis 不使用 delete 等阻塞命令，临时数据完全依赖 TTL 自动过期清理。
     * 本方法仅保留日志和方法签名，作为通话生命周期事件钩子；如未来引入其他清理动作可在此扩展。
     */
    public void cleanup(String callId) {
        if (!StringUtils.hasText(callId)) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }
        log.debug("[M02] Cleanup invoked, relying on TTL expiration, callId={}", callId);
    }

    /**
     * 多 Pod 共享轮询 Redis 延迟队列，只有成功 claim 当前 round 的实例会触发意图识别。
     */
    @Scheduled(fixedDelayString = "${copilot.debounce.polling-interval-ms:200}")
    public void pollDueTasks() {
        Set<String> dueMembers;
        try {
            dueMembers = redisTemplate.opsForZSet().rangeByScore(
                    DUE_QUEUE_KEY,
                    0,
                    System.currentTimeMillis(),
                    0,
                    Math.max(1, pollingBatchSize));
        } catch (DataAccessException e) {
            log.warn("[M02] Redis sentence timer poll failed", e);
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
            log.debug("[M02] Intent recognition triggered, callId={}, roundId={}, source={}",
                    event.getCallId(), event.getRoundId(), event.getSource());
            trigger.fire(event.getCallId());
        } catch (Exception e) {
            log.error("[M02] Intent recognition trigger failed, callId={}, roundId={}, source={}",
                    event.getCallId(), event.getRoundId(), event.getSource(), e);
        }
    }

    private boolean claimTask(String member, TimerEvent event) {
        try {
            Long claimed = redisTemplate.execute(
                    CLAIM_TASK_SCRIPT,
                    Arrays.asList(DUE_QUEUE_KEY, stateKey(event.getCallId())),
                    member,
                    event.getRoundId());
            return claimed != null && claimed == 1L;
        } catch (DataAccessException e) {
            log.warn("[M02] Redis sentence timer claim failed, member={}", member, e);
            return false;
        }
    }

    private void removeInvalidTask(String member) {
        try {
            redisTemplate.opsForZSet().remove(DUE_QUEUE_KEY, member);
        } catch (DataAccessException e) {
            log.warn("[M02] Redis invalid sentence timer cleanup failed, member={}", member, e);
        }
    }

    private long debounceMs(SentenceContinuity continuity) {
        if (SentenceContinuity.COMPLETE == continuity) {
            return Math.max(0L, props.getCompleteMs());
        }
        if (SentenceContinuity.INCOMPLETE == continuity) {
            return Math.max(0L, props.getIncompleteMs());
        }
        return Math.max(0L, props.getNeutralMs());
    }

    private static String nextRoundId() {
        return System.currentTimeMillis() + "-" + UUID.randomUUID().toString().replace("-", "");
    }

    private static String stateKey(String callId) {
        return STATE_KEY_PREFIX + callId;
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

        private String getSource() {
            return source;
        }
    }
}
