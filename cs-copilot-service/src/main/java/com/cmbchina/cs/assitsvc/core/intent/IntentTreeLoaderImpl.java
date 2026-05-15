package com.cmbchina.cs.assitsvc.core.intent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.cmbchina.cs.assitsvc.domain.IntentTreeNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * 基于 Spring Resource 的意图树加载器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentTreeLoaderImpl implements IntentTreeLoader {

    private final IntentTreeProperties props;

    private volatile IntentTreeSnapshot snapshot = new IntentTreeSnapshot(null, 0, null);

    /**
     * 启动时加载 classpath 中的意图树配置。
     */
    @PostConstruct
    public void init() {
        reload();
    }

    @Override
    public synchronized void reload() {
        IntentTreeNode newTree = loadTree();
        int newNodeCount = countNodes(newTree);
        Instant newLoadTime = Instant.now();

        this.snapshot = new IntentTreeSnapshot(newTree, newNodeCount, newLoadTime);

        log.info("[M05] Intent tree loaded, version={}, nodeCount={}, loadTime={}",
                props.getVersion(), newNodeCount, newLoadTime);
    }

    @Override
    public IntentTreeNode getTree() {
        return snapshot.getTree();
    }

    @Override
    public String getVersion() {
        return props.getVersion();
    }

    @Override
    public int getNodeCount() {
        return snapshot.getNodeCount();
    }

    @Override
    public Instant getLastLoadTime() {
        return snapshot.getLastLoadTime();
    }

    private IntentTreeNode loadTree() {
        try (InputStream inputStream = props.getFile().getInputStream()) {
            String content = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            IntentTreeNode root = JSON.parseObject(content, IntentTreeNode.class);
            validateRoot(root);
            return root;
        } catch (IOException e) {
            throw new IllegalStateException("Load intent tree failed", e);
        } catch (JSONException e) {
            throw new IllegalStateException("Parse intent tree failed", e);
        }
    }

    private static void validateRoot(IntentTreeNode root) {
        if (root == null) {
            throw new IllegalStateException("Intent tree root must not be null");
        }
        if (!StringUtils.hasText(root.getIntentCode())) {
            throw new IllegalStateException("Intent tree root intentCode must not be empty");
        }
        if (!StringUtils.hasText(root.getIntentName())) {
            throw new IllegalStateException("Intent tree root intentName must not be empty");
        }
    }

    private static int countNodes(IntentTreeNode node) {
        if (node == null) {
            return 0;
        }

        int count = 1;
        List<IntentTreeNode> children = node.getChildren();
        if (children == null || children.isEmpty()) {
            return count;
        }

        for (IntentTreeNode child : children) {
            count += countNodes(child);
        }
        return count;
    }

    private static class IntentTreeSnapshot {
        private final IntentTreeNode tree;
        private final int nodeCount;
        private final Instant lastLoadTime;

        private IntentTreeSnapshot(IntentTreeNode tree, int nodeCount, Instant lastLoadTime) {
            this.tree = tree;
            this.nodeCount = nodeCount;
            this.lastLoadTime = lastLoadTime;
        }

        private IntentTreeNode getTree() {
            return tree;
        }

        private int getNodeCount() {
            return nodeCount;
        }

        private Instant getLastLoadTime() {
            return lastLoadTime;
        }
    }
}
