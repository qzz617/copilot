package com.cmbchina.cs.assitsvc.infra.feign;

import com.cmbchina.cs.assitsvc.infra.feign.dto.IntentRecognitionRequest;
import com.cmbchina.cs.assitsvc.infra.feign.dto.IntentRecognitionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * AI 意图识别 Feign Client。
 */
@FeignClient(
        name = "ai-intent-client",
        url = "${copilot.ai.url}",
        configuration = AiFeignConfig.class
)
public interface AiIntentFeignClient {

    /**
     * 调用 AI 意图识别接口。
     *
     * @param request 请求体
     * @return AI 响应
     */
    @PostMapping(value = "/AICSCopilotReplyGen/getSopResult.json", consumes = MediaType.APPLICATION_JSON_VALUE)
    IntentRecognitionResponse recognize(@RequestBody IntentRecognitionRequest request);
}
