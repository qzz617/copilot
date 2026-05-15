package com.cmbchina.cs.assitsvc.core.intent;

import com.cmbchina.cs.assitsvc.asr.DialogHistoryManager;
import com.cmbchina.cs.assitsvc.domain.DialogMessage;
import com.cmbchina.cs.assitsvc.domain.IntentResult;
import com.cmbchina.cs.assitsvc.infra.feign.AiIntentFeignClient;
import com.cmbchina.cs.assitsvc.infra.feign.dto.AiDialogMessage;
import com.cmbchina.cs.assitsvc.infra.feign.dto.IntentRecognitionRequest;
import com.cmbchina.cs.assitsvc.infra.feign.dto.IntentRecognitionResponse;
import com.cmbchina.cs.assitsvc.infra.metrics.ReasonCodeConstants;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    private static final String AI_COUNT_KEY_PREFIX = "copilot:ai_count:{";
    private static final String AI_COUNT_KEY_SUFFIX = "}";
    private static final int AI_COUNT_TTL_SECONDS = 2 * 3600;
    private static final DefaultRedisScript<Long> AI_COUNT_SCRIPT = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); "
                    + "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; "
                    + "return count;",
            Long.class);

    private final AiIntentFeignClient feignClient;
    private final DialogHistoryManager historyManager;
    private final IntentTreeLoader treeLoader;
    private final ExecutedStepsManager stepsManager;
    private final StringRedisTemplate redisTemplate;

    @Value("${copilot.call-limits.max-ai-calls:50}")
    private int maxAiCalls;

    @Override
    @CircuitBreaker(name = "aiIntentClient", fallbackMethod = "fallback")
    public IntentRecognitionOutcome recognize(String callId) {
        if (!StringUtils.hasText(callId)) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }
        if (!precheckAiCall(callId)) {
            log.warn("[M06] AI call limit exceeded, callId={}, maxAiCalls={}", callId, maxAiCalls);
            return IntentRecognitionOutcome.failure(ReasonCodeConstants.AI_CALL_LIMIT_EXCEEDED);
        }

        List<AiDialogMessage> customerOnly = customerOnlyHistory(callId);
        if (customerOnly.isEmpty()) {
            log.debug("[M06] No customer history for AI recognition, callId={}", callId);
            return IntentRecognitionOutcome.failure(ReasonCodeConstants.NO_CUSTOMER_HISTORY);
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
            IntentRecognitionOutcome outcome = parseResponse(callId, request.getRequestId(), response);
            if (outcome.isSuccess()) {
                incrementAiCallCount(callId);
            }
            return outcome;
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
     * @return 失败结果包装
     */
    public IntentRecognitionOutcome fallback(String callId, Throwable t) {
        if (t instanceof CallNotPermittedException) {
            log.warn("[M06] AI circuit breaker open, callId={}", callId);
            return IntentRecognitionOutcome.failure(ReasonCodeConstants.AI_CIRCUIT_BREAKER_OPEN);
        }
        log.warn("[M06] AI call fallback by network/timeout failure, callId={}", callId, t);
        return IntentRecognitionOutcome.failure(ReasonCodeConstants.AI_NETWORK_FAIL);
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

    private IntentRecognitionOutcome parseResponse(String callId, String requestId, IntentRecognitionResponse response) {
        if (response == null) {
            log.warn("[M06] AI response is null, callId={}, requestId={}", callId, requestId);
            return IntentRecognitionOutcome.failure(ReasonCodeConstants.AI_BUSINESS_FAIL);
        }
        if (!"1000".equals(response.getRespCode())) {
            log.warn("[M06] AI response code not success, callId={}, requestId={}, respCode={}, respMsg={}",
                    callId, requestId, response.getRespCode(), response.getRespMsg());
            return IntentRecognitionOutcome.failure(ReasonCodeConstants.AI_BUSINESS_FAIL);
        }
        IntentRecognitionResponse.DataNode data = response.getData();
        if (data == null || !StringUtils.hasText(data.getIntentCode())) {
            log.debug("[M06] AI returned empty intent, callId={}, requestId={}", callId, requestId);
            return IntentRecognitionOutcome.failure(ReasonCodeConstants.INTENT_EMPTY);
        }

        return IntentRecognitionOutcome.success(IntentResult.builder()
                .intentCode(data.getIntentCode())
                .intentName(data.getIntentName())
                .build());
    }

    private boolean precheckAiCall(String callId) {
        String key = AI_COUNT_KEY_PREFIX + callId + AI_COUNT_KEY_SUFFIX;
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return true;
            }
            long count = Long.parseLong(value);
            return count < maxAiCalls;
        } catch (NumberFormatException | DataAccessException e) {
            log.warn("[M06] Redis AI call precheck failed, callId={}", callId, e);
            return true;
        }
    }

    private void incrementAiCallCount(String callId) {
        String key = AI_COUNT_KEY_PREFIX + callId + AI_COUNT_KEY_SUFFIX;
        try {
            Long count = redisTemplate.execute(AI_COUNT_SCRIPT,
                    Collections.singletonList(key),
                    String.valueOf(AI_COUNT_TTL_SECONDS));
            if (count != null && count > maxAiCalls) {
                log.warn("[M06] AI call count exceeded after success, callId={}, count={}", callId, count);
            }
        } catch (DataAccessException e) {
            log.warn("[M06] Redis AI call count increment failed, callId={}", callId, e);
        }
    }

    private static String generateRequestId() {
        return "req_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
