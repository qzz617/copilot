package com.cmbchina.cs.assitsvc.core.intent;

import lombok.extern.slf4j.Slf4j;

/**
 * 意图识别触发入口的默认空实现。
 *
 * <p>M10 主链路已提供真实 {@link IntentRecognitionTrigger} Bean，本类仅保留为手工替换用空实现。
 */
@Slf4j
public class NoOpIntentRecognitionTrigger implements IntentRecognitionTrigger {

    @Override
    public void fire(String callId) {
        log.debug("[M02] Intent recognition trigger is not implemented yet, callId={}", callId);
    }
}
