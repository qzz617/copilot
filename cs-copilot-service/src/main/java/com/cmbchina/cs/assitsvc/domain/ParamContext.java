package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 参数上下文，供 ParamResolver 按 StandardParamType 取值。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParamContext {

    /** 客户 session 数据，key 为标准字段路径（如 customer.customerId） */
    private Map<String, Object> sessionData;

    /** 通话元数据，key 为标准字段路径（如 callId、calledNumber） */
    private Map<String, Object> callMetaData;
}
