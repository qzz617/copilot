package com.cmbchina.cs.assitsvc.config;

import com.cmbchina.cs.assitsvc.core.directive.UrlSecurityProperties;
import com.cmbchina.cs.assitsvc.core.param.CookiePlaceholderValidator;
import com.cmbchina.cs.assitsvc.domain.ActionReference;
import com.cmbchina.cs.assitsvc.domain.CopilotActionConfig;
import com.cmbchina.cs.assitsvc.domain.CopilotConfigSnapshot;
import com.cmbchina.cs.assitsvc.domain.ItemParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Copilot 配置基础校验实现。
 */
@Service
@RequiredArgsConstructor
public class CopilotConfigValidationServiceImpl implements CopilotConfigValidationService {

    private final UrlSecurityProperties urlSecurityProperties;
    private final CookiePlaceholderValidator cookiePlaceholderValidator;

    @Override
    public ConfigValidationResult validate(CopilotConfigSnapshot snapshot) {
        List<String> errors = new ArrayList<>();
        validateRoot(snapshot, errors);
        if (snapshot != null) {
            validateIndex(snapshot, errors);
        }
        return errors.isEmpty() ? ConfigValidationResult.ok() : ConfigValidationResult.fail(errors);
    }

    private void validateRoot(CopilotConfigSnapshot snapshot, List<String> errors) {
        if (snapshot == null) {
            errors.add("CopilotConfigSnapshot must not be null");
            return;
        }
        if (!StringUtils.hasText(snapshot.getVersionId())) {
            errors.add("versionId must not be empty");
        }
        if (snapshot.getIntentToActions() == null) {
            errors.add("intentToActions must not be null");
        } else if (snapshot.getIntentToActions().isEmpty()) {
            errors.add("intentToActions must not be empty");
        }
        if (snapshot.getActionById() == null) {
            errors.add("actionById must not be null");
        } else if (snapshot.getActionById().isEmpty()) {
            errors.add("actionById must not be empty");
        }
    }

    private void validateIndex(CopilotConfigSnapshot snapshot, List<String> errors) {
        if (snapshot.getIntentToActions() == null || snapshot.getActionById() == null) {
            return;
        }

        for (Map.Entry<String, List<ActionReference>> entry : snapshot.getIntentToActions().entrySet()) {
            validateIntentMapping(entry.getKey(), entry.getValue(), snapshot.getActionById(), errors);
        }
    }

    private void validateIntentMapping(String intentCode, List<ActionReference> refs,
                                       Map<String, CopilotActionConfig> actionById, List<String> errors) {
        if (!StringUtils.hasText(intentCode)) {
            errors.add("intentCode in intentToActions must not be empty");
        }
        if (refs == null || refs.isEmpty()) {
            errors.add("intentToActions." + intentCode + " must not be empty");
            return;
        }
        for (ActionReference ref : refs) {
            if (ref == null || !StringUtils.hasText(ref.getActionId())) {
                errors.add("intentToActions." + intentCode + " contains empty actionId");
                continue;
            }
            CopilotActionConfig action = actionById.get(ref.getActionId());
            if (action == null) {
                errors.add("actionById missing actionId=" + ref.getActionId());
            } else {
                validateAction(action, errors);
            }
        }
    }

    private void validateAction(CopilotActionConfig action, List<String> errors) {
        if (!StringUtils.hasText(action.getActionId())) {
            errors.add("action.actionId must not be empty");
        }
        if (!StringUtils.hasText(action.getActionName())) {
            errors.add("action.actionName must not be empty, actionId=" + action.getActionId());
        }

        if (action.getMenuItemId() != null) {
            return;
        }

        validateActionCombination(action.getActionId(), action.getTargetKind(), action.getOpenMode(), errors);

        String targetUrl = resolveTargetUrl(action);
        if (requiresUrl(action.getTargetKind())) {
            validateUrl(action.getActionId(), targetUrl, errors);
        } else if ("ROUTE".equalsIgnoreCase(action.getTargetKind()) && !StringUtils.hasText(targetUrl)) {
            errors.add("routePath missing, actionId=" + action.getActionId());
        }
        validateCookieParams(action, targetUrl, errors);
    }

    private void validateActionCombination(String actionId, String targetKind, String openMode, List<String> errors) {
        if (!StringUtils.hasText(targetKind) || !StringUtils.hasText(openMode)) {
            errors.add("targetKind/openMode missing, actionId=" + actionId);
            return;
        }
        String pair = targetKind.toUpperCase() + "/" + openMode.toUpperCase();
        if (!"URL/CURRENT_TAB".equals(pair)
                && !"URL/NEW_TAB".equals(pair)
                && !"ROUTE/CURRENT_TAB".equals(pair)
                && !"ROUTE/NEW_TAB".equals(pair)
                && !"IFRAME/IFRAME".equals(pair)
                && !"NEW_WINDOW/WINDOW".equals(pair)) {
            errors.add("unsupported targetKind/openMode=" + pair + ", actionId=" + actionId);
        }
    }

    private void validateUrl(String actionId, String targetUrl, List<String> errors) {
        if (!StringUtils.hasText(targetUrl)) {
            errors.add("target URL missing, actionId=" + actionId);
            return;
        }
        try {
            URI uri = new URI(targetUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                errors.add("target URL must use https, actionId=" + actionId);
            }
            if (!urlSecurityProperties.getUrlWhitelist().contains(uri.getHost())) {
                errors.add("target URL domain not allowed, actionId=" + actionId + ", domain=" + uri.getHost());
            }
        } catch (URISyntaxException e) {
            errors.add("target URL invalid, actionId=" + actionId);
        }
    }

    private void validateCookieParams(CopilotActionConfig action, String targetUrl, List<String> errors) {
        if (action.getParams() == null) {
            return;
        }
        for (ItemParam param : action.getParams()) {
            if (param != null && "COOKIE_PLACEHOLDER".equals(param.getParamType())
                    && !cookiePlaceholderValidator.validate(param, targetUrl)) {
                errors.add("cookie placeholder not allowed, actionId=" + action.getActionId()
                        + ", paramKey=" + param.getParamKey());
            }
        }
    }

    private static boolean requiresUrl(String targetKind) {
        return "URL".equalsIgnoreCase(targetKind)
                || "IFRAME".equalsIgnoreCase(targetKind)
                || "NEW_WINDOW".equalsIgnoreCase(targetKind);
    }

    private static String resolveTargetUrl(CopilotActionConfig action) {
        if ("ROUTE".equalsIgnoreCase(action.getTargetKind())) {
            return action.getRoutePath();
        }
        return action.getTargetUrl();
    }
}
