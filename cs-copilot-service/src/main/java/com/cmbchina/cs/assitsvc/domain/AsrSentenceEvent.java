package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ASR 实时识别句子事件，来自 Kafka topic cs.asr.sentences。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsrSentenceEvent {

    /** 通话 ID */
    private String callId;

    /** 句子 ID（去重用） */
    private String sentenceId;

    /** 说话方：CUSTOMER / AGENT */
    private String speakerRole;

    /** 坐席工号（仅 AGENT 有值） */
    private String speakerId;

    /** 语音识别原文 */
    private String content;

    /** 句子开始时间，ISO 8601 格式 */
    private String beginTime;

    /** 句子结束时间，ISO 8601 格式 */
    private String endTime;

    /** ASR 识别置信度，可为 null（P1-1：缺失时按配置决定） */
    private Float confidence;
}
