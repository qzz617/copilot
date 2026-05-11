package com.cmbchina.cs.assitsvc.core.match;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 最简坐席灰度白名单过滤器。
 */
@Component
@RequiredArgsConstructor
public class GrayWhitelistFilter {

    private final GrayProperties props;

    /**
     * 判断坐席是否启用 Copilot。
     *
     * @param operatorId 坐席工号
     * @return true 表示允许推荐
     */
    public boolean isOperatorEnabled(String operatorId) {
        if (!props.isEnabled()) {
            return true;
        }

        List<String> whitelist = props.getOperatorWhitelist();
        if (whitelist == null || whitelist.isEmpty()) {
            return true;
        }
        return StringUtils.hasText(operatorId) && whitelist.contains(operatorId);
    }
}
