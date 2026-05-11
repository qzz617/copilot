package com.cmbchina.cs.assitsvc.asr;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 句间合并防抖配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "copilot.debounce")
public class DebounceProperties {

    /** 完整句防抖时间。 */
    private long completeMs = 500L;

    /** 中性句防抖时间。 */
    private long neutralMs = 1500L;

    /** 待续句防抖时间。 */
    private long incompleteMs = 3000L;

    /** 沉默兜底触发时间。 */
    private long silenceMs = 2000L;
}
