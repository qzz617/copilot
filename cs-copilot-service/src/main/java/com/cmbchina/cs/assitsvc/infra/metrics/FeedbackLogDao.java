package com.cmbchina.cs.assitsvc.infra.metrics;

/**
 * 反馈日志 DAO。
 */
public interface FeedbackLogDao {

    void insert(FeedbackLogRecord record);

    boolean existsEffective(String directiveId);
}
