package com.cmbchina.cs.assitsvc.api.controller;

import com.cmbchina.cs.assitsvc.api.dto.ApiResult;
import com.cmbchina.cs.assitsvc.api.dto.UnbindRequest;
import com.cmbchina.cs.assitsvc.asr.DialogHistoryManager;
import com.cmbchina.cs.assitsvc.asr.SentenceMerger;
import com.cmbchina.cs.assitsvc.core.feedback.MuteListManager;
import com.cmbchina.cs.assitsvc.core.intent.ExecutedStepsManager;
import com.cmbchina.cs.assitsvc.domain.CallSession;
import com.cmbchina.cs.assitsvc.session.CallSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 通话会话接口。
 */
@RestController
@RequestMapping("/copilot/session")
@RequiredArgsConstructor
public class SessionController {

    private final CallSessionManager callSessionManager;
    private final DialogHistoryManager dialogHistoryManager;
    private final SentenceMerger sentenceMerger;
    private final ExecutedStepsManager executedStepsManager;
    private final MuteListManager muteListManager;

    /**
     * 绑定 callId 与 operatorId。
     *
     * @param session 通话会话
     * @return 处理结果
     */
    @PostMapping("/bind")
    public ApiResult bind(@Valid @RequestBody CallSession session) {
        callSessionManager.bind(session);
        return ApiResult.ok();
    }

    /**
     * 解绑并清理通话临时状态。
     *
     * @param request 解绑请求
     * @return 处理结果
     */
    @PostMapping("/unbind")
    public ApiResult unbind(@Valid @RequestBody UnbindRequest request) {
        String callId = request.getCallId();
        callSessionManager.cleanup(callId);
        dialogHistoryManager.cleanup(callId);
        sentenceMerger.cleanup(callId);
        executedStepsManager.cleanup(callId);
        muteListManager.cleanup(callId);
        return ApiResult.ok();
    }
}
