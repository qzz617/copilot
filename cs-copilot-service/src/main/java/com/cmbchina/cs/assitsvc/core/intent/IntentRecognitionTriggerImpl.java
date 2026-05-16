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
import com.cmbchina.cs.assitsvc.infra.metrics.FilterStageConstants;
import com.cmbchina.cs.assitsvc.infra.metrics.MetricsService;
import com.cmbchina.cs.assitsvc.infra.metrics.ReasonCodeConstants;
import com.cmbchina.cs.assitsvc.push.CopilotPushService;
import com.cmbchina.cs.assitsvc.session.CallSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

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

        IntentRecognitionOutcome outcome = intentRecognitionService.recognize(callId);
        if (outcome == null || !outcome.isSuccess()) {
            String failReason = outcome == null ? ReasonCodeConstants.INTENT_EMPTY : outcome.getFailReason();
            metricsService.recordTriggerFailure(callId, session, null, null,
                    failReason,
                    FilterStageConstants.INTENT_RECOGNITION,
                    configCache.getCurrentVersion());
            return;
        }
        IntentResult intentResult = outcome.getIntent();

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
                    .build();
            DirectiveDTO directive = directiveBuilderService.build(context, intentResult);
            boolean published = pushService.publishDirectiveAsync(directive);
            if (published) {
                metricsService.recordTriggerSuccess(directive, session, candidate, candidateCount);
            } else {
                metricsService.recordTriggerFailure(callId, session, intentResult.getIntentCode(),
                        intentResult.getIntentName(), ReasonCodeConstants.PUSH_FAILED,
                        FilterStageConstants.PUSH, configCache.getCurrentVersion());
            }
            return published;
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
}
