package com.cmbchina.cs.assitsvc.core.directive;

import com.cmbchina.cs.assitsvc.core.param.ParamResolveResult;
import com.cmbchina.cs.assitsvc.core.param.ParamResolverService;
import com.cmbchina.cs.assitsvc.domain.ActionInfo;
import com.cmbchina.cs.assitsvc.domain.BuildContext;
import com.cmbchina.cs.assitsvc.domain.CopilotExt;
import com.cmbchina.cs.assitsvc.domain.DirectiveDTO;
import com.cmbchina.cs.assitsvc.domain.DisplayInfo;
import com.cmbchina.cs.assitsvc.domain.FunctionInfo;
import com.cmbchina.cs.assitsvc.domain.IntentInfo;
import com.cmbchina.cs.assitsvc.domain.IntentResult;
import com.cmbchina.cs.assitsvc.domain.ItemFullConfig;
import com.cmbchina.cs.assitsvc.domain.RiskInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    @Override
    public DirectiveDTO build(BuildContext context, IntentResult intentResult) {
        validate(context, intentResult);

        ItemFullConfig item = context.getItem();
        CopilotExt ext = item.getCopilotExt();
        String targetKind = firstText(extValue(ext, "targetKind"), item.getTargetKind());
        String openMode = firstText(extValue(ext, "openMode"), item.getOpenMode());
        String targetUrl = resolveTargetUrl(item, ext, targetKind);

        ParamResolveResult paramResult = paramResolverService.resolveParams(
                item.getParams(), context.getParamContext(), targetUrl);
        if (!paramResult.isSuccess()) {
            throw new DirectiveBuildException("Required params missing: " + paramResult.getMissingParams());
        }

        String actionUrl = buildActionUrl(targetKind, targetUrl, paramResult.getParams());
        String actionType = deriveActionType(targetKind, openMode);
        String riskLevel = riskLevel(item, ext);

        return DirectiveDTO.builder()
                .directiveId(generateDirectiveId())
                .directiveType("RECOMMENDATION")
                .callId(context.getCallId())
                .operatorId(context.getOperatorId())
                .configVersion(context.getConfigVersion())
                .expireAt(Instant.now().plus(30, ChronoUnit.SECONDS).toString())
                .intent(buildIntent(intentResult))
                .function(buildFunction(item, ext))
                .display(buildDisplay(intentResult, ext))
                .action(buildAction(targetKind, openMode, actionType, actionUrl, paramResult.getParams()))
                .risk(buildRisk(riskLevel))
                .build();
    }

    private static void validate(BuildContext context, IntentResult intentResult) {
        if (context == null || context.getItem() == null) {
            throw new DirectiveBuildException("BuildContext.item must not be null");
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

    private static FunctionInfo buildFunction(ItemFullConfig item, CopilotExt ext) {
        return FunctionInfo.builder()
                .itemId(item.getItemId())
                .itemName(item.getItemName())
                .functionPath(ext == null ? null : ext.getFunctionPath())
                .build();
    }

    private static DisplayInfo buildDisplay(IntentResult intentResult, CopilotExt ext) {
        String displayText = ext == null ? null : ext.getAiDisplayText();
        String titleText = StringUtils.hasText(displayText) ? displayText : intentResult.getIntentName();
        return DisplayInfo.builder()
                .title("识别到：" + (StringUtils.hasText(titleText) ? titleText : intentResult.getIntentCode()))
                .tip(ext == null ? null : ext.getFloatingTipText())
                .iconUrl(ext == null ? null : ext.getIconUrl())
                .build();
    }

    private static ActionInfo buildAction(String targetKind, String openMode, String actionType,
                                          String actionUrl, Map<String, String> params) {
        return ActionInfo.builder()
                .targetKind(targetKind)
                .openMode(openMode)
                .actionType(actionType)
                .url(actionUrl)
                .params(params)
                .build();
    }

    private static RiskInfo buildRisk(String riskLevel) {
        String normalized = StringUtils.hasText(riskLevel) ? riskLevel : "LOW";
        return RiskInfo.builder()
                .riskLevel(normalized)
                .needConfirm("MEDIUM".equalsIgnoreCase(normalized) || "HIGH".equalsIgnoreCase(normalized))
                .build();
    }

    private static String resolveTargetUrl(ItemFullConfig item, CopilotExt ext, String targetKind) {
        if ("ROUTE".equalsIgnoreCase(targetKind) && ext != null && StringUtils.hasText(ext.getRoutePath())) {
            return ext.getRoutePath();
        }
        if ("COMPONENT".equalsIgnoreCase(targetKind)) {
            return ext == null ? null : ext.getFallbackUrl();
        }
        String url = item.getUrl();
        if (!StringUtils.hasText(url) && ext != null) {
            url = ext.getFallbackUrl();
        }
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
        if ("COMPONENT".equals(target) && "DRAWER".equals(mode)) {
            return "OPEN_COMPONENT_DRAWER";
        }
        if ("COMPONENT".equals(target)) {
            return "OPEN_COMPONENT_POPUP";
        }
        if ("IFRAME".equals(target)) {
            return "OPEN_IFRAME";
        }
        if ("NEW_WINDOW".equals(target)) {
            return "OPEN_NEW_WINDOW";
        }
        throw new DirectiveBuildException("Unsupported targetKind/openMode: " + targetKind + "/" + openMode);
    }

    private static String riskLevel(ItemFullConfig item, CopilotExt ext) {
        if (ext != null && StringUtils.hasText(ext.getRiskLevel())) {
            return ext.getRiskLevel();
        }
        return item.getRiskLevel();
    }

    private static String extValue(CopilotExt ext, String field) {
        if (ext == null) {
            return null;
        }
        if ("targetKind".equals(field)) {
            return ext.getTargetKind();
        }
        if ("openMode".equals(field)) {
            return ext.getOpenMode();
        }
        return null;
    }

    private static String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private static String generateDirectiveId() {
        return "D_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
