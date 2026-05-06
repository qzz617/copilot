-- =========================================================================
-- Customer Service Copilot Service - 数据库初始化脚本 V1
-- 文档版本：DD-V1.2
-- 数据库类型：PostgreSQL 占位（实际由 DBA 确认）
--
-- 复用存量表（不动）：
--   cs_menu_item, cs_menu_item_param, cs_menu_item_info,
--   cs_menu_group, cs_menu_module, cs_menu_module_item,
--   cs_menu_group_authority, cs_menu_version
--
-- 本期新增表（4 张）：
--   cs_menu_item_copilot_ext       Copilot 扩展配置
--   cs_copilot_intent_mapping      意图-功能映射
--   cs_copilot_trigger_log         触发日志（含 DD-V1.2 扩展字段）
--   cs_copilot_feedback_log        反馈日志（含 DD-V1.2 扩展字段）
-- =========================================================================

-- =========================================================================
-- Table: cs_menu_item_copilot_ext
-- Copilot 扩展配置表（与存量 cs_menu_item 1:1 关联）
-- =========================================================================
CREATE TABLE svccfg.cs_menu_item_copilot_ext (
    -- 关联键
    item_id                 numeric(131089,0) NOT NULL,

    -- 启用控制（DD-V1.2 简化：单字段，无状态机）
    copilot_enabled         char(1) DEFAULT 'N' NOT NULL,
                            -- Y/N

    -- 功能元信息
    function_type_code      varchar(32),
                            -- SELF_DEVELOPED / CROSS_SYSTEM / IFRAME
    function_path           varchar(256),

    -- 打开方式（5 种）
    target_kind             varchar(16),
                            -- URL / ROUTE / COMPONENT / IFRAME / NEW_WINDOW
    open_mode               varchar(16),
                            -- CURRENT_TAB / NEW_TAB / POPUP / DRAWER / WINDOW / IFRAME
    route_path              varchar(256),
    component_code          varchar(128),
    component_name          varchar(128),
    props_schema            text,
    fallback_url            varchar(512),
    window_feature          varchar(256),

    -- AI 展示文案
    ai_display_text         varchar(128),
    floating_tip_text       varchar(256),
    trigger_keywords        text,
                            -- JSON 数组，仅用于运营检索/解释，非运行时触发条件

    -- 风险等级（人工指定）
    risk_level              varchar(16) DEFAULT 'LOW',
                            -- LOW / MEDIUM / HIGH / DISABLED

    -- 条件规则
    condition_rule          text,

    -- UI
    icon_url                varchar(256),
    sort_order              numeric(8,0) DEFAULT 0,

    -- 审计
    created_by              varchar(32),
    created_name            varchar(40),
    created_time            timestamp,
    updated_by              varchar(32),
    updated_name            varchar(40),
    updated_time            timestamp,

    CONSTRAINT cs_menu_item_copilot_ext_pkey PRIMARY KEY (item_id),
    CONSTRAINT cs_menu_item_copilot_ext_fk
        FOREIGN KEY (item_id) REFERENCES svccfg.cs_menu_item(item_id)
);

CREATE INDEX idx_copilot_ext_enabled
    ON svccfg.cs_menu_item_copilot_ext(copilot_enabled);

COMMENT ON TABLE svccfg.cs_menu_item_copilot_ext IS 'Copilot 扩展配置表';

-- =========================================================================
-- Table: cs_copilot_intent_mapping
-- 意图-功能映射表
-- =========================================================================
CREATE TABLE svccfg.cs_copilot_intent_mapping (
    mapping_id              varchar(64) NOT NULL,

    -- AI 意图
    standard_intent_code    varchar(64) NOT NULL,
                            -- 对应 AI 接口的 intentCode（字符串，如 INTENT_BILL_QUERY）
    standard_intent_name    varchar(256),

    -- 关联功能
    item_id                 numeric(131089,0) NOT NULL,

    -- 映射优先级（同一 intentCode 多候选时使用）
    mapping_priority        numeric(5,0) DEFAULT 0,

    -- 附加条件
    condition_rule          text,

    -- 启用
    enabled                 char(1) DEFAULT 'Y' NOT NULL,

    -- 审计
    created_by              varchar(32),
    created_name            varchar(40),
    created_time            timestamp,
    updated_by              varchar(32),
    updated_name            varchar(40),
    updated_time            timestamp,

    CONSTRAINT cs_copilot_intent_mapping_pkey PRIMARY KEY (mapping_id),
    CONSTRAINT cs_copilot_intent_mapping_fk
        FOREIGN KEY (item_id) REFERENCES svccfg.cs_menu_item(item_id),
    CONSTRAINT uq_intent_item UNIQUE (standard_intent_code, item_id)
);

CREATE INDEX idx_intent_mapping_intent
    ON svccfg.cs_copilot_intent_mapping(standard_intent_code, enabled);

COMMENT ON TABLE svccfg.cs_copilot_intent_mapping IS 'Copilot 意图-功能映射表';

