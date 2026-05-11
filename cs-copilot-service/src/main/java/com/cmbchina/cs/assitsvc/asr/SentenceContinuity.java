package com.cmbchina.cs.assitsvc.asr;

/**
 * ASR 客户句子的连续性类型。
 */
public enum SentenceContinuity {

    /** 包含明确业务触发信息，可以较快触发。 */
    COMPLETE,

    /** 信息不明确，按默认防抖时间等待。 */
    NEUTRAL,

    /** 句子明显未结束，需要等待更久。 */
    INCOMPLETE
}
