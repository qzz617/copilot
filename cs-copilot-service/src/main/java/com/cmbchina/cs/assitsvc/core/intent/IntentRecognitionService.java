package com.cmbchina.cs.assitsvc.core.intent;

/**
 * AI 意图识别服务。
 */
public interface IntentRecognitionService {

    /**
     * 基于当前通话历史识别意图。
     *
     * @param callId 通话 ID
     * @return 识别结果包装；含成功/失败语义和失败分类
     */
    IntentRecognitionOutcome recognize(String callId);
}
