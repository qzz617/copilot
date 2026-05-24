package com.cmbchina.cs.assitsvc.core.match;

import com.cmbchina.cs.assitsvc.domain.CallSession;
import com.cmbchina.cs.assitsvc.domain.IntentResult;
import com.cmbchina.cs.assitsvc.domain.ItemCandidate;

import java.util.List;

/**
 * 意图-动作匹配服务。
 */
public interface IntentFunctionMatcherService {

    /**
     * 按意图识别结果匹配候选动作。
     *
     * @param intentResult 意图识别结果
     * @param session      通话会话
     * @return 候选动作列表
     */
    List<ItemCandidate> match(IntentResult intentResult, CallSession session);
}
