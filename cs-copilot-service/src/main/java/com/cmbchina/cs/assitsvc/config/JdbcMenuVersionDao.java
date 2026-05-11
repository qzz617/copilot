package com.cmbchina.cs.assitsvc.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 基于 JDBC 的菜单版本 CLOB 读取 DAO。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JdbcMenuVersionDao implements MenuVersionDao {

    private static final String LATEST_CLOB_SQL =
            "SELECT config_data FROM svccfg.cs_menu_version ORDER BY created_time DESC LIMIT 1";
    private static final String LATEST_VERSION_SQL =
            "SELECT version FROM svccfg.cs_menu_version ORDER BY created_time DESC LIMIT 1";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String fetchLatestActiveVersion() {
        return jdbcTemplate.query(LATEST_CLOB_SQL, this::extractFirstString);
    }

    @Override
    public String fetchLatestVersionMarker() {
        return jdbcTemplate.query(LATEST_VERSION_SQL, this::extractFirstString);
    }

    private String extractFirstString(ResultSet rs) throws SQLException {
        if (!rs.next()) {
            return null;
        }
        return rs.getString(1);
    }
}
