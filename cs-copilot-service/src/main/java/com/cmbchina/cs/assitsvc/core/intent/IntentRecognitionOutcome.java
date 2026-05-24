package com.cmbchina.cs.assitsvc.core.intent;

import com.cmbchina.cs.assitsvc.domain.IntentResult;
import lombok.Builder;
import lombok.Getter;

/**
 * AI 意图识别结果包装，区分成功与多种失败分类。
 */
@Getter
@Builder
public class IntentRecognitionOutcome {

    /** 识别结果；仅 success=true 时有值。 */
    private final IntentResult intent;

    /** 是否成功识别到非空 intentCode。 */
    private final boolean success;

    /** 失败原因码；与 ReasonCodeConstants 对齐。 */
    private final String failReason;

    public static IntentRecognitionOutcome success(IntentResult intent) {
        return IntentRecognitionOutcome.builder()
                .intent(intent)
                .success(true)
                .build();
    }

    public static IntentRecognitionOutcome failure(String failReason) {
        return IntentRecognitionOutcome.builder()
                .success(false)
                .failReason(failReason)
                .build();
    }
}
