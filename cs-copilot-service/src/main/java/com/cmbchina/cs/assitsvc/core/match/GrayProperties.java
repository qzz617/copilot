package com.cmbchina.cs.assitsvc.core.match;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 最简灰度白名单配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "copilot.gray")
public class GrayProperties {

    /** 灰度开关。 */
    private boolean enabled = true;

    /** 坐席白名单。 */
    private List<String> operatorWhitelist = new ArrayList<>();
}
