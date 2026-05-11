package com.cmbchina.cs.assitsvc.config;

import com.cmbchina.cs.assitsvc.core.directive.UrlSecurityProperties;
import com.cmbchina.cs.assitsvc.core.param.CookiePlaceholderValidator;
import com.cmbchina.cs.assitsvc.domain.CopilotExt;
import com.cmbchina.cs.assitsvc.domain.CopilotIndex;
import com.cmbchina.cs.assitsvc.domain.ItemFullConfig;
import com.cmbchina.cs.assitsvc.domain.ItemParam;
import com.cmbchina.cs.assitsvc.domain.ItemReference;
import com.cmbchina.cs.assitsvc.domain.MenuVersionData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Copilot CLOB 基础校验实现。
 */
@Service
@RequiredArgsConstructor
public class CopilotConfigValidationServiceImpl implements CopilotConfigValidationService {

    private final UrlSecurityProperties urlSecurityProperties;
    private final CookiePlaceholderValidator cookiePlaceholderValidator;

    @Override
    public ConfigValidationResult validate(MenuVersionData data) {
        List<String> errors = new ArrayList<>();
        validateRoot(data, errors);
        if (data != null && data.getCopilotIndex() != null) {
            validateIndex(data.getCopilotIndex(), errors);
        }
        return errors.isEmpty() ? ConfigValidationResult.ok() : ConfigValidationResult.fail(errors);
    }

    private void validateRoot(MenuVersionData data, List<String> errors) {
        if (data == null) {
            errors.add("MenuVersionData must not be null");
            return;
        }
        if (!StringUtils.hasText(data.getVersion())) {
            errors.add("version must not be empty");
        }
        if (data.getCopilotIndex() == null) {
            errors.add("copilotIndex must not be null");
        }
    }

    private void validateIndex(CopilotIndex index, List<String> errors) {
        if (index.getIntentToItems() == null) {
            errors.add("copilotIndex.intentToItems must not be null");
        }
        if (index.getItemById() == null) {
            errors.add("copilotIndex.itemById must not be null");
        }
        if (index.getIntentToItems() == null || index.getItemById() == null) {
            return;
        }

        for (Map.Entry<String, List<ItemReference>> entry : index.getIntentToItems().entrySet()) {
            validateIntentMapping(entry.getKey(), entry.getValue(), index.getItemById(), errors);
        }
    }

    private void validateIntentMapping(String intentCode, List<ItemReference> refs,
                                       Map<String, ItemFullConfig> itemById, List<String> errors) {
        if (!StringUtils.hasText(intentCode)) {
            errors.add("intentCode in intentToItems must not be empty");
        }
        if (refs == null || refs.isEmpty()) {
            errors.add("intentToItems." + intentCode + " must not be empty");
            return;
        }
        for (ItemReference ref : refs) {
            if (ref == null || ref.getItemId() == null) {
                errors.add("intentToItems." + intentCode + " contains empty itemId");
                continue;
            }
            ItemFullConfig item = itemById.get(String.valueOf(ref.getItemId()));
            if (item == null) {
                errors.add("itemById missing itemId=" + ref.getItemId());
            } else {
                validateItem(item, errors);
            }
        }
    }

    private void validateItem(ItemFullConfig item, List<String> errors) {
        if (item.getItemId() == null) {
            errors.add("item.itemId must not be null");
        }
        if (!StringUtils.hasText(item.getItemName())) {
            errors.add("item.itemName must not be empty, itemId=" + item.getItemId());
        }

        CopilotExt ext = item.getCopilotExt();
        String targetKind = ext != null && StringUtils.hasText(ext.getTargetKind())
                ? ext.getTargetKind() : item.getTargetKind();
        String openMode = ext != null && StringUtils.hasText(ext.getOpenMode())
                ? ext.getOpenMode() : item.getOpenMode();
        validateActionCombination(item.getItemId(), targetKind, openMode, errors);

        String targetUrl = resolveTargetUrl(item, ext, targetKind);
        if (requiresUrl(targetKind)) {
            validateUrl(item.getItemId(), targetUrl, errors);
        }
        validateCookieParams(item, targetUrl, errors);
    }

    private void validateActionCombination(Long itemId, String targetKind, String openMode, List<String> errors) {
        if (!StringUtils.hasText(targetKind) || !StringUtils.hasText(openMode)) {
            errors.add("targetKind/openMode missing, itemId=" + itemId);
            return;
        }
        String pair = targetKind + "/" + openMode;
        if (!"URL/CURRENT_TAB".equals(pair)
                && !"URL/NEW_TAB".equals(pair)
                && !"ROUTE/CURRENT_TAB".equals(pair)
                && !"ROUTE/NEW_TAB".equals(pair)
                && !"COMPONENT/POPUP".equals(pair)
                && !"COMPONENT/DRAWER".equals(pair)
                && !"IFRAME/IFRAME".equals(pair)
                && !"NEW_WINDOW/WINDOW".equals(pair)) {
            errors.add("unsupported targetKind/openMode=" + pair + ", itemId=" + itemId);
        }
    }

    private void validateUrl(Long itemId, String targetUrl, List<String> errors) {
        if (!StringUtils.hasText(targetUrl)) {
            errors.add("target URL missing, itemId=" + itemId);
            return;
        }
        try {
            URI uri = new URI(targetUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                errors.add("target URL must use https, itemId=" + itemId);
            }
            if (!urlSecurityProperties.getUrlWhitelist().contains(uri.getHost())) {
                errors.add("target URL domain not allowed, itemId=" + itemId + ", domain=" + uri.getHost());
            }
        } catch (URISyntaxException e) {
            errors.add("target URL invalid, itemId=" + itemId);
        }
    }

    private void validateCookieParams(ItemFullConfig item, String targetUrl, List<String> errors) {
        if (item.getParams() == null) {
            return;
        }
        for (ItemParam param : item.getParams()) {
            if (param != null && "COOKIE_PLACEHOLDER".equals(param.getParamType())
                    && !cookiePlaceholderValidator.validate(param, targetUrl)) {
                errors.add("cookie placeholder not allowed, itemId=" + item.getItemId()
                        + ", paramKey=" + param.getParamKey());
            }
        }
    }

    private static boolean requiresUrl(String targetKind) {
        return "URL".equalsIgnoreCase(targetKind)
                || "IFRAME".equalsIgnoreCase(targetKind)
                || "NEW_WINDOW".equalsIgnoreCase(targetKind);
    }

    private static String resolveTargetUrl(ItemFullConfig item, CopilotExt ext, String targetKind) {
        if ("ROUTE".equalsIgnoreCase(targetKind) && ext != null) {
            return ext.getRoutePath();
        }
        if ("COMPONENT".equalsIgnoreCase(targetKind) && ext != null) {
            return ext.getFallbackUrl();
        }
        if (StringUtils.hasText(item.getUrl())) {
            return item.getUrl();
        }
        return ext == null ? null : ext.getFallbackUrl();
    }
}
