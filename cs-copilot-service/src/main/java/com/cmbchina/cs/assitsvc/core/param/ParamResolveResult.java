package com.cmbchina.cs.assitsvc.core.param;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 参数解析结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParamResolveResult {

    /** 已解析参数。 */
    private Map<String, String> params;

    /** 缺失或不允许的参数 key。 */
    private List<String> missingParams;

    /**
     * 是否解析成功。
     *
     * @return true 表示没有缺失参数
     */
    public boolean isSuccess() {
        return missingParams == null || missingParams.isEmpty();
    }
}
