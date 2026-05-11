package com.cmbchina.cs.assitsvc.core.intent;

import com.cmbchina.cs.assitsvc.asr.DialogHistoryManager;
import com.cmbchina.cs.assitsvc.domain.DialogMessage;
import com.cmbchina.cs.assitsvc.domain.IntentResult;
import com.cmbchina.cs.assitsvc.infra.feign.AiIntentFeignClient;
import com.cmbchina.cs.assitsvc.infra.feign.dto.AiDialogMessage;
import com.cmbchina.cs.assitsvc.infra.feign.dto.IntentRecognitionRequest;
import com.cmbchina.cs.assitsvc.infra.feign.dto.IntentRecognitionResponse;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * AI 意图识别服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentRecognitionServiceImpl implements IntentRecognitionService {

    private static final String AI_COUNT_KEY_PREFIX = "copilot:ai_count:";
    private static final int AI_COUNT_TTL_SECONDS = 2 * 3600;
    private static final String AI_COUNT_SCRIPT =
            "local count = redis.call('INCR', KEYS[1]); "
                    + "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; "
                    + "return count;";

    private final AiIntentFeignClient feignClient;
    private final DialogHistoryManager historyManager;
    private final IntentTreeLoader treeLoader;
    private final ExecutedStepsManager stepsManager;
    private final JedisPool jedisPool;

    @Value("${copilot.call-limits.max-ai-calls:50}")
    private int maxAiCalls;

    @Override
    @CircuitBreaker(name = "aiIntentClient", fallbackMethod = "fallback")
    public IntentResult recognize(String callId) {
        if (!StringUtils.hasText(callId)) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }
        if (!allowAiCall(callId)) {
            log.warn("[M06] AI call limit exceeded, callId={}, maxAiCalls={}", callId, maxAiCalls);
            return null;
        }

        List<AiDialogMessage> customerOnly = customerOnlyHistory(callId);
        if (customerOnly.isEmpty()) {
            log.debug("[M06] No customer history for AI recognition, callId={}", callId);
            return null;
        }

        IntentRecognitionRequest request = IntentRecognitionRequest.builder()
                .sessionId(callId)
                .requestId(generateRequestId())
                .history(customerOnly)
                .executedSteps(stepsManager.getSteps(callId))
                .intentTree(treeLoader.getTree())
                .treeVersion(treeLoader.getVersion())
                .build();

        try {
            IntentRecognitionResponse response = feignClient.recognize(request);
            return parseResponse(callId, request.getRequestId(), response);
        } catch (FeignException e) {
            log.warn("[M06] AI intent recognition failed, callId={}, status={}", callId, e.status(), e);
            throw e;
        }
    }

    /**
     * Resilience4j 熔断 fallback。
     *
     * @param callId 通话 ID
     * @param t      异常
     * @return null
     */
    public IntentResult fallback(String callId, Throwable t) {
        log.warn("[M06] AI circuit breaker fallback, callId={}", callId, t);
        return null;
    }

    private List<AiDialogMessage> customerOnlyHistory(String callId) {
        List<DialogMessage> fullHistory = historyManager.getHistory(callId);
        List<AiDialogMessage> customerOnly = new ArrayList<>();
        for (DialogMessage message : fullHistory) {
            if (message != null && "CUSTOMER".equalsIgnoreCase(message.getSpeakerRole())) {
                customerOnly.add(toAiMessage(message));
            }
        }
        return customerOnly;
    }

    private static AiDialogMessage toAiMessage(DialogMessage message) {
        return AiDialogMessage.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .contentType(message.getContentType())
                .createTime(message.getCreateTime())
                .build();
    }

    private IntentResult parseResponse(String callId, String requestId, IntentRecognitionResponse response) {
        if (response == null) {
            log.warn("[M06] AI response is null, callId={}, requestId={}", callId, requestId);
            return null;
        }
        if (!"1000".equals(response.getRespCode())) {
            log.warn("[M06] AI response code not success, callId={}, requestId={}, respCode={}, respMsg={}",
                    callId, requestId, response.getRespCode(), response.getRespMsg());
            return null;
        }
        IntentRecognitionResponse.DataNode data = response.getData();
        if (data == null || !StringUtils.hasText(data.getIntentCode())) {
            log.debug("[M06] AI returned empty intent, callId={}, requestId={}", callId, requestId);
            return null;
        }

        return IntentResult.builder()
                .intentCode(data.getIntentCode())
                .intentName(data.getIntentName())
                .build();
    }

    private boolean allowAiCall(String callId) {
        String key = AI_COUNT_KEY_PREFIX + callId;
        try (Jedis jedis = jedisPool.getResource()) {
            Object result = jedis.eval(AI_COUNT_SCRIPT,
                    Collections.singletonList(key),
                    Collections.singletonList(String.valueOf(AI_COUNT_TTL_SECONDS)));
            Long count = toLong(result);
            return count == null || count <= maxAiCalls;
        } catch (JedisException e) {
            log.warn("[M06] Redis AI call count failed, callId={}", callId, e);
            return true;
        }
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private static String generateRequestId() {
        return "req_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
