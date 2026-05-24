package com.cmbchina.cs.assitsvc.infra.feign;

import feign.Logger;
import feign.Request;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * AI 意图识别 Feign 配置。
 */
public class AiFeignConfig {

    /**
     * AI Feign 超时配置。
     *
     * @param connectTimeout 连接超时，毫秒
     * @param readTimeout    读取超时，毫秒
     * @return Feign 请求配置
     */
    @Bean
    public Request.Options aiRequestOptions(
            @Value("${copilot.ai.connect-timeout-ms:1000}") int connectTimeout,
            @Value("${copilot.ai.read-timeout-ms:3000}") int readTimeout) {
        return new Request.Options(
                connectTimeout,
                TimeUnit.MILLISECONDS,
                readTimeout,
                TimeUnit.MILLISECONDS,
                true);
    }

    /**
     * Feign 日志级别。
     *
     * @return BASIC 日志级别
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * AI 错误解码器。
     *
     * @return 错误解码器
     */
    @Bean
    public ErrorDecoder aiErrorDecoder() {
        return new AiErrorDecoder();
    }
}
