package com.cmbchina.cs.assitsvc.session;

import com.cmbchina.cs.assitsvc.domain.CallSession;

/**
 * 通话会话管理，维护 callId 与 operatorId 的绑定关系。
 *
 * <p>M04 只负责保存、查询和清理绑定；绑定缺失时由后续推荐流程 fail closed。
 */
public interface CallSessionManager {

    /**
     * 保存通话会话绑定。
     *
     * @param session 通话会话，callId 和 operatorId 不可为空
     * @throws IllegalArgumentException session、callId 或 operatorId 为空时抛出
     */
    void bind(CallSession session);

    /**
     * 查询通话会话绑定。
     *
     * @param callId 通话 ID，不可为空
     * @return 会话绑定；不存在或 Redis 不可达时返回 null
     * @throws IllegalArgumentException callId 为空时抛出
     */
    CallSession get(String callId);

    /**
     * 清理通话会话绑定。
     *
     * @param callId 通话 ID，不可为空
     * @throws IllegalArgumentException callId 为空时抛出
     */
    void cleanup(String callId);
}
