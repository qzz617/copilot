package com.cmbchina.cs.assitsvc.core.intent;

/**
 * 意图识别触发入口。
 *
 * <p>M02 只负责判断触发时机；实际 AI 调用和推荐编排由后续 M06/M07 实现。
 */
public interface IntentRecognitionTrigger {

    /**
     * 触发指定通话的意图识别。
     *
     * @param callId 通话 ID
     */
    void fire(String callId);
}
