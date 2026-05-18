package com.cmbchina.cs.assitsvc.infra.metrics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 反馈埋点异步执行器配置。
 */
@Configuration
public class FeedbackMetricsAsyncConfig {

    /**
     * 构建反馈结果写 ES 的有界异步线程池。
     *
     * @return 反馈埋点异步执行器
     */
    @Bean(name = "feedbackMetricsExecutor")
    public Executor feedbackMetricsExecutor(
            @Value("${copilot.metrics.feedback-async.core-size:2}") int coreSize,
            @Value("${copilot.metrics.feedback-async.max-size:4}") int maxSize,
            @Value("${copilot.metrics.feedback-async.queue-capacity:1000}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("copilot-feedback-metrics-");
        executor.setCorePoolSize(Math.max(1, coreSize));
        executor.setMaxPoolSize(Math.max(Math.max(1, coreSize), maxSize));
        executor.setQueueCapacity(Math.max(1, queueCapacity));
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(2);
        return executor;
    }
}
