package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Copilot 扩展配置，对应 cs_menu_item_copilot_ext 表，同时作为 CLOB 中 copilotExt 节点。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopilotExt {

    /** 关联功能 ID（DAO 场景使用） */
    private Long itemId;

    /** Copilot 是否启用（CLOB 中为 boolean，DB 中为 Y/N） */
    private Boolean enabled;

    /** 功能类型代码：SELF_DEVELOPED / CROSS_SYSTEM 等 */
    private String functionTypeCode;

    /** 运营标注的功能路径，用于卡片展示 */
    private String functionPath;

    /** 打开目标类型：URL / ROUTE / COMPONENT / IFRAME / NEW_WINDOW */
    private String targetKind;

    /** 打开方式：CURRENT_TAB / NEW_TAB / POPUP / DRAWER / WINDOW / IFRAME */
    private String openMode;

    /** Vue 路由路径（targetKind=ROUTE 时填） */
    private String routePath;

    /** 组件唯一编码（targetKind=COMPONENT 时填） */
    private String componentCode;

    /** 组件显示名（targetKind=COMPONENT 时填） */
    private String componentName;

    /** 组件 Props JSON Schema */
    private String propsSchema;

    /** 组件加载失败时的兜底 URL */
    private String fallbackUrl;

    /** 新窗口特性（targetKind=NEW_WINDOW 时填） */
    private String windowFeature;

    /** 浮窗 AI 展示文字（Copilot 启用时必填） */
    private String aiDisplayText;

    /** 跳转前提示文案 */
    private String floatingTipText;

    /** 关键词列表，运营检索/解释推荐用 */
    private List<String> triggerKeywords;

    /** 风险等级：LOW / MEDIUM / HIGH / DISABLED */
    private String riskLevel;

    /** 条件规则 JSON 字符串 */
    private String conditionRule;

    /** 图标 URL */
    private String iconUrl;

    /** UI 排序权重 */
    private Integer sortOrder;

    /** 同意图多候选优先级（来自 cs_copilot_intent_mapping.mapping_priority） */
    private Integer mappingPriority;
}
