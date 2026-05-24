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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * AI 意图识别服务实现。
 *
 * <p>单通话调用频次的保护由上游 {@link com.cmbchina.cs.assitsvc.asr.SentenceMerger}
 * 的固定窗口（默认 5s）天然限制；服务级保护由 Resilience4j 熔断器（{@code aiIntentClient}）承担。
 * 因此本类不再做"单通话最大 AI 调用次数"的应用层计数。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentRecognitionServiceImpl implements IntentRecognitionService {

    private final AiIntentFeignClient feignClient;
    private final DialogHistoryManager historyManager;
    private final IntentTreeLoader treeLoader;
    private final ExecutedStepsManager stepsManager;

    @Override
    @CircuitBreaker(name = "aiIntentClient", fallbackMethod = "fallback")
    public IntentRecognitionOutcome recognize(String callId) {
        if (!StringUtils.hasText(callId)) {
            throw new IllegalArgumentException("callId must not be null or empty");
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
        // customerOnlyHistory 已过滤为 CUSTOMER 句子，此处 role 固定为 "user"
        return AiDialogMessage.builder()
                .id(message.getId())
                .role("user")
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
        if (!IntentRecognitionResponse.RESP_CODE_SUCCESS.equals(response.getRespCode())) {
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

    private static String generateRequestId() {
        return "req_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