-- =========================================================================
-- Table: cs_copilot_trigger_log
-- 触发日志表（DD-V1.2 大幅扩展字段）
-- =========================================================================
CREATE TABLE svccfg.cs_copilot_trigger_log (
    log_id                  varchar(64) NOT NULL,

    -- 上下文
    call_id                 varchar(64) NOT NULL,
    operator_id             varchar(32),                -- DD-V1.2 P2-2 长度 16->32
    customer_id             varchar(64),

    -- 意图识别
    intent_code             varchar(64),
    intent_name             varchar(256),

    -- 匹配结果
    item_id                 numeric(131089,0),
    item_name               varchar(255),
    candidate_count         numeric(5,0),

    -- 推荐策略
    risk_level              varchar(16),
    directive_id            varchar(64),
    expire_at               timestamp,                  -- DD-V1.2 P0-6 指令过期时间
    directive_status        varchar(16),                -- DD-V1.2 P0-6 PUSHED/EXPIRED/CONSUMED

    -- AI 调用情况
    ai_request_id           varchar(64),
    ai_response_time_ms     numeric(8,0),
    ai_success              char(1),
    ai_failure_reason       varchar(64),                -- DD-V1.2 P2-10 失败分类

    -- 业务结果（DD-V1.2 P1-16）
    result_status           varchar(32),                -- SUCCESS/FAIL/FILTERED
    reason_code             varchar(64),                -- 详见 reason_code 枚举
    filter_stage            varchar(32),                -- 过滤发生的阶段
    missing_params_json     text,                       -- JSON 数组

    -- ASR 相关（DD-V1.2 P1-16）
    asr_confidence          numeric(5,4),               -- ASR 平均置信度
    asr_text_hash           varchar(64),                -- 不存原文，存哈希

    -- 通用
    trigger_time            timestamp,
    config_version          varchar(32),

    CONSTRAINT cs_copilot_trigger_log_pkey PRIMARY KEY (log_id)
);

-- DD-V1.2 P0-6 + P1-15：directive_id 唯一索引（保证反查稳定）
CREATE UNIQUE INDEX uq_trigger_log_directive
    ON svccfg.cs_copilot_trigger_log(directive_id)
    WHERE directive_id IS NOT NULL;

CREATE INDEX idx_trigger_log_call ON svccfg.cs_copilot_trigger_log(call_id);
CREATE INDEX idx_trigger_log_intent
    ON svccfg.cs_copilot_trigger_log(intent_code, trigger_time);
CREATE INDEX idx_trigger_log_time ON svccfg.cs_copilot_trigger_log(trigger_time);
CREATE INDEX idx_trigger_log_result
    ON svccfg.cs_copilot_trigger_log(result_status, reason_code, trigger_time);

COMMENT ON TABLE svccfg.cs_copilot_trigger_log IS 'Copilot 触发日志';

-- =========================================================================
-- Table: cs_copilot_feedback_log
-- 反馈日志表（DD-V1.2 调整）
-- =========================================================================
CREATE TABLE svccfg.cs_copilot_feedback_log (
    log_id                  varchar(64) NOT NULL,

    -- 关联触发记录
    directive_id            varchar(64) NOT NULL,
    trigger_log_id          varchar(64),                -- DD-V1.2 P1-15 后端反查填充

    -- 上下文
    call_id                 varchar(64) NOT NULL,
    operator_id             varchar(32),                -- DD-V1.2 P2-2 长度 16->32

    -- 反馈内容
    feedback_type           varchar(32) NOT NULL,
                            -- ACCEPTED / IGNORED / WRONG_INTENT / WRONG_FUNCTION
    intent_code             varchar(64),
    item_id                 numeric(131089,0),
    frontend_reason         varchar(64),                -- DD-V1.2 P2-10 前端附加原因

    -- 幂等控制（DD-V1.2 P1-14）
    is_effective            char(1) DEFAULT 'Y' NOT NULL,
                            -- Y: 首次有效反馈，影响业务状态
                            -- N: 重复反馈，仅记录不影响

    -- 时间
    feedback_time           timestamp,

    CONSTRAINT cs_copilot_feedback_log_pkey PRIMARY KEY (log_id)
);

CREATE INDEX idx_feedback_log_directive
    ON svccfg.cs_copilot_feedback_log(directive_id);
CREATE INDEX idx_feedback_log_time
    ON svccfg.cs_copilot_feedback_log(feedback_time);
CREATE INDEX idx_feedback_log_call
    ON svccfg.cs_copilot_feedback_log(call_id);
CREATE INDEX idx_feedback_log_effective
    ON svccfg.cs_copilot_feedback_log(directive_id, is_effective);

COMMENT ON TABLE svccfg.cs_copilot_feedback_log IS 'Copilot 反馈日志';

-- =========================================================================
-- 初始化样例数据（开发环境）
-- 上线前由运营在配置后台维护，此处仅为开发/测试参考
-- =========================================================================

-- 示例：境外行程报备
-- 假设 cs_menu_item 中已存在 item_id=12345
-- INSERT INTO svccfg.cs_menu_item_copilot_ext
--     (item_id, copilot_enabled, function_type_code, function_path,
--      target_kind, open_mode, ai_display_text, floating_tip_text,
--      risk_level, sort_order, created_by, created_time, updated_by, updated_time)
-- VALUES
--     (12345, 'Y', 'CROSS_SYSTEM', '信用卡风险侦测系统-参数设置-行程报备登记',
--      'IFRAME', 'IFRAME', '境外行程报备', '您正离开信用卡客服系统',
--      'MEDIUM', 1, 'admin', NOW(), 'admin', NOW());
--
-- INSERT INTO svccfg.cs_copilot_intent_mapping
--     (mapping_id, standard_intent_code, standard_intent_name, item_id,
--      mapping_priority, enabled, created_by, created_time, updated_by, updated_time)
-- VALUES
--     ('m_001', 'INTENT_TRAVEL_DECLARE', '境外行程报备', 12345,
--      10, 'Y', 'admin', NOW(), 'admin', NOW());
