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
            redisTemplate.opsForList().rightPush(key, json);
            // 保留尾部最新的 maxSize 条，超出时裁剪头部旧数据
            redisTemplate.opsForList().trim(key, -props.getMaxSize(), -1);
            redisTemplate.expire(key, props.getTtlHours() * 3600L, java.util.concurrent.TimeUnit.SECONDS);
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

    @Override
    public void cleanup(String callId) {
        if (callId == null || callId.isEmpty()) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }

        try {
            redisTemplate.delete(key(callId));
        } catch (DataAccessException e) {
            log.warn("[M03] Redis cleanup failed, callId={}", callId, e);
        }
    }

    /**
     * 将 ASR 事件转换为对话消息。
     * speakerRole 映射：CUSTOMER → user，其余（AGENT）→ assistant。
     * speakerRole 字段原样保留，供 M06 按需过滤。
     */
    private static DialogMessage toDialogMessage(AsrSentenceEvent event) {
        String role = "CUSTOMER".equalsIgnoreCase(event.getSpeakerRole()) ? "user" : "assistant";
        return DialogMessage.builder()
                .id(event.getSentenceId())
                .role(role)
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
