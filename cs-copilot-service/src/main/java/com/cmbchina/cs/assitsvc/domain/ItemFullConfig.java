package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 功能完整配置，对应 CLOB copilotIndex.itemById 中的单条记录。
 *
 * <p>字段优先级说明：{@link #riskLevel}、{@link #targetKind}、{@link #openMode} 与
 * {@link #copilotExt} 中的同名字段存在重叠（反映存量表与扩展表的双重来源）。
 * 运行时以 {@code copilotExt} 中的值为准，本类冗余字段仅供快速索引使用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemFullConfig {

    /** 功能 ID */
    private Long itemId;

    /** 功能名称 */
    private String itemName;

    /** 跳转 URL（含占位符） */
    private String url;

    /** 系统标识，如 FITS */
    private String sysFlag;

    /** 页面 ID */
    private String pageId;

    /** Copilot 是否启用 */
    private Boolean copilotEnabled;

    /** 风险等级：LOW / MEDIUM / HIGH / DISABLED */
    private String riskLevel;

    /** 打开目标类型 */
    private String targetKind;

    /** 打开方式 */
    private String openMode;

    /** 功能参数列表 */
    private List<ItemParam> params;

    /** Copilot 扩展配置 */
    private CopilotExt copilotExt;
}
