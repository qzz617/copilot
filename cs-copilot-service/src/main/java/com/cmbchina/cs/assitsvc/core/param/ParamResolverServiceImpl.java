package com.cmbchina.cs.assitsvc.core.param;

import com.cmbchina.cs.assitsvc.domain.ItemParam;
import com.cmbchina.cs.assitsvc.domain.ParamContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 参数解析服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParamResolverServiceImpl implements ParamResolverService {

    private final CookiePlaceholderValidator cookiePlaceholderValidator;

    @Override
    public ParamResolveResult resolveParams(List<ItemParam> paramList, ParamContext ctx, String targetUrl) {
        Map<String, String> params = new LinkedHashMap<>();
        List<String> missingParams = new ArrayList<>();
        if (paramList == null || paramList.isEmpty()) {
            return ParamResolveResult.builder().params(params).missingParams(missingParams).build();
        }

        for (ItemParam param : paramList) {
            resolveOne(param, ctx, targetUrl, params, missingParams);
        }
        return ParamResolveResult.builder().params(params).missingParams(missingParams).build();
    }

    private void resolveOne(ItemParam param, ParamContext ctx, String targetUrl,
                            Map<String, String> params, List<String> missingParams) {
        if (param == null || !StringUtils.hasText(param.getParamKey()) || !StringUtils.hasText(param.getParamType())) {
            return;
        }

        StandardParamType type;
        try {
            type = StandardParamType.valueOf(param.getParamType());
        } catch (IllegalArgumentException e) {
            log.warn("[M08] Unknown paramType ignored, paramType={}, paramKey={}",
                    param.getParamType(), param.getParamKey());
            return;
        }

        String value = resolveValue(type, param, ctx, targetUrl);
        if (StringUtils.hasText(value)) {
            params.put(param.getParamKey(), value);
        } else if (type.getSourceType() != ParamSourceType.LITERAL) {
            missingParams.add(param.getParamKey());
        }
    }

    private String resolveValue(StandardParamType type, ItemParam param, ParamContext ctx, String targetUrl) {
        if (ParamSourceType.LITERAL == type.getSourceType()) {
            return param.getParamValue();
        }
        if (ParamSourceType.SESSION == type.getSourceType()) {
            return toStringValue(lookup(ctx == null ? null : ctx.getSessionData(), type.getDefaultSourceKey()));
        }
        if (ParamSourceType.CALL_META == type.getSourceType()) {
            return toStringValue(lookup(ctx == null ? null : ctx.getCallMetaData(), type.getDefaultSourceKey()));
        }
        if (ParamSourceType.COOKIE == type.getSourceType()) {
            if (!cookiePlaceholderValidator.validate(param, targetUrl)) {
                return null;
            }
            return "${COOKIE." + param.getParamValue() + "}";
        }
        return null;
    }

    private static Object lookup(Map<String, Object> source, String path) {
        if (source == null || !StringUtils.hasText(path)) {
            return null;
        }
        if (source.containsKey(path)) {
            return source.get(path);
        }

        Object current = source;
        String normalizedPath = path.replaceAll("\\[(\\d+)\\]", ".$1");
        String[] parts = normalizedPath.split("\\.");
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else if (current instanceof List) {
                current = getListValue((List<?>) current, part);
            } else if (current != null && current.getClass().isArray()) {
                current = getArrayValue(current, part);
            } else {
                current = null;
            }
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static Object getListValue(List<?> list, String indexText) {
        Integer index = parseIndex(indexText);
        if (index == null || index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    private static Object getArrayValue(Object array, String indexText) {
        Integer index = parseIndex(indexText);
        if (index == null || index < 0 || index >= Array.getLength(array)) {
            return null;
        }
        return Array.get(array, index);
    }

    private static Integer parseIndex(String indexText) {
        try {
            return Integer.valueOf(indexText);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
