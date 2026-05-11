package com.cmbchina.cs.assitsvc.infra.feign;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * AI Feign 错误解码器。
 */
@Slf4j
public class AiErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.warn("[M06] AI feign error, methodKey={}, status={}", methodKey, response.status());
        return defaultDecoder.decode(methodKey, response);
    }
}
