package com.cmbchina.cs.assitsvc.core.directive;

import com.cmbchina.cs.assitsvc.domain.BuildContext;
import com.cmbchina.cs.assitsvc.domain.DirectiveDTO;
import com.cmbchina.cs.assitsvc.domain.IntentResult;

/**
 * 跳转指令构建服务。
 */
public interface DirectiveBuilderService {

    /**
     * 构建推荐指令。
     *
     * @param context      指令构建上下文
     * @param intentResult 意图识别结果
     * @return 推送指令
     */
    DirectiveDTO build(BuildContext context, IntentResult intentResult);
}
