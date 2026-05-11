package com.cmbchina.cs.assitsvc.infra.metrics;

/**
 * 触发日志 DAO。
 */
public interface TriggerLogDao {

    void insert(TriggerLogRecord record);

    TriggerLogRecord findByDirectiveId(String directiveId);

    void updateDirectiveStatus(String directiveId, String status);

    boolean markDirectiveConsumedIfOpen(String directiveId);
}
