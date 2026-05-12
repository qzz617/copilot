package com.cmbchina.cs.assitsvc.session;

import com.cmbchina.cs.assitsvc.domain.CallSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * {@link CallSessionManager} 的 Redis Hash 实现。
 *
 * <p>Redis key 格式：{@code copilot:call_session:{callId}}，TTL 固定 30 分钟。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CallSessionManagerImpl implements CallSessionManager {

    private static final String KEY_PREFIX = "copilot:call_session:{";
    private static final String KEY_SUFFIX = "}";
    private static final int SESSION_TTL_SECONDS = 30 * 60;

    private static final String FIELD_OPERATOR_ID = "operatorId";
    private static final String FIELD_CUSTOMER_ID = "customerId";
    private static final String FIELD_CUSTOMER_TYPE = "customerType";
    private static final String FIELD_SESSION_START_TIME = "sessionStartTime";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void bind(CallSession session) {
        if (session == null) {
            throw new IllegalArgumentException("session must not be null");
        }
        if (!StringUtils.hasText(session.getCallId())) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }
        if (!StringUtils.hasText(session.getOperatorId())) {
            throw new IllegalArgumentException("operatorId must not be null or empty");
        }

        Map<String, String> fields = new HashMap<>();
        fields.put(FIELD_OPERATOR_ID, session.getOperatorId());
        putIfHasText(fields, FIELD_CUSTOMER_ID, session.getCustomerId());
        putIfHasText(fields, FIELD_CUSTOMER_TYPE, session.getCustomerType());
        fields.put(FIELD_SESSION_START_TIME, resolveSessionStartTime(session));

        String key = key(session.getCallId());
        try {
            redisTemplate.opsForHash().putAll(key, fields);
            redisTemplate.expire(key, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (DataAccessException e) {
            log.warn("[M04] Redis bind call session failed, callId={}, operatorId={}",
                    session.getCallId(), session.getOperatorId(), e);
        }
    }

    @Override
    public CallSession get(String callId) {
        if (!StringUtils.hasText(callId)) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }

        Map<String, String> fields;
        try {
            fields = readHash(key(callId));
        } catch (DataAccessException e) {
            log.warn("[M04] Redis get call session failed, callId={}", callId, e);
            return null;
        }

        if (fields == null || fields.isEmpty()) {
            return null;
        }

        return CallSession.builder()
                .callId(callId)
                .operatorId(fields.get(FIELD_OPERATOR_ID))
                .customerId(fields.get(FIELD_CUSTOMER_ID))
                .customerType(fields.get(FIELD_CUSTOMER_TYPE))
                .sessionStartTime(fields.get(FIELD_SESSION_START_TIME))
                .build();
    }

    @Override
    public void cleanup(String callId) {
        if (!StringUtils.hasText(callId)) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }

        try {
            redisTemplate.delete(key(callId));
        } catch (DataAccessException e) {
            log.warn("[M04] Redis cleanup call session failed, callId={}", callId, e);
        }
    }

    private static void putIfHasText(Map<String, String> fields, String fieldName, String value) {
        if (StringUtils.hasText(value)) {
            fields.put(fieldName, value);
        }
    }

    private static String resolveSessionStartTime(CallSession session) {
        if (StringUtils.hasText(session.getSessionStartTime())) {
            return session.getSessionStartTime();
        }
        return String.valueOf(System.currentTimeMillis());
    }

    private static String key(String callId) {
        return KEY_PREFIX + callId + KEY_SUFFIX;
    }

    private Map<String, String> readHash(String key) {
        Map<Object, Object> rawFields = redisTemplate.opsForHash().entries(key);
        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<Object, Object> entry : rawFields.entrySet()) {
            fields.put(String.valueOf(entry.getKey()), entry.getValue() == null ? null : String.valueOf(entry.getValue()));
        }
        return fields;
    }
}
