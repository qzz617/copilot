package com.cmbchina.cs.assitsvc.core.intent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * 意图树加载配置，绑定 copilot.intent-tree.*。
 */
@Data
@Component
@ConfigurationProperties(prefix = "copilot.intent-tree")
public class IntentTreeProperties {

    /** 意图树 JSON 文件位置，Spring Resource 表达式（如 classpath:intent-tree.json）。 */
    private Resource file;

    /** 意图树版本号，传给 AI 接口用。 */
    private String version;
}
