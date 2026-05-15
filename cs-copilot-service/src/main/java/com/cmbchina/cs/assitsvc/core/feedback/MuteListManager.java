package com.cmbchina.cs.assitsvc.core.feedback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
        log.debug("[M11] Cleanup invoked, relying on TTL expiration, callId={}", callId);
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
