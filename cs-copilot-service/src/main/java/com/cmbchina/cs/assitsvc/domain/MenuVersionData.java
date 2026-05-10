package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.alibaba.fastjson2.JSONObject;

import java.util.List;

/**
 * CLOB 数据，对应 cs_menu_version.config_clob 的顶层结构。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuVersionData {

    /** 版本号，如 20260424.001 */
    private String version;

    /** CLOB 生成时间，ISO 8601 格式 */
    private String configBuildTime;

    /** 存量菜单树（groups → modules → items），Copilot 侧不解析，保持 JSONObject 原始形态 */
    private List<JSONObject> groups;

    /** Copilot 反向索引，由发布时计算生成 */
    private CopilotIndex copilotIndex;
}
