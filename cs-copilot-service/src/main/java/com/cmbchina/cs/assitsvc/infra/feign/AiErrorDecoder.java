package com.cmbchina.cs.assitsvc.infra.feign;

import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * AI Feign 错误解码器。
 */
@Slf4j
public class AiErrorDecoder implements ErrorDecoder {

    private static final int MAX_BODY_LOG_LENGTH = 2048;

    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        Response replayableResponse = response;
        String responseBody = null;
        try {
            byte[] bodyData = response.body() == null
                    ? new byte[0]
                    : Util.toByteArray(response.body().asInputStream());
            if (bodyData.length > 0) {
                responseBody = abbreviate(new String(bodyData, StandardCharsets.UTF_8));
                replayableResponse = response.toBuilder().body(bodyData).build();
            }
        } catch (IOException e) {
            log.warn("[M06] Read AI feign error body failed, methodKey={}, status={}",
                    methodKey, response.status(), e);
        }

        log.warn("[M06] AI feign error, methodKey={}, status={}, body={}",
                methodKey, response.status(), responseBody);
        return defaultDecoder.decode(methodKey, replayableResponse);
    }

    private static String abbreviate(String body) {
        if (body == null || body.length() <= MAX_BODY_LOG_LENGTH) {
            return body;
        }
        return body.substring(0, MAX_BODY_LOG_LENGTH) + "...";
    }
}
