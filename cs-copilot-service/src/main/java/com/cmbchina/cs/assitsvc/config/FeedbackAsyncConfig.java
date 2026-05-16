package com.cmbchina.cs.assitsvc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 反馈异步生效线程池配置。
 */
@Configuration
public class FeedbackAsyncConfig {

    @Bean("feedbackEffectExecutor")
    public Executor feedbackEffectExecutor(
            @Value("${copilot.feedback.async.core-pool-size:4}") int corePoolSize,
            @Value("${copilot.feedback.async.max-pool-size:16}") int maxPoolSize,
            @Value("${copilot.feedback.async.queue-capacity:1000}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("copilot-feedback-");
        executor.initialize();
        return executor;
    }
}
