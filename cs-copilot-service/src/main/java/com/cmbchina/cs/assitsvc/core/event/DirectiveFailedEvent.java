package com.cmbchina.cs.assitsvc.core.event;

import com.cmbchina.cs.assitsvc.domain.CallSession;
import lombok.Builder;
import lombok.Getter;

/**
 * 指令构建失败事件。
 *
 * <p>用于覆盖各阶段的失败场景：session 绑定缺失、意图识别失败、意图未映射、参数解析失败、
 * URL 校验失败、推送失败等。具体阶段通过 {@link #filterStage} 和 {@link #reasonCode} 区分。
 */
@Getter
@Builder
public class DirectiveFailedEvent {

    /** 通话 ID。 */
    private final String callId;

    /** 通话会话上下文，session 绑定阶段失败时可能为 null。 */
    private final CallSession session;

    /** 意图代码，未到意图识别阶段时为 null。 */
    private final String intentCode;

    /** 意图名称，未到意图识别阶段时为 null。 */
    private final String intentName;

    /** 失败原因码，见 {@link com.cmbchina.cs.assitsvc.infra.metrics.ReasonCodeConstants}。 */
    private final String reasonCode;

    /** 失败阶段，见 {@link com.cmbchina.cs.assitsvc.infra.metrics.FilterStageConstants}。 */
    private final String filterStage;

    /** 配置版本。 */
    private final String configVersion;
}
