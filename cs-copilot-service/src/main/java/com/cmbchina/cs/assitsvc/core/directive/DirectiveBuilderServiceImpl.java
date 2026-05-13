package com.cmbchina.cs.assitsvc.core.directive;

import com.cmbchina.cs.assitsvc.core.param.ParamResolveResult;
import com.cmbchina.cs.assitsvc.core.param.ParamResolverService;
import com.cmbchina.cs.assitsvc.domain.ActionInfo;
import com.cmbchina.cs.assitsvc.domain.BuildContext;
import com.cmbchina.cs.assitsvc.domain.CopilotActionConfig;
import com.cmbchina.cs.assitsvc.domain.DirectiveDTO;
import com.cmbchina.cs.assitsvc.domain.DisplayInfo;
import com.cmbchina.cs.assitsvc.domain.FunctionInfo;
import com.cmbchina.cs.assitsvc.domain.IntentInfo;
import com.cmbchina.cs.assitsvc.domain.IntentResult;
import com.cmbchina.cs.assitsvc.domain.RiskInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 跳转指令构建服务实现。
 */
@Service
@RequiredArgsConstructor
public class DirectiveBuilderServiceImpl implements DirectiveBuilderService {

    private final ParamResolverService paramResolverService;
    private final UrlBuilder urlBuilder;

    @Value("${copilot.directive.expire-seconds:30}")
    private int directiveExpireSeconds;

    @Override
    public DirectiveDTO build(BuildContext context, IntentResult intentResult) {
        validate(context, intentResult);

        CopilotActionConfig action = context.getAction();
        String targetKind = action.getTargetKind();
        String openMode = action.getOpenMode();
        String targetUrl = resolveTargetUrl(action);

        ParamResolveResult paramResult = paramResolverService.resolveParams(
                action.getParams(), context.getParamContext(), targetUrl);
        if (!paramResult.isSuccess()) {
            throw new DirectiveBuildException("Required params missing: " + paramResult.getMissingParams());
        }

        String actionUrl = buildActionUrl(targetKind, targetUrl, paramResult.getParams());
        String actionType = deriveActionType(targetKind, openMode);

        return DirectiveDTO.builder()
                .directiveId(generateDirectiveId())
                .directiveType("RECOMMENDATION")
                .callId(context.getCallId())
                .operatorId(context.getOperatorId())
                .configVersion(context.getConfigVersion())
                .expireAt(Instant.now().plusSeconds(Math.max(1, directiveExpireSeconds)).toString())
                .intent(buildIntent(intentResult))
                .function(buildFunction(action))
                .display(buildDisplay(intentResult, action))
                .action(buildAction(resolveTargetSource(action), targetKind, openMode, actionType,
                        actionUrl, paramResult.getParams()))
                .risk(buildRisk(action.getRiskLevel()))
                .build();
    }

    private static void validate(BuildContext context, IntentResult intentResult) {
        if (context == null || context.getAction() == null) {
            throw new DirectiveBuildException("BuildContext.action must not be null");
        }
        if (!StringUtils.hasText(context.getCallId()) || !StringUtils.hasText(context.getOperatorId())) {
            throw new DirectiveBuildException("callId and operatorId must not be empty");
        }
        if (intentResult == null || !StringUtils.hasText(intentResult.getIntentCode())) {
            throw new DirectiveBuildException("intentResult.intentCode must not be empty");
        }
    }

    private String buildActionUrl(String targetKind, String targetUrl, Map<String, String> params) {
        if ("URL".equalsIgnoreCase(targetKind)
                || "IFRAME".equalsIgnoreCase(targetKind)
                || "NEW_WINDOW".equalsIgnoreCase(targetKind)) {
            return urlBuilder.buildUrl(targetUrl, params);
        }
        return targetUrl;
    }

    private static IntentInfo buildIntent(IntentResult intentResult) {
        return IntentInfo.builder()
                .intentCode(intentResult.getIntentCode())
                .intentName(intentResult.getIntentName())
                .build();
    }

    private static FunctionInfo buildFunction(CopilotActionConfig action) {
        return FunctionInfo.builder()
                .actionId(action.getActionId())
                .actionName(action.getActionName())
                .menuItemId(action.getMenuItemId())
                .functionPath(action.getFunctionPath())
                .build();
    }

    private static DisplayInfo buildDisplay(IntentResult intentResult, CopilotActionConfig action) {
        String displayText = action.getAiDisplayText();
        String titleText = StringUtils.hasText(displayText) ? displayText : intentResult.getIntentName();
        return DisplayInfo.builder()
                .title("识别到：" + (StringUtils.hasText(titleText) ? titleText : intentResult.getIntentCode()))
                .tip(action.getFloatingTipText())
                .iconUrl(action.getIconUrl())
                .build();
    }

    private static ActionInfo buildAction(String targetSource, String targetKind, String openMode, String actionType,
                                          String actionUrl, Map<String, String> params) {
        return ActionInfo.builder()
                .targetSource(targetSource)
                .targetKind(targetKind)
                .openMode(openMode)
                .actionType(actionType)
                .url(actionUrl)
                .params(params)
                .build();
    }

    private static String resolveTargetSource(CopilotActionConfig action) {
        return action.getMenuItemId() == null ? "ACTION" : "MENU_ITEM";
    }

    private static RiskInfo buildRisk(String riskLevel) {
        String normalized = StringUtils.hasText(riskLevel) ? riskLevel : "LOW";
        return RiskInfo.builder()
                .riskLevel(normalized)
                .needConfirm("MEDIUM".equalsIgnoreCase(normalized) || "HIGH".equalsIgnoreCase(normalized))
                .build();
    }

    private static String resolveTargetUrl(CopilotActionConfig action) {
        String targetKind = action.getTargetKind();
        if ("ROUTE".equalsIgnoreCase(targetKind)) {
            if (!StringUtils.hasText(action.getRoutePath())) {
                throw new DirectiveBuildException("routePath must not be empty for targetKind=ROUTE");
            }
            return action.getRoutePath();
        }
        String url = action.getTargetUrl();
        if (("URL".equalsIgnoreCase(targetKind)
                || "IFRAME".equalsIgnoreCase(targetKind)
                || "NEW_WINDOW".equalsIgnoreCase(targetKind)) && !StringUtils.hasText(url)) {
            throw new DirectiveBuildException("target URL must not be empty for targetKind=" + targetKind);
        }
        return url;
    }

    private static String deriveActionType(String targetKind, String openMode) {
        String target = targetKind == null ? "" : targetKind.toUpperCase();
        String mode = openMode == null ? "" : openMode.toUpperCase();
        if ("URL".equals(target) && "NEW_TAB".equals(mode)) {
            return "OPEN_URL_NEW_TAB";
        }
        if ("URL".equals(target)) {
            return "OPEN_URL";
        }
        if ("ROUTE".equals(target) && "NEW_TAB".equals(mode)) {
            return "OPEN_ROUTE_NEW_TAB";
        }
        if ("ROUTE".equals(target)) {
            return "OPEN_ROUTE";
        }
        if ("IFRAME".equals(target)) {
            return "OPEN_IFRAME";
        }
        if ("NEW_WINDOW".equals(target)) {
            return "OPEN_NEW_WINDOW";
        }
        throw new DirectiveBuildException("Unsupported targetKind/openMode: " + targetKind + "/" + openMode);
    }

    private static String generateDirectiveId() {
        return "D_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
