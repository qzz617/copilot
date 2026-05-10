package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通话会话，存储 callId-operatorId 绑定关系（Redis Hash）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallSession {

    /** 通话 ID */
    private String callId;

    /** 坐席工号 */
    private String operatorId;

    /** 客户号（来电弹屏获取） */
    private String customerId;

    /** 客户类型，如 VIP3 / Normal */
    private String customerType;

    /** 通话开始时间，epoch ms 字符串 */
    private String sessionStartTime;
}
