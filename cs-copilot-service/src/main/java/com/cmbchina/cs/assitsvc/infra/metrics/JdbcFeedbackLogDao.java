package com.cmbchina.cs.assitsvc.infra.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * JDBC 反馈日志 DAO。
 */
@Repository
@RequiredArgsConstructor
public class JdbcFeedbackLogDao implements FeedbackLogDao {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void insert(FeedbackLogRecord record) {
        String sql = "INSERT INTO svccfg.cs_copilot_feedback_log "
                + "(log_id, directive_id, trigger_log_id, call_id, operator_id, feedback_type, intent_code, "
                + "item_id, frontend_reason, is_effective, feedback_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                record.getLogId(), record.getDirectiveId(), record.getTriggerLogId(), record.getCallId(),
                record.getOperatorId(), record.getFeedbackType(), record.getIntentCode(), record.getItemId(),
                record.getFrontendReason(), record.getIsEffective(), toTimestamp(record.getFeedbackTime()));
    }

    @Override
    public boolean existsEffective(String directiveId) {
        if (!StringUtils.hasText(directiveId)) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM svccfg.cs_copilot_feedback_log "
                        + "WHERE directive_id = ? AND is_effective = 'Y'",
                Integer.class,
                directiveId);
        return count != null && count > 0;
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
