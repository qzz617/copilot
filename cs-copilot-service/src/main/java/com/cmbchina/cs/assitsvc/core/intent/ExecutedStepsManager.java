package com.cmbchina.cs.assitsvc.core.intent;

import com.alibaba.fastjson2.JSON;
import com.cmbchina.cs.assitsvc.domain.ExecutedStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 已执行步骤管理器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutedStepsManager {

    private static final String KEY_PREFIX = "copilot:steps:{";
    private static final String KEY_SUFFIX = "}";
    private static final int STEPS_TTL_SECONDS = 3600;

    private final StringRedisTemplate redisTemplate;

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
        try {
            redisTemplate.opsForList().rightPush(key, JSON.toJSONString(step));
            redisTemplate.expire(key, STEPS_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (DataAccessException e) {
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
        try {
            rawList = redisTemplate.opsForList().range(key(callId), 0, -1);
        } catch (DataAccessException e) {
            log.warn("[M06] Redis get executed steps failed, callId={}", callId, e);
            return Collections.emptyList();
        }
        if (rawList == null) {
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
        try {
            redisTemplate.delete(key(callId));
        } catch (DataAccessException e) {
            log.warn("[M06] Redis cleanup executed steps failed, callId={}", callId, e);
        }
    }

    private static String key(String callId) {
        return KEY_PREFIX + callId + KEY_SUFFIX;
    }
}
