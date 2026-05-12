package com.cmbchina.cs.assitsvc.asr;

import com.cmbchina.cs.assitsvc.domain.AsrSentenceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * ASR 句子 Kafka 消费器。
 *
 * <p>处理顺序遵循 DD-V1.2 P0-7：先全量保存历史，再做触发过滤。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsrSentenceConsumer {

    private final SentenceDedupService dedupService;
    private final DialogHistoryManager historyManager;
    private final SentenceMerger sentenceMerger;
    private final AsrSentenceEventParser parser;
    private final AsrProperties props;

    /**
     * 消费 ASR Kafka 消息。
     *
     * @param record Kafka 消息
     * @param ack    手动提交句柄
     */
    @KafkaListener(topics = "${copilot.asr.topic}", concurrency = "${copilot.asr.concurrency:4}")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String payload = record.value();
        try {
            process(payload, record.key(), record.partition(), record.offset());
        } catch (Exception e) {
            log.error("[M01] Process ASR event failed, payload={}", payload, e);
        } finally {
            acknowledge(ack);
        }
    }

    void process(String payload, String recordKey, int partition, long offset) {
        AsrSentenceEvent event = parser.parse(payload);
        if (!basicValid(event)) {
            log.warn("[M01] Invalid ASR event ignored, event={}", event);
            return;
        }

        if (dedupService.isDuplicate(event.getSentenceId())) {
            log.debug("[M01] Duplicate ASR sentence ignored, callId={}, sentenceId={}",
                    event.getCallId(), event.getSentenceId());
            return;
        }

        historyManager.append(event.getCallId(), event);

        if (!"CUSTOMER".equalsIgnoreCase(event.getSpeakerRole())) {
            return;
        }
        if (!routeValid(event, recordKey, partition, offset)) {
            return;
        }
        if (!triggerValid(event)) {
            return;
        }

        sentenceMerger.handleSentence(event);
    }

    private boolean basicValid(AsrSentenceEvent event) {
        return event != null
                && StringUtils.hasText(event.getCallId())
                && StringUtils.hasText(event.getSentenceId())
                && StringUtils.hasText(event.getContent());
    }

    private boolean triggerValid(AsrSentenceEvent event) {
        if (event.getContent().trim().length() < props.getMinTextLength()) {
            return false;
        }

        Float confidence = event.getConfidence();
        if (confidence == null) {
            return props.isAsrConfidenceDefaultPass();
        }

        return confidence >= props.getAsrConfidenceThreshold();
    }

    private boolean routeValid(AsrSentenceEvent event, String recordKey, int partition, long offset) {
        if (!props.isRequireCallIdKey()) {
            return true;
        }
        if (event.getCallId().equals(recordKey)) {
            return true;
        }
        log.error("[M01] ASR event Kafka key mismatch, trigger skipped, callId={}, recordKey={}, partition={}, offset={}",
                event.getCallId(), recordKey, partition, offset);
        return false;
    }

    private static void acknowledge(Acknowledgment ack) {
        if (ack != null) {
            ack.acknowledge();
        }
    }
}
