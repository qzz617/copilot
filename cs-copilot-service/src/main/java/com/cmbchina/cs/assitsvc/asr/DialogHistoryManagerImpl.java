package com.cmbchina.cs.assitsvc.asr;

import com.alibaba.fastjson2.JSON;
import com.cmbchina.cs.assitsvc.domain.AsrSentenceEvent;
import com.cmbchina.cs.assitsvc.domain.DialogMessage;
import com.cmbchina.cs.assitsvc.infra.redis.HistoryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link DialogHistoryManager} 的 Redis 实现。
 *
 * <p>Redis key 格式：{@code copilot:history:{callId}}，类型 List。
 * 每次 append 写入尾部后裁剪到最新 maxSize 条，并刷新 TTL。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DialogHistoryManagerImpl implements DialogHistoryManager {

    private static final String KEY_PREFIX = "copilot:history:{";
    private static final String KEY_SUFFIX = "}";
    private static final DateTimeFormatter CREATE_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Lua 脚本：RPUSH + LTRIM + EXPIRE 原子化。
     * 避免三步之间的瞬态不一致（如 RPUSH 后崩溃导致 list 超长且无 TTL）。
     * <pre>
     * KEYS[1] = list key
     * ARGV[1] = JSON 消息
     * ARGV[2] = maxSize（trim 起点 = -maxSize）
     * ARGV[3] = TTL（秒）
     * </pre>
     */
    private static final org.springframework.data.redis.core.script.DefaultRedisScript<Long> APPEND_SCRIPT =
            new org.springframework.data.redis.core.script.DefaultRedisScript<>(
                    "redis.call('RPUSH', KEYS[1], ARGV[1]); "
                            + "redis.call('LTRIM', KEYS[1], -tonumber(ARGV[2]), -1); "
                            + "redis.call('EXPIRE', KEYS[1], ARGV[3]); "
                            + "return 1;",
                    Long.class);

    private final StringRedisTemplate redisTemplate;
    private final HistoryProperties props;

    @Override
    public void append(String callId, AsrSentenceEvent event) {
        if (callId == null || callId.isEmpty()) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }

        String json;
        try {
            json = JSON.toJSONString(toDialogMessage(event));
        } catch (Exception e) {
            log.error("[M03] JSON serialize failed, callId={}, sentenceId={}", callId, event.getSentenceId(), e);
            return;
        }

        String key = key(callId);
        try {
            redisTemplate.execute(APPEND_SCRIPT,
                    java.util.Collections.singletonList(key),
                    json,
                    String.valueOf(props.getMaxSize()),
                    String.valueOf(props.getTtlHours() * 3600L));
        } catch (DataAccessException e) {
            log.warn("[M03] Redis append failed, callId={}", callId, e);
        }
    }

    @Override
    public List<DialogMessage> getHistory(String callId) {
        if (callId == null || callId.isEmpty()) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }

        List<String> jsonList;
        try {
            jsonList = redisTemplate.opsForList().range(key(callId), 0, -1);
        } catch (DataAccessException e) {
            log.warn("[M03] Redis getHistory failed, callId={}", callId, e);
            return Collections.emptyList();
        }
        if (jsonList == null) {
            return Collections.emptyList();
        }

        List<DialogMessage> result = new ArrayList<>(jsonList.size());
        for (String json : jsonList) {
            try {
                result.add(JSON.parseObject(json, DialogMessage.class));
            } catch (Exception e) {
                log.error("[M03] JSON deserialize failed, callId={}, json={}", callId, json, e);
            }
        }
        return result;
    }

    /**
     * 通话结束时调用。
     *
     * <p><b>行内规范</b>：Redis 不使用 delete 等阻塞命令，临时数据完全依赖 TTL 自动过期清理。
     * 本方法仅保留日志和方法签名，作为通话生命周期事件钩子；如未来引入其他清理动作可在此扩展。
     */
    @Override
    public void cleanup(String callId) {
        if (callId == null || callId.isEmpty()) {
            log.debug("[M03] Cleanup skipped on blank callId");
            return;
        }
        log.debug("[M03] Cleanup invoked, relying on TTL expiration, callId={}", callId);
    }

    /**
     * 将 ASR 事件转换为对话消息。
     *
     * <p>不在写入阶段硬编码 role 字段：原先 {@code AGENT → "assistant"} 的映射与
     * AI 接口语义（assistant 指 AI 自己）冲突。role 改由消费侧
     * （{@link com.cmbchina.cs.assitsvc.core.intent.IntentRecognitionServiceImpl}）在
     * 出口阶段按需映射，本类只忠实保留 speakerRole 原值。
     */
    private static DialogMessage toDialogMessage(AsrSentenceEvent event) {
        return DialogMessage.builder()
                .id(event.getSentenceId())
                .role(null)
                .content(event.getContent())
                .contentType("text")
                .createTime(formatBeginTime(event.getBeginTime()))
                .speakerRole(event.getSpeakerRole())
                .build();
    }

    /** ISO 8601 → yyyy-MM-dd HH:mm:ss（UTC），beginTime 为 null 或解析失败时返回当前时间。 */
    private static String formatBeginTime(String beginTime) {
        if (beginTime == null || beginTime.isEmpty()) {
            return LocalDateTime.now().format(CREATE_TIME_FMT);
        }
        try {
            return Instant.parse(beginTime)
                    .atOffset(java.time.ZoneOffset.UTC)
                    .toLocalDateTime()
                    .format(CREATE_TIME_FMT);
        } catch (Exception e) {
            return LocalDateTime.now().format(CREATE_TIME_FMT);
        }
    }

    private static String key(String callId) {
        return KEY_PREFIX + callId + KEY_SUFFIX;
    }
}
