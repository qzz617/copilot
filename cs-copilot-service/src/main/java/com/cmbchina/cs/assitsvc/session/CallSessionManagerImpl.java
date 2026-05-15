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
    private static final String FIELD_ID_NO = "idNo";
    private static final String FIELD_NO_ID_TYPE = "noIdType";
    private static final String FIELD_PALM_LIFE_USER_ID = "palmLifeUserId";
    private static final String FIELD_PHONE_NO = "phoneNo";
    private static final String FIELD_PHONE_NO_NO_ZERO = "phoneNoNoZero";
    private static final String FIELD_ACCOUNT_NO = "accountNo";
    private static final String FIELD_ADDRESS = "address";
    private static final String FIELD_ADDRESS_ENCODE = "addressEncode";
    private static final String FIELD_CALLED_NUMBER = "calledNumber";
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
        putIfHasText(fields, FIELD_ID_NO, session.getIdNo());
        putIfHasText(fields, FIELD_NO_ID_TYPE, session.getNoIdType());
        putIfHasText(fields, FIELD_PALM_LIFE_USER_ID, session.getPalmLifeUserId());
        putIfHasText(fields, FIELD_PHONE_NO, session.getPhoneNo());
        putIfHasText(fields, FIELD_PHONE_NO_NO_ZERO, session.getPhoneNoNoZero());
        putIfHasText(fields, FIELD_ACCOUNT_NO, session.getAccountNo());
        putIfHasText(fields, FIELD_ADDRESS, session.getAddress());
        putIfHasText(fields, FIELD_ADDRESS_ENCODE, session.getAddressEncode());
        putIfHasText(fields, FIELD_CALLED_NUMBER, session.getCalledNumber());
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
                .idNo(fields.get(FIELD_ID_NO))
                .noIdType(fields.get(FIELD_NO_ID_TYPE))
                .palmLifeUserId(fields.get(FIELD_PALM_LIFE_USER_ID))
                .phoneNo(fields.get(FIELD_PHONE_NO))
                .phoneNoNoZero(fields.get(FIELD_PHONE_NO_NO_ZERO))
                .accountNo(fields.get(FIELD_ACCOUNT_NO))
                .address(fields.get(FIELD_ADDRESS))
                .addressEncode(fields.get(FIELD_ADDRESS_ENCODE))
                .calledNumber(fields.get(FIELD_CALLED_NUMBER))
                .sessionStartTime(fields.get(FIELD_SESSION_START_TIME))
                .build();
    }

    /**
     * 通话结束时调用。
     *
     * <p><b>行内规范</b>：Redis 不使用 delete 等阻塞命令，临时数据完全依赖 TTL 自动过期清理。
     * 本方法仅保留日志和方法签名，作为通话生命周期事件钩子；如未来引入其他清理动作可在此扩展。
     */
    @Override
    public void cleanup(String callId) {
        if (!StringUtils.hasText(callId)) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }
        log.debug("[M04] Cleanup invoked, relying on TTL expiration, callId={}", callId);
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
