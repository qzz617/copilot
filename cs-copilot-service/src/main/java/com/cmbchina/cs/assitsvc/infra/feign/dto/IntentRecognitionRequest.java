package com.cmbchina.cs.assitsvc.infra.feign.dto;

import com.cmbchina.cs.assitsvc.domain.ExecutedStep;
import com.cmbchina.cs.assitsvc.domain.IntentTreeNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 意图识别请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentRecognitionRequest {

    /** 通话 ID。 */
    private String sessionId;

    /** 请求 ID。 */
    private String requestId;

    /** 调用前过滤后的客户历史消息。 */
    private List<AiDialogMessage> history;

    /** 坐席已接受执行的历史步骤。 */
    private List<ExecutedStep> executedSteps;

    /** 意图树。 */
    private IntentTreeNode intentTree;

    /** 意图树版本。 */
    private String treeVersion;
}
