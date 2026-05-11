package com.cmbchina.cs.assitsvc.core.intent;

import com.alibaba.fastjson2.JSON;
import com.cmbchina.cs.assitsvc.domain.ExecutedStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 已执行步骤管理器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutedStepsManager {

    private static final String KEY_PREFIX = "copilot:steps:";
    private static final int STEPS_TTL_SECONDS = 3600;

    private final JedisPool jedisPool;

    /**
     * 追加已执行步骤。
     *
     * @param callId     通话 ID
     * @param intentCode 意图代码
     * @param intentName 意图名称
     */
    public void appendStep(String callId, String intentCode, String intentName) {
        if (!StringUtils.hasText(callId) || !StringUtils.hasText(intentCode)) {
            throw new IllegalArgumentException("callId and intentCode must not be null or empty");
        }

        ExecutedStep step = ExecutedStep.builder()
                .intentCode(intentCode)
                .intentName(intentName)
                .timestamp(System.currentTimeMillis())
                .build();

        String key = key(callId);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.rpush(key, JSON.toJSONString(step));
            jedis.expire(key, STEPS_TTL_SECONDS);
        } catch (JedisException e) {
            log.warn("[M06] Redis append executed step failed, callId={}, intentCode={}", callId, intentCode, e);
        }
    }

    /**
     * 查询已执行步骤。
     *
     * @param callId 通话 ID
     * @return 已执行步骤列表
     */
    public List<ExecutedStep> getSteps(String callId) {
        if (!StringUtils.hasText(callId)) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }

        List<String> rawList;
        try (Jedis jedis = jedisPool.getResource()) {
            rawList = jedis.lrange(key(callId), 0, -1);
        } catch (JedisException e) {
            log.warn("[M06] Redis get executed steps failed, callId={}", callId, e);
            return Collections.emptyList();
        }

        List<ExecutedStep> result = new ArrayList<>();
        for (String raw : rawList) {
            try {
                result.add(JSON.parseObject(raw, ExecutedStep.class));
            } catch (Exception e) {
                log.warn("[M06] Parse executed step failed, callId={}, raw={}", callId, raw, e);
            }
        }
        return result;
    }

    /**
     * 清理已执行步骤。
     *
     * @param callId 通话 ID
     */
    public void cleanup(String callId) {
        if (!StringUtils.hasText(callId)) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key(callId));
        } catch (JedisException e) {
            log.warn("[M06] Redis cleanup executed steps failed, callId={}", callId, e);
        }
    }

    private static String key(String callId) {
        return KEY_PREFIX + callId;
    }
}
