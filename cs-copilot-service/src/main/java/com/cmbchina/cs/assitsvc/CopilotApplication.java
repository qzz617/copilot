package com.cmbchina.cs.assitsvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 客服工作台 AI Copilot 服务入口。
 *
 * <p>启动后会：
 * <ul>
 *     <li>从 classpath:intent-tree.json 加载意图树</li>
 *     <li>从存量 cs_menu_version 加载最新 CLOB 配置到内存</li>
 *     <li>开始监听 Kafka topic cs.asr.sentences</li>
 *     <li>每 30 秒轮询配置版本号兜底（多 Pod 一致性）</li>
 * </ul>
 *
 * @author cs-copilot-team
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.cmbchina.cs.assitsvc.infra.feign")
@EnableKafka
@EnableScheduling
public class CopilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(CopilotApplication.class, args);
    }
}
