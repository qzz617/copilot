package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 指令-展示信息，DirectiveDTO 的 display 子对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisplayInfo {

    /** 推荐卡片标题 */
    private String title;

    /** 跳转前提示文案 */
    private String tip;

    /** 图标 URL */
    private String iconUrl;
}
