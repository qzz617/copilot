package com.cmbchina.cs.assitsvc.asr;

import com.alibaba.fastjson2.JSON;
import com.cmbchina.cs.assitsvc.domain.AsrSentenceEvent;
import com.cmbchina.cs.assitsvc.domain.DialogMessage;
import com.cmbchina.cs.assitsvc.infra.redis.HistoryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link DialogHistoryManager} 的 Redis 实现。
 *
 * <p>Redis key 格式：{@code copilot:history:{callId}}，类型 List。
 * 每次 append 用 Pipeline 批发 RPUSH + LTRIM + EXPIRE，减少网络往返。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DialogHistoryManagerImpl implements DialogHistoryManager {

    private static final String KEY_PREFIX = "copilot:history:";

    private final JedisPool jedisPool;
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

        String key = KEY_PREFIX + callId;
        try (Jedis jedis = jedisPool.getResource()) {
            Pipeline pipeline = jedis.pipelined();
            pipeline.rpush(key, json);
            // 保留尾部最新的 maxSize 条，超出时裁剪头部旧数据
            pipeline.ltrim(key, -props.getMaxSize(), -1);
            pipeline.expire(key, (long) props.getTtlHours() * 3600);
            pipeline.sync();
        } catch (JedisException e) {
            log.warn("[M03] Redis append failed, callId={}", callId, e);
        }
    }

    @Override
    public List<DialogMessage> getHistory(String callId) {
        if (callId == null || callId.isEmpty()) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }

        String key = KEY_PREFIX + callId;
        List<String> jsonList;
        try (Jedis jedis = jedisPool.getResource()) {
            jsonList = jedis.lrange(key, 0, -1);
        } catch (JedisException e) {
            log.warn("[M03] Redis getHistory failed, callId={}", callId, e);
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

        String key = KEY_PREFIX + callId;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        } catch (JedisException e) {
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
                .createTime(event.getBeginTime())
                .speakerRole(event.getSpeakerRole())
                .build();
    }
}
