package com.cmbchina.cs.assitsvc.core.intent;

import com.cmbchina.cs.assitsvc.domain.IntentResult;

/**
 * AI 意图识别服务。
 */
public interface IntentRecognitionService {

    /**
     * 基于当前通话历史识别意图。
     *
     * @param callId 通话 ID
     * @return 意图识别结果；无法识别时返回 null
     */
    IntentResult recognize(String callId);
}
