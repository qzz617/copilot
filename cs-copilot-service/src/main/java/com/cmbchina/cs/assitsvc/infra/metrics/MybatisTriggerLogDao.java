package com.cmbchina.cs.assitsvc.infra.metrics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cmbchina.cs.assitsvc.infra.metrics.mapper.TriggerLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * MyBatis-Plus 触发日志 DAO。
 */
@Repository
@RequiredArgsConstructor
public class MybatisTriggerLogDao implements TriggerLogDao {

    private final TriggerLogMapper triggerLogMapper;

    @Override
    public void insert(TriggerLogRecord record) {
        triggerLogMapper.insert(record);
    }

    @Override
    public TriggerLogRecord findByDirectiveId(String directiveId) {
        if (!StringUtils.hasText(directiveId)) {
            return null;
        }
        LambdaQueryWrapper<TriggerLogRecord> wrapper = Wrappers.<TriggerLogRecord>lambdaQuery()
                .eq(TriggerLogRecord::getDirectiveId, directiveId)
                .last("LIMIT 1");
        return triggerLogMapper.selectOne(wrapper);
    }

    @Override
    public void updateDirectiveStatus(String directiveId, String status) {
        if (!StringUtils.hasText(directiveId) || !StringUtils.hasText(status)) {
            return;
        }
        triggerLogMapper.update(null, Wrappers.<TriggerLogRecord>lambdaUpdate()
                .set(TriggerLogRecord::getDirectiveStatus, status)
                .eq(TriggerLogRecord::getDirectiveId, directiveId));
    }

    @Override
    public boolean markDirectiveConsumedIfOpen(String directiveId) {
        if (!StringUtils.hasText(directiveId)) {
            return false;
        }
        LambdaUpdateWrapper<TriggerLogRecord> wrapper = Wrappers.<TriggerLogRecord>lambdaUpdate()
                .set(TriggerLogRecord::getDirectiveStatus, "CONSUMED")
                .eq(TriggerLogRecord::getDirectiveId, directiveId)
                .and(w -> w.isNull(TriggerLogRecord::getDirectiveStatus)
                        .or()
                        .ne(TriggerLogRecord::getDirectiveStatus, "CONSUMED"))
                .and(w -> w.isNull(TriggerLogRecord::getExpireAt)
                        .or()
                        .apply("expire_at >= CURRENT_TIMESTAMP"));
        return triggerLogMapper.update(null, wrapper) > 0;
    }
}
