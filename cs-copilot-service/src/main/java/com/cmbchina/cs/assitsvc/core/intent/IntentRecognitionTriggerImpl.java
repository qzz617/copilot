package com.cmbchina.cs.assitsvc.core.intent;

import com.cmbchina.cs.assitsvc.config.CopilotConfigCache;
import com.cmbchina.cs.assitsvc.core.directive.DirectiveBuildException;
import com.cmbchina.cs.assitsvc.core.directive.DirectiveBuilderService;
import com.cmbchina.cs.assitsvc.core.directive.UrlValidationException;
import com.cmbchina.cs.assitsvc.core.match.IntentFunctionMatcherService;
import com.cmbchina.cs.assitsvc.domain.BuildContext;
import com.cmbchina.cs.assitsvc.domain.CallSession;
import com.cmbchina.cs.assitsvc.domain.DirectiveDTO;
import com.cmbchina.cs.assitsvc.domain.IntentResult;
import com.cmbchina.cs.assitsvc.domain.ItemCandidate;
import com.cmbchina.cs.assitsvc.domain.ParamContext;
import com.cmbchina.cs.assitsvc.infra.metrics.FilterStageConstants;
import com.cmbchina.cs.assitsvc.infra.metrics.MetricsService;
import com.cmbchina.cs.assitsvc.infra.metrics.ReasonCodeConstants;
import com.cmbchina.cs.assitsvc.push.CopilotPushService;
import com.cmbchina.cs.assitsvc.session.CallSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 意图识别触发编排实现。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentRecognitionTriggerImpl implements IntentRecognitionTrigger {

    private final CallSessionManager callSessionManager;
    private final IntentRecognitionService intentRecognitionService;
    private final IntentFunctionMatcherService matcherService;
    private final DirectiveBuilderService directiveBuilderService;
    private final CopilotPushService pushService;
    private final CopilotConfigCache configCache;
    private final MetricsService metricsService;

    @Override
    public void fire(String callId) {
        if (!StringUtils.hasText(callId)) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }

        CallSession session = callSessionManager.get(callId);
        if (session == null || !StringUtils.hasText(session.getOperatorId())) {
            log.warn("[M06] Call session missing, recommendation skipped, callId={}", callId);
            metricsService.recordTriggerFailure(callId, null, null, null,
                    ReasonCodeConstants.SESSION_BIND_MISSING,
                    FilterStageConstants.SESSION_BIND,
                    configCache.getCurrentVersion());
            return;
        }

        IntentResult intentResult = intentRecognitionService.recognize(callId);
        if (intentResult == null || !StringUtils.hasText(intentResult.getIntentCode())) {
            metricsService.recordTriggerFailure(callId, session, null, null,
                    ReasonCodeConstants.INTENT_EMPTY,
                    FilterStageConstants.INTENT_RECOGNITION,
                    configCache.getCurrentVersion());
            return;
        }

        List<ItemCandidate> candidates = matcherService.match(intentResult, session);
        if (candidates.isEmpty()) {
            log.debug("[M07] No candidate after matching, callId={}, intentCode={}",
                    callId, intentResult.getIntentCode());
            metricsService.recordTriggerFailure(callId, session, intentResult.getIntentCode(),
                    intentResult.getIntentName(), ReasonCodeConstants.INTENT_NOT_MAPPED,
                    FilterStageConstants.INTENT_MAPPING, configCache.getCurrentVersion());
            return;
        }

        for (ItemCandidate candidate : candidates) {
            if (tryBuildAndPush(callId, session, intentResult, candidate, candidates.size())) {
                return;
            }
        }
        log.warn("[M09] No directive pushed after trying all candidates, callId={}, intentCode={}",
                callId, intentResult.getIntentCode());
    }

    private boolean tryBuildAndPush(String callId, CallSession session,
                                    IntentResult intentResult, ItemCandidate candidate, int candidateCount) {
        try {
            BuildContext context = BuildContext.builder()
                    .callId(callId)
                    .operatorId(session.getOperatorId())
                    .configVersion(configCache.getCurrentVersion())
                    .action(candidate.getConfig())
                    .paramContext(buildParamContext(session))
                    .build();
            DirectiveDTO directive = directiveBuilderService.build(context, intentResult);
            boolean pushed = pushService.pushDirective(directive);
            if (pushed) {
                metricsService.recordTriggerSuccess(directive, session, candidate, candidateCount);
            } else {
                metricsService.recordTriggerFailure(callId, session, intentResult.getIntentCode(),
                        intentResult.getIntentName(), ReasonCodeConstants.PUSH_FAILED,
                        FilterStageConstants.PUSH, configCache.getCurrentVersion());
            }
            return pushed;
        } catch (RuntimeException e) {
            log.warn("[M09] Build or push directive failed, callId={}, actionId={}",
                    callId, candidate == null ? null : candidate.getActionId(), e);
            recordBuildFailure(callId, session, intentResult, e);
            return false;
        }
    }

    private void recordBuildFailure(String callId, CallSession session, IntentResult intentResult, RuntimeException e) {
        if (e instanceof UrlValidationException) {
            metricsService.recordTriggerFailure(callId, session, intentResult.getIntentCode(),
                    intentResult.getIntentName(), ReasonCodeConstants.URL_NOT_ALLOWED,
                    FilterStageConstants.URL_VALIDATION, configCache.getCurrentVersion());
        } else if (e instanceof DirectiveBuildException) {
            metricsService.recordTriggerFailure(callId, session, intentResult.getIntentCode(),
                    intentResult.getIntentName(), ReasonCodeConstants.PARAM_MISSING,
                    FilterStageConstants.PARAM_RESOLVE, configCache.getCurrentVersion());
        } else {
            metricsService.recordTriggerFailure(callId, session, intentResult.getIntentCode(),
                    intentResult.getIntentName(), ReasonCodeConstants.PUSH_FAILED,
                    FilterStageConstants.PUSH, configCache.getCurrentVersion());
        }
    }

    private static ParamContext buildParamContext(CallSession session) {
        Map<String, Object> sessionData = new HashMap<>();
        putIfHasText(sessionData, "customer.customerId", session.getCustomerId());
        putIfHasText(sessionData, "customer.customerType", session.getCustomerType());
        putIfHasText(sessionData, "customer.idNo", session.getIdNo());
        putIfHasText(sessionData, "customer.noIdType", session.getNoIdType());
        putIfHasText(sessionData, "customer.palmLifeUserId", session.getPalmLifeUserId());
        putIfHasText(sessionData, "customer.phoneNo", session.getPhoneNo());
        putIfHasText(sessionData, "customer.phoneNoNoZero", session.getPhoneNoNoZero());
        putIfHasText(sessionData, "accounts[0].accountNo", session.getAccountNo());
        putIfHasText(sessionData, "customer.address", session.getAddress());
        putIfHasText(sessionData, "customer.addressEncode", session.getAddressEncode());
        putIfHasText(sessionData, "customerId", session.getCustomerId());
        putIfHasText(sessionData, "customerType", session.getCustomerType());

        Map<String, Object> callMetaData = new HashMap<>();
        putIfHasText(callMetaData, "callId", session.getCallId());
        putIfHasText(callMetaData, "operatorId", session.getOperatorId());
        putIfHasText(callMetaData, "calledNumber", session.getCalledNumber());

        return ParamContext.builder()
                .sessionData(sessionData)
                .callMetaData(callMetaData)
                .build();
    }

    private static void putIfHasText(Map<String, Object> target, String key, String value) {
        if (StringUtils.hasText(value)) {
            target.put(key, value);
        }
    }
}
