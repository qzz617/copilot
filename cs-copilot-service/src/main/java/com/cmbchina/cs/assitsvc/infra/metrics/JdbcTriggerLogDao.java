package com.cmbchina.cs.assitsvc.infra.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * JDBC 触发日志 DAO。
 */
@Repository
@RequiredArgsConstructor
public class JdbcTriggerLogDao implements TriggerLogDao {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void insert(TriggerLogRecord record) {
        String sql = "INSERT INTO svccfg.cs_copilot_trigger_log "
                + "(log_id, call_id, operator_id, customer_id, intent_code, intent_name, item_id, item_name, "
                + "candidate_count, risk_level, directive_id, expire_at, directive_status, ai_request_id, "
                + "ai_response_time_ms, ai_success, ai_failure_reason, result_status, reason_code, filter_stage, "
                + "missing_params_json, asr_confidence, asr_text_hash, trigger_time, config_version) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                record.getLogId(), record.getCallId(), record.getOperatorId(), record.getCustomerId(),
                record.getIntentCode(), record.getIntentName(), record.getItemId(), record.getItemName(),
                record.getCandidateCount(), record.getRiskLevel(), record.getDirectiveId(),
                toTimestamp(record.getExpireAt()), record.getDirectiveStatus(), record.getAiRequestId(),
                record.getAiResponseTimeMs(), record.getAiSuccess(), record.getAiFailureReason(),
                record.getResultStatus(), record.getReasonCode(), record.getFilterStage(),
                record.getMissingParamsJson(), record.getAsrConfidence(), record.getAsrTextHash(),
                toTimestamp(record.getTriggerTime()), record.getConfigVersion());
    }

    @Override
    public TriggerLogRecord findByDirectiveId(String directiveId) {
        if (!StringUtils.hasText(directiveId)) {
            return null;
        }
        String sql = "SELECT log_id, call_id, operator_id, customer_id, intent_code, intent_name, item_id, "
                + "item_name, candidate_count, risk_level, directive_id, expire_at, directive_status, "
                + "result_status, reason_code, filter_stage, trigger_time, config_version "
                + "FROM svccfg.cs_copilot_trigger_log WHERE directive_id = ?";
        List<TriggerLogRecord> records = jdbcTemplate.query(sql, new Object[]{directiveId}, this::mapRecord);
        return records.isEmpty() ? null : records.get(0);
    }

    @Override
    public void updateDirectiveStatus(String directiveId, String status) {
        if (!StringUtils.hasText(directiveId) || !StringUtils.hasText(status)) {
            return;
        }
        jdbcTemplate.update("UPDATE svccfg.cs_copilot_trigger_log SET directive_status = ? WHERE directive_id = ?",
                status, directiveId);
    }

    @Override
    public boolean markDirectiveConsumedIfOpen(String directiveId) {
        if (!StringUtils.hasText(directiveId)) {
            return false;
        }
        int updated = jdbcTemplate.update(
                "UPDATE svccfg.cs_copilot_trigger_log SET directive_status = 'CONSUMED' "
                        + "WHERE directive_id = ? "
                        + "AND (directive_status IS NULL OR directive_status <> 'CONSUMED') "
                        + "AND (expire_at IS NULL OR expire_at >= CURRENT_TIMESTAMP)",
                directiveId);
        return updated > 0;
    }

    private TriggerLogRecord mapRecord(ResultSet rs, int rowNum) throws SQLException {
        return TriggerLogRecord.builder()
                .logId(rs.getString("log_id"))
                .callId(rs.getString("call_id"))
                .operatorId(rs.getString("operator_id"))
                .customerId(rs.getString("customer_id"))
                .intentCode(rs.getString("intent_code"))
                .intentName(rs.getString("intent_name"))
                .itemId(getLong(rs, "item_id"))
                .itemName(rs.getString("item_name"))
                .candidateCount(getInteger(rs, "candidate_count"))
                .riskLevel(rs.getString("risk_level"))
                .directiveId(rs.getString("directive_id"))
                .expireAt(toInstant(rs.getTimestamp("expire_at")))
                .directiveStatus(rs.getString("directive_status"))
                .resultStatus(rs.getString("result_status"))
                .reasonCode(rs.getString("reason_code"))
                .filterStage(rs.getString("filter_stage"))
                .triggerTime(toInstant(rs.getTimestamp("trigger_time")))
                .configVersion(rs.getString("config_version"))
                .build();
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
