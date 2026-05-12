package com.cmbchina.cs.assitsvc.infra.metrics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmbchina.cs.assitsvc.infra.metrics.FeedbackLogRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * cs_copilot_feedback_log Mapper。
 */
@Mapper
public interface FeedbackLogMapper extends BaseMapper<FeedbackLogRecord> {
}
