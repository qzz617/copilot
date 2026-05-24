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
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 通话会话接口。
 */
@Slf4j
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
     * 解绑并触发通话结束钩子。
     *
     * <p>每个 cleanup 独立容错：任一步骤异常都不影响后续清理，避免单点故障导致部分资源清理遗漏。
     *
     * @param request 解绑请求
     * @return 处理结果
     */
    @PostMapping("/unbind")
    public ApiResult unbind(@Valid @RequestBody UnbindRequest request) {
        String callId = request.getCallId();
        runCleanup("callSession", callId, () -> callSessionManager.cleanup(callId));
        runCleanup("dialogHistory", callId, () -> dialogHistoryManager.cleanup(callId));
        runCleanup("sentenceMerger", callId, () -> sentenceMerger.cleanup(callId));
        runCleanup("executedSteps", callId, () -> executedStepsManager.cleanup(callId));
        runCleanup("muteList", callId, () -> muteListManager.cleanup(callId));
        return ApiResult.ok();
    }

    /**
     * 包装单个 cleanup 调用，捕获任意异常仅记日志，保证后续 cleanup 正常进行。
     */
    private static void runCleanup(String name, String callId, Runnable cleanup) {
        try {
            cleanup.run();
        } catch (Exception e) {
            log.warn("[M04] Cleanup step failed but proceeding, step={}, callId={}", name, callId, e);
        }
    }
}
