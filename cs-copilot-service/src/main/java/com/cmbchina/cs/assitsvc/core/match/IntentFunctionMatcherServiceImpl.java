package com.cmbchina.cs.assitsvc.core.match;

import com.cmbchina.cs.assitsvc.config.CopilotConfigCache;
import com.cmbchina.cs.assitsvc.core.feedback.MuteListManager;
import com.cmbchina.cs.assitsvc.domain.ActionReference;
import com.cmbchina.cs.assitsvc.domain.CallSession;
import com.cmbchina.cs.assitsvc.domain.CopilotActionConfig;
import com.cmbchina.cs.assitsvc.domain.IntentResult;
import com.cmbchina.cs.assitsvc.domain.ItemCandidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 意图-动作匹配服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentFunctionMatcherServiceImpl implements IntentFunctionMatcherService {

    private final CopilotConfigCache configCache;
    private final GrayWhitelistFilter grayWhitelistFilter;
    private final MuteListManager muteListManager;

    @Override
    public List<ItemCandidate> match(IntentResult intentResult, CallSession session) {
        if (intentResult == null || !StringUtils.hasText(intentResult.getIntentCode())) {
            return Collections.emptyList();
        }
        if (session == null || !StringUtils.hasText(session.getOperatorId())) {
            return Collections.emptyList();
        }
        if (!grayWhitelistFilter.isOperatorEnabled(session.getOperatorId())) {
            log.info("[M07] Operator filtered by gray whitelist, callId={}, operatorId={}, intentCode={}",
                    session.getCallId(), session.getOperatorId(), intentResult.getIntentCode());
            return Collections.emptyList();
        }
        if (muteListManager.isIntentMuted(session.getCallId(), intentResult.getIntentCode())) {
            log.info("[M07] Intent muted by agent, callId={}, intentCode={}",
                    session.getCallId(), intentResult.getIntentCode());
            return Collections.emptyList();
        }

        List<ActionReference> refs = configCache.findCandidatesByIntent(intentResult.getIntentCode());
        if (refs.isEmpty()) {
            log.debug("[M07] No action mapping for intent, callId={}, intentCode={}",
                    session.getCallId(), intentResult.getIntentCode());
            return Collections.emptyList();
        }

        List<ItemCandidate> candidates = new ArrayList<>();
        for (ActionReference ref : refs) {
            if (muteListManager.isActionMuted(session.getCallId(), ref == null ? null : ref.getActionId())) {
                continue;
            }
            ItemCandidate candidate = toCandidate(ref);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }

        Collections.sort(candidates, new Comparator<ItemCandidate>() {
            @Override
            public int compare(ItemCandidate left, ItemCandidate right) {
                return Integer.compare(priority(right), priority(left));
            }
        });
        return candidates;
    }

    private ItemCandidate toCandidate(ActionReference ref) {
        if (ref == null || !StringUtils.hasText(ref.getActionId())) {
            return null;
        }

        CopilotActionConfig action = configCache.getActionConfig(ref.getActionId());
        if (action == null || !isCopilotEnabled(action) || isRiskDisabled(action)) {
            return null;
        }

        return ItemCandidate.builder()
                .actionId(ref.getActionId())
                .priority(ref.getPriority())
                .config(action)
                .build();
    }

    private static boolean isCopilotEnabled(CopilotActionConfig action) {
        return Boolean.TRUE.equals(action.getEnabled());
    }

    private static boolean isRiskDisabled(CopilotActionConfig action) {
        return "DISABLED".equalsIgnoreCase(action.getRiskLevel());
    }

    private static int priority(ItemCandidate candidate) {
        return candidate == null || candidate.getPriority() == null ? 0 : candidate.getPriority();
    }
}
