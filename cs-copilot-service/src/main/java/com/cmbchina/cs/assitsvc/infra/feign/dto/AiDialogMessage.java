package com.cmbchina.cs.assitsvc.infra.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送给 AI 的对话消息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiDialogMessage {

    /** 消息 ID。 */
    private String id;

    /** 角色：user / assistant。 */
    private String role;

    /** 文本内容。 */
    private String content;

    /** 内容类型。 */
    private String contentType;

    /** 创建时间。 */
    private String createTime;
}
