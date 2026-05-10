package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 意图树节点，从 classpath:intent-tree.json 加载。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentTreeNode {

    /** 意图代码 */
    private String intentCode;

    /** 意图名称 */
    private String intentName;

    /** 父意图代码 */
    private String parentIntentCode;

    /** 子节点列表 */
    private List<IntentTreeNode> children;
}
