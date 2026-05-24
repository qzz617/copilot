package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话消息，存储于 Redis List（全量保存客户+坐席）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DialogMessage {

    /** 消息 ID，来自 sentenceId */
    private String id;

    /** 角色：由消费侧按 speakerRole 在出口阶段映射；写入阶段为 null */
    private String role;

    /** 语音识别原文 */
    private String content;

    /** 内容类型，固定为 "text" */
    private String contentType;

    /** 创建时间，格式 YYYY-MM-DD HH:mm:ss */
    private String createTime;

    /** 原始说话方：CUSTOMER / AGENT（保留便于 M06 过滤） */
    private String speakerRole;
}
