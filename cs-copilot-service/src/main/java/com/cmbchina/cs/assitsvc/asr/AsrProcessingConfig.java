package com.cmbchina.cs.assitsvc.asr;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ASR 处理链路运行时配置。
 */
@Configuration
public class AsrProcessingConfig {

    /**
     * M02 防抖与沉默 timer 使用的调度线程池。
     *
     * @return 调度线程池
     */
    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService sentenceMergerScheduler() {
        return Executors.newScheduledThreadPool(2, new ThreadFactory() {
            private final AtomicInteger sequence = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "copilot-sentence-merger-" + sequence.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        });
    }
}
