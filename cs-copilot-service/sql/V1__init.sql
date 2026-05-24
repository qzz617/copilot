-- =========================================================================
-- Customer Service Copilot Service - 数据库初始化脚本 V1
-- 文档版本：DD-V1.2
-- 数据库类型：TDSQL（PG 兼容方言）
--
-- 复用存量表（不动）：
--   cs_menu_item, cs_menu_item_param, cs_menu_item_info,
--   cs_menu_group, cs_menu_module, cs_menu_module_item,
--   cs_menu_group_authority, cs_menu_version
--
-- 本期新增表（3 张）：
--   cs_copilot_config_version      Copilot 配置版本
--   cs_copilot_action              Copilot 动作配置
--   cs_copilot_intent_mapping      意图-动作映射
--
-- MVP 阶段不再建以下表（数据改由应用日志 + ES 承担）：
--   cs_copilot_trigger_log         触发日志（改为 log.info JSON 行）
--   cs_copilot_feedback_log        反馈日志（改为写 ES）
-- =========================================================================

-- =========================================================================
-- Table: cs_copilot_config_version
-- Copilot 配置版本表；独立于 cs_menu_version
-- =========================================================================
CREATE TABLE svccfg.cs_copilot_config_version (
    version_id              varchar(32) NOT NULL,
    publish_status          varchar(16) DEFAULT 'PUBLISHED' NOT NULL,
                            -- PUBLISHED / DISABLED
    change_summary          varchar(512),

    -- 审计
    created_by              varchar(32) NOT NULL,
    created_name            varchar(40) NOT NULL,
    created_time            timestamp NOT NULL,

    CONSTRAINT cs_copilot_config_version_pkey PRIMARY KEY (version_id)
);

CREATE INDEX idx_copilot_config_version_time
    ON svccfg.cs_copilot_config_version(publish_status, created_time);

COMMENT ON TABLE svccfg.cs_copilot_config_version IS 'Copilot 配置版本表';

-- =========================================================================
-- Table: cs_copilot_action
-- Copilot 动作配置表；menu_item_id 可选，仅复用快捷导航时填写
-- =========================================================================
CREATE TABLE svccfg.cs_copilot_action (
    version_id              varchar(32) NOT NULL,
    action_id               varchar(64) NOT NULL,

    -- 可选快捷导航关联，不加外键，发布流程负责校验
    menu_item_id            numeric(20,0),
    item_snapshot_json      text,

    -- 动作基本信息
    action_name             varchar(128) NOT NULL,
    enabled                 char(1) DEFAULT 'Y' NOT NULL,
    function_path           varchar(256),

    -- 打开方式
    target_kind             varchar(16),
                            -- URL / ROUTE / IFRAME / NEW_WINDOW
    open_mode               varchar(16),
                            -- CURRENT_TAB / NEW_TAB / WINDOW / IFRAME
    target_url              varchar(512),
    route_path              varchar(256),
    window_feature          varchar(256),

    -- AI 展示文案
    ai_display_text         varchar(128) NOT NULL,
    floating_tip_text       varchar(256),

    -- 风险等级（人工指定）
    risk_level              varchar(16) DEFAULT 'LOW' NOT NULL,
                            -- LOW / MEDIUM / HIGH / DISABLED

    -- UI
    icon_url                varchar(256),

    -- 非快捷导航动作的参数配置，JSON 数组，结构同 ItemParam
    param_config_json       text,

    -- 审计
    created_by              varchar(32),
    created_name            varchar(40),
    created_time            timestamp,
    updated_by              varchar(32),
    updated_name            varchar(40),
    updated_time            timestamp,

    CONSTRAINT cs_copilot_action_pkey PRIMARY KEY (version_id, action_id)
);

CREATE INDEX idx_copilot_action_version_enabled
    ON svccfg.cs_copilot_action(version_id, enabled);

CREATE INDEX idx_copilot_action_menu_item
    ON svccfg.cs_copilot_action(version_id, menu_item_id);

COMMENT ON TABLE svccfg.cs_copilot_action IS 'Copilot 动作配置表';

-- =========================================================================
-- Table: cs_copilot_intent_mapping
-- 意图-动作映射表
-- =========================================================================
CREATE TABLE svccfg.cs_copilot_intent_mapping (
    version_id              varchar(32) NOT NULL,
    mapping_id              varchar(64) NOT NULL,

    -- AI 意图
    standard_intent_code    varchar(64) NOT NULL,
                            -- 对应 AI 接口的 intentCode（字符串，如 INTENT_BILL_QUERY）
    standard_intent_name    varchar(256),

    -- 关联 Copilot 动作，不加外键，发布流程负责校验
    action_id               varchar(64) NOT NULL,

    -- 映射优先级（同一 intentCode 多候选时使用）
    mapping_priority        numeric(5,0) DEFAULT 0,

    -- 启用
    enabled                 char(1) DEFAULT 'Y' NOT NULL,

    -- 审计
    created_by              varchar(32),
    created_name            varchar(40),
    created_time            timestamp,
    updated_by              varchar(32),
    updated_name            varchar(40),
    updated_time            timestamp,

    CONSTRAINT cs_copilot_intent_mapping_pkey PRIMARY KEY (version_id, mapping_id),
    CONSTRAINT uq_intent_action UNIQUE (version_id, standard_intent_code, action_id)
);

CREATE INDEX idx_intent_mapping_intent
    ON svccfg.cs_copilot_intent_mapping(version_id, standard_intent_code, enabled);

CREATE INDEX idx_intent_mapping_action
    ON svccfg.cs_copilot_intent_mapping(version_id, action_id);

COMMENT ON TABLE svccfg.cs_copilot_intent_mapping IS 'Copilot 意图-动作映射表';

-- =========================================================================
-- 初始化样例数据（开发环境）
-- 上线前由运营在配置后台维护，此处仅为开发/测试参考
-- =========================================================================

-- 示例：境外行程报备（纯意图唤起，不依赖快捷导航）
-- INSERT INTO svccfg.cs_copilot_action
--     (version_id, action_id, menu_item_id, action_name, enabled, function_path,
--      target_kind, open_mode, target_url, ai_display_text, floating_tip_text,
--      risk_level, created_by, created_name, created_time, updated_by, updated_name, updated_time)
-- VALUES
--     ('202605130001', 'act_travel_declare', NULL, '境外行程报备', 'Y', '信用卡风险侦测系统-参数设置-行程报备登记',
--      'IFRAME', 'IFRAME', 'https://frdctrfront.paas.cmbchina.cn/travel-declare',
--      '境外行程报备', '将为坐席打开境外行程报备页面',
--      'MEDIUM', 'admin', '系统管理员', NOW(), 'admin', '系统管理员', NOW());
--
-- INSERT INTO svccfg.cs_copilot_intent_mapping
--     (version_id, mapping_id, standard_intent_code, standard_intent_name, action_id,
--      mapping_priority, enabled, created_by, created_name, created_time, updated_by, updated_name, updated_time)
-- VALUES
--     ('202605130001', 'm_001', 'INTENT_TRAVEL_DECLARE', '境外行程报备', 'act_travel_declare',
--      10, 'Y', 'admin', '系统管理员', NOW(), 'admin', '系统管理员', NOW());
--
-- action/mapping 快照校验通过后，最后插入版本发布行。
-- INSERT INTO svccfg.cs_copilot_config_version
--     (version_id, publish_status, change_summary, created_by, created_name, created_time)
-- VALUES
--     ('202605130001', 'PUBLISHED', '初始化 Copilot 配置', 'admin', '系统管理员', NOW());
