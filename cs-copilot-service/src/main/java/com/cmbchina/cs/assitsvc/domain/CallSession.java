package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 通话会话，存储 callId-operatorId 绑定关系（Redis Hash）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallSession {

    /** 通话 ID */
    @NotBlank
    private String callId;

    /** 坐席工号 */
    @NotBlank
    private String operatorId;

    /** 客户号（来电弹屏获取） */
    private String customerId;

    /** 客户类型，如 VIP3 / Normal */
    private String customerType;

    /** 证件号 */
    private String idNo;

    /** 无证件类型标识 */
    private String noIdType;

    /** 掌上生活用户 ID */
    private String palmLifeUserId;

    /** 预留手机号一 */
    private String phoneNo;

    /** 预留手机号一，去 0 */
    private String phoneNoNoZero;

    /** 账户号，本期只取主账户 */
    private String accountNo;

    /** 地址 */
    private String address;

    /** 编码后的地址 */
    private String addressEncode;

    /** 进线号码 */
    private String calledNumber;

    /** 通话开始时间，epoch ms 字符串 */
    private String sessionStartTime;
}
