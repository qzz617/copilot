package com.cmbchina.cs.assitsvc.core.intent;

import com.cmbchina.cs.assitsvc.config.CopilotConfigCache;
import com.cmbchina.cs.assitsvc.core.directive.DirectiveBuildException;
import com.cmbchina.cs.assitsvc.core.directive.DirectiveBuilderService;
import com.cmbchina.cs.assitsvc.core.directive.UrlValidationException;
import com.cmbchina.cs.assitsvc.core.event.DirectiveFailedEvent;
import com.cmbchina.cs.assitsvc.core.event.DirectivePreparedEvent;
import com.cmbchina.cs.assitsvc.core.match.IntentFunctionMatcherService;
import com.cmbchina.cs.assitsvc.domain.BuildContext;
import com.cmbchina.cs.assitsvc.domain.CallSession;
import com.cmbchina.cs.assitsvc.domain.DirectiveDTO;
import com.cmbchina.cs.assitsvc.domain.IntentResult;
import com.cmbchina.cs.assitsvc.domain.ItemCandidate;
import com.cmbchina.cs.assitsvc.infra.metrics.FilterStageConstants;
import com.cmbchina.cs.assitsvc.infra.metrics.ReasonCodeConstants;
import com.cmbchina.cs.assitsvc.session.CallSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 意图识别触发编排实现。
 *
 * <p>本类仅负责"分内事"：意图识别 → 匹配 → 指令构建。
 * 完成（或失败）后通过 {@link ApplicationEventPublisher} 发布事件，
 * 推送、日志、埋点等"事后处理"由独立监听器承担（见 {@code core/event} 包）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentRecognitionTriggerImpl implements IntentRecognitionTrigger {

    private final CallSessionManager callSessionManager;
    private final IntentRecognitionService intentRecognitionService;
    private final IntentFunctionMatcherService matcherService;
    private final DirectiveBuilderService directiveBuilderService;
    private final CopilotConfigCache configCache;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void fire(String callId) {
        if (!StringUtils.hasText(callId)) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }

        CallSession session = callSessionManager.get(callId);
        if (session == null || !StringUtils.hasText(session.getOperatorId())) {
            log.warn("[M06] Call session missing, recommendation skipped, callId={}", callId);
            publishFailed(callId, null, null, null,
                    ReasonCodeConstants.SESSION_BIND_MISSING,
                    FilterStageConstants.SESSION_BIND);
            return;
        }

        IntentRecognitionOutcome outcome = intentRecognitionService.recognize(callId);
        if (outcome == null || !outcome.isSuccess()) {
            String failReason = outcome == null ? ReasonCodeConstants.INTENT_EMPTY : outcome.getFailReason();
            publishFailed(callId, session, null, null,
                    failReason, FilterStageConstants.INTENT_RECOGNITION);
            return;
        }
        IntentResult intentResult = outcome.getIntent();

        List<ItemCandidate> candidates = matcherService.match(intentResult, session);
        if (candidates.isEmpty()) {
            log.debug("[M07] No candidate after matching, callId={}, intentCode={}",
                    callId, intentResult.getIntentCode());
            publishFailed(callId, session, intentResult.getIntentCode(), intentResult.getIntentName(),
                    ReasonCodeConstants.INTENT_NOT_MAPPED, FilterStageConstants.INTENT_MAPPING);
            return;
        }

        for (ItemCandidate candidate : candidates) {
            if (tryBuildAndPublish(callId, session, intentResult, candidate, candidates.size())) {
                return;
            }
        }
        log.warn("[M09] No directive prepared after trying all candidates, callId={}, intentCode={}",
                callId, intentResult.getIntentCode());
    }

    private boolean tryBuildAndPublish(String callId, CallSession session,
                                       IntentResult intentResult, ItemCandidate candidate, int candidateCount) {
        try {
            BuildContext context = BuildContext.builder()
                    .callId(callId)
                    .operatorId(session.getOperatorId())
                    .configVersion(configCache.getCurrentVersion())
                    .action(candidate.getConfig())
                    .build();
            DirectiveDTO directive = directiveBuilderService.build(context, intentResult);
            eventPublisher.publishEvent(new DirectivePreparedEvent(directive, session, candidate, candidateCount));
            return true;
        } catch (RuntimeException e) {
            log.warn("[M09] Build directive failed, callId={}, actionId={}",
                    callId, candidate == null ? null : candidate.getActionId(), e);
            publishBuildFailure(callId, session, intentResult, e);
            return false;
        }
    }

    private void publishBuildFailure(String callId, CallSession session, IntentResult intentResult, RuntimeException e) {
        if (e instanceof UrlValidationException) {
            publishFailed(callId, session, intentResult.getIntentCode(), intentResult.getIntentName(),
                    ReasonCodeConstants.URL_NOT_ALLOWED, FilterStageConstants.URL_VALIDATION);
        } else if (e instanceof DirectiveBuildException) {
            publishFailed(callId, session, intentResult.getIntentCode(), intentResult.getIntentName(),
                    ReasonCodeConstants.PARAM_MISSING, FilterStageConstants.PARAM_RESOLVE);
        } else {
            // 未明确分类的构建期异常，归入 DIRECTIVE_BUILD_ERROR；
            // 注意：此处尚未触发推送，因此不应误报为 PUSH_FAILED
            publishFailed(callId, session, intentResult.getIntentCode(), intentResult.getIntentName(),
                    ReasonCodeConstants.DIRECTIVE_BUILD_ERROR, FilterStageConstants.DIRECTIVE_BUILD);
        }
    }

    private void publishFailed(String callId, CallSession session, String intentCode, String intentName,
                               String reasonCode, String filterStage) {
        DirectiveFailedEvent event = DirectiveFailedEvent.builder()
                .callId(callId)
                .session(session)
                .intentCode(intentCode)
                .intentName(intentName)
                .reasonCode(reasonCode)
                .filterStage(filterStage)
                .configVersion(configCache.getCurrentVersion())
                .build();
        eventPublisher.publishEvent(event);
    }
}
