package com.cmbchina.cs.assitsvc.infra.metrics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmbchina.cs.assitsvc.infra.metrics.TriggerLogRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * cs_copilot_trigger_log Mapper。
 */
@Mapper
public interface TriggerLogMapper extends BaseMapper<TriggerLogRecord> {
}
