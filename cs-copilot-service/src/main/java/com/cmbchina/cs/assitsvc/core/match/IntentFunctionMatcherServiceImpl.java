package com.cmbchina.cs.assitsvc.core.match;

import com.cmbchina.cs.assitsvc.config.CopilotConfigCache;
import com.cmbchina.cs.assitsvc.core.feedback.MuteListManager;
import com.cmbchina.cs.assitsvc.domain.CallSession;
import com.cmbchina.cs.assitsvc.domain.CopilotExt;
import com.cmbchina.cs.assitsvc.domain.EvaluationContext;
import com.cmbchina.cs.assitsvc.domain.IntentResult;
import com.cmbchina.cs.assitsvc.domain.ItemCandidate;
import com.cmbchina.cs.assitsvc.domain.ItemFullConfig;
import com.cmbchina.cs.assitsvc.domain.ItemReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 意图-功能匹配服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentFunctionMatcherServiceImpl implements IntentFunctionMatcherService {

    private final CopilotConfigCache configCache;
    private final RuleEvaluator ruleEvaluator;
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

        List<ItemReference> refs = configCache.findCandidatesByIntent(intentResult.getIntentCode());
        if (refs.isEmpty()) {
            log.debug("[M07] No item mapping for intent, callId={}, intentCode={}",
                    session.getCallId(), intentResult.getIntentCode());
            return Collections.emptyList();
        }

        EvaluationContext ctx = buildEvaluationContext(session);
        List<ItemCandidate> candidates = new ArrayList<>();
        for (ItemReference ref : refs) {
            if (muteListManager.isItemMuted(session.getCallId(), ref == null ? null : ref.getItemId())) {
                continue;
            }
            ItemCandidate candidate = toCandidate(ref, ctx);
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

    private ItemCandidate toCandidate(ItemReference ref, EvaluationContext ctx) {
        if (ref == null || ref.getItemId() == null) {
            return null;
        }

        ItemFullConfig item = configCache.getItemConfig(ref.getItemId());
        if (item == null || !isCopilotEnabled(item) || isRiskDisabled(item)) {
            return null;
        }

        String conditionRule = conditionRule(item);
        if (!ruleEvaluator.evaluate(conditionRule, ctx)) {
            return null;
        }

        return ItemCandidate.builder()
                .itemId(ref.getItemId())
                .priority(ref.getPriority())
                .config(item)
                .build();
    }

    private static EvaluationContext buildEvaluationContext(CallSession session) {
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("customer.customerId", session.getCustomerId());
        sessionData.put("customer.customerType", session.getCustomerType());
        sessionData.put("customerId", session.getCustomerId());
        sessionData.put("customerType", session.getCustomerType());

        Map<String, Object> callMetaData = new HashMap<>();
        callMetaData.put("callId", session.getCallId());

        return EvaluationContext.builder()
                .callId(session.getCallId())
                .operatorId(session.getOperatorId())
                .customerId(session.getCustomerId())
                .customerType(session.getCustomerType())
                .sessionData(sessionData)
                .callMetaData(callMetaData)
                .build();
    }

    private static boolean isCopilotEnabled(ItemFullConfig item) {
        CopilotExt ext = item.getCopilotExt();
        if (ext != null && ext.getEnabled() != null) {
            return ext.getEnabled();
        }
        return Boolean.TRUE.equals(item.getCopilotEnabled());
    }

    private static boolean isRiskDisabled(ItemFullConfig item) {
        String riskLevel = riskLevel(item);
        return "DISABLED".equalsIgnoreCase(riskLevel);
    }

    private static String riskLevel(ItemFullConfig item) {
        CopilotExt ext = item.getCopilotExt();
        if (ext != null && StringUtils.hasText(ext.getRiskLevel())) {
            return ext.getRiskLevel();
        }
        return item.getRiskLevel();
    }

    private static String conditionRule(ItemFullConfig item) {
        CopilotExt ext = item.getCopilotExt();
        return ext == null ? null : ext.getConditionRule();
    }

    private static int priority(ItemCandidate candidate) {
        return candidate == null || candidate.getPriority() == null ? 0 : candidate.getPriority();
    }
}
