package com.cmbchina.cs.assitsvc.core.event;

import com.cmbchina.cs.assitsvc.domain.CallSession;
import com.cmbchina.cs.assitsvc.domain.DirectiveDTO;
import com.cmbchina.cs.assitsvc.domain.ItemCandidate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 指令构建成功事件。
 *
 * <p>主链路（{@link com.cmbchina.cs.assitsvc.core.intent.IntentRecognitionTriggerImpl}）
 * 在完成意图识别、匹配、指令构建后发布本事件，由各监听器决定后续处理（推送、日志、数据飞轮等）。
 */
@Getter
@RequiredArgsConstructor
public class DirectivePreparedEvent {

    /** 已构建好的指令。 */
    private final DirectiveDTO directive;

    /** 通话会话上下文。 */
    private final CallSession session;

    /** 命中的候选动作。 */
    private final ItemCandidate candidate;

    /** 候选总数，用于埋点。 */
    private final int candidateCount;
}
