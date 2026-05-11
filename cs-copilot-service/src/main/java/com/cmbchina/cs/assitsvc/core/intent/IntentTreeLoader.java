package com.cmbchina.cs.assitsvc.core.intent;

import com.cmbchina.cs.assitsvc.domain.IntentTreeNode;

import java.time.Instant;

/**
 * 意图树加载器。
 */
public interface IntentTreeLoader {

    /**
     * 重新加载意图树配置。
     */
    void reload();

    /**
     * 获取当前缓存的意图树。
     *
     * @return 意图树根节点
     */
    IntentTreeNode getTree();

    /**
     * 获取意图树版本号。
     *
     * @return 版本号
     */
    String getVersion();

    /**
     * 获取当前意图树节点总数。
     *
     * @return 节点数
     */
    int getNodeCount();

    /**
     * 获取最近一次加载时间。
     *
     * @return 最近一次加载时间
     */
    Instant getLastLoadTime();
}
