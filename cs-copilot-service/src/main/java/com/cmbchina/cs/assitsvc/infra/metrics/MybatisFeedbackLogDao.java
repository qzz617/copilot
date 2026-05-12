package com.cmbchina.cs.assitsvc.infra.metrics;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cmbchina.cs.assitsvc.infra.metrics.mapper.FeedbackLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * MyBatis-Plus 反馈日志 DAO。
 */
@Repository
@RequiredArgsConstructor
public class MybatisFeedbackLogDao implements FeedbackLogDao {

    private final FeedbackLogMapper feedbackLogMapper;

    @Override
    public void insert(FeedbackLogRecord record) {
        feedbackLogMapper.insert(record);
    }

    @Override
    public boolean existsEffective(String directiveId) {
        if (!StringUtils.hasText(directiveId)) {
            return false;
        }
        Long count = feedbackLogMapper.selectCount(Wrappers.<FeedbackLogRecord>lambdaQuery()
                .eq(FeedbackLogRecord::getDirectiveId, directiveId)
                .eq(FeedbackLogRecord::getIsEffective, "Y"));
        return count != null && count > 0;
    }
}
