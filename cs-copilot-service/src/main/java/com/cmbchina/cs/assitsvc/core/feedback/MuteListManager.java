package com.cmbchina.cs.assitsvc.core.feedback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

/**
 * 本通话静默列表管理器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MuteListManager {

    private static final String INTENT_KEY_PREFIX = "copilot:mute:intent:";
    private static final String ITEM_KEY_PREFIX = "copilot:mute:item:";
    private static final int CALL_MUTE_TTL_SECONDS = 2 * 3600;

    private final JedisPool jedisPool;

    public void muteIntent(String callId, String intentCode, int ttlSeconds) {
        addToSet(INTENT_KEY_PREFIX + callId, intentCode, ttlSeconds);
    }

    public void muteIntentForCall(String callId, String intentCode) {
        muteIntent(callId, intentCode, CALL_MUTE_TTL_SECONDS);
    }

    public void muteItemForCall(String callId, Long itemId) {
        if (itemId != null) {
            addToSet(ITEM_KEY_PREFIX + callId, String.valueOf(itemId), CALL_MUTE_TTL_SECONDS);
        }
    }

    public boolean isIntentMuted(String callId, String intentCode) {
        return isMember(INTENT_KEY_PREFIX + callId, intentCode);
    }

    public boolean isItemMuted(String callId, Long itemId) {
        return itemId != null && isMember(ITEM_KEY_PREFIX + callId, String.valueOf(itemId));
    }

    public void cleanup(String callId) {
        if (!StringUtils.hasText(callId)) {
            return;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(INTENT_KEY_PREFIX + callId);
            jedis.del(ITEM_KEY_PREFIX + callId);
        } catch (JedisException e) {
            log.warn("[M11] Redis cleanup mute list failed, callId={}", callId, e);
        }
    }

    private void addToSet(String key, String value, int ttlSeconds) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.sadd(key, value);
            jedis.expire(key, ttlSeconds);
        } catch (JedisException e) {
            log.warn("[M11] Redis mute failed, key={}, value={}", key, value, e);
        }
    }

    private boolean isMember(String key, String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.sismember(key, value);
        } catch (JedisException e) {
            log.warn("[M11] Redis mute check failed, key={}, value={}", key, value, e);
            return false;
        }
    }
}
