package com.cmbchina.cs.assitsvc.core.feedback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 本通话静默列表管理器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MuteListManager {

    private static final String INTENT_KEY_PREFIX = "copilot:mute:{";
    private static final String INTENT_KEY_SUFFIX = "}:intent";
    private static final String ACTION_KEY_PREFIX = "copilot:mute:{";
    private static final String ACTION_KEY_SUFFIX = "}:action";
    private static final int CALL_MUTE_TTL_SECONDS = 2 * 3600;

    private final StringRedisTemplate redisTemplate;

    public void muteIntent(String callId, String intentCode, int ttlSeconds) {
        addToSet(intentKey(callId), intentCode, ttlSeconds);
    }

    public void muteIntentForCall(String callId, String intentCode) {
        muteIntent(callId, intentCode, CALL_MUTE_TTL_SECONDS);
    }

    public void muteActionForCall(String callId, String actionId) {
        if (StringUtils.hasText(actionId)) {
            addToSet(actionKey(callId), actionId, CALL_MUTE_TTL_SECONDS);
        }
    }

    public boolean isIntentMuted(String callId, String intentCode) {
        return isMember(intentKey(callId), intentCode);
    }

    public boolean isActionMuted(String callId, String actionId) {
        return StringUtils.hasText(actionId) && isMember(actionKey(callId), actionId);
    }

    public void cleanup(String callId) {
        if (!StringUtils.hasText(callId)) {
            return;
        }
        try {
            redisTemplate.delete(Arrays.asList(intentKey(callId), actionKey(callId)));
        } catch (DataAccessException e) {
            log.warn("[M11] Redis cleanup mute list failed, callId={}", callId, e);
        }
    }

    private void addToSet(String key, String value, int ttlSeconds) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        try {
            redisTemplate.opsForSet().add(key, value);
            redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        } catch (DataAccessException e) {
            log.warn("[M11] Redis mute failed, key={}, value={}", key, value, e);
        }
    }

    private boolean isMember(String key, String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
        } catch (DataAccessException e) {
            log.warn("[M11] Redis mute check failed, key={}, value={}", key, value, e);
            return false;
        }
    }

    private static String intentKey(String callId) {
        return INTENT_KEY_PREFIX + callId + INTENT_KEY_SUFFIX;
    }

    private static String actionKey(String callId) {
        return ACTION_KEY_PREFIX + callId + ACTION_KEY_SUFFIX;
    }
}
