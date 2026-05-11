package com.cmbchina.cs.assitsvc.asr;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.cmbchina.cs.assitsvc.domain.AsrSentenceEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * ASR Kafka 消息反序列化器。
 */
@Slf4j
@Component
public class AsrSentenceEventParser {

    /**
     * 将 Kafka payload 解析为 ASR 事件。
     *
     * @param payload Kafka 消息体
     * @return 解析成功的事件；payload 为空或 JSON 非法时返回 null
     */
    public AsrSentenceEvent parse(String payload) {
        if (!StringUtils.hasText(payload)) {
            log.warn("[M01] Empty ASR payload ignored");
            return null;
        }

        try {
            return JSON.parseObject(payload, AsrSentenceEvent.class);
        } catch (JSONException e) {
            log.warn("[M01] Invalid ASR payload ignored, payload={}", payload, e);
            return null;
        }
    }
}
