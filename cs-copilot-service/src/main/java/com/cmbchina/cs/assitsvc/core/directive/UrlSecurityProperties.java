package com.cmbchina.cs.assitsvc.core.directive;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * URL 安全校验配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "copilot")
public class UrlSecurityProperties {

    /** 允许跳转的域名白名单。 */
    private List<String> urlWhitelist = new ArrayList<>();

    /** URL Builder 配置。 */
    private BuilderOptions urlBuilder = new BuilderOptions();

    /**
     * URL Builder 选项。
     */
    @Data
    public static class BuilderOptions {

        /** 生产环境禁止访问的 UAT 域名。 */
        private List<String> uatDomains = new ArrayList<>();

        /** 同名参数冲突策略：OVERRIDE / PRESERVE / ERROR。 */
        private String sameKeyPolicy = "OVERRIDE";
    }
}
