package com.cmbchina.cs.assitsvc.asr;

import com.cmbchina.cs.assitsvc.domain.AsrSentenceEvent;
import com.cmbchina.cs.assitsvc.domain.DialogMessage;

import java.util.List;

/**
 * 对话历史管理，负责将 ASR 事件写入 Redis 并供 M06 读取。
 *
 * <p>存储所有说话方（customer + agent），不做过滤；过滤职责在 M06。
 */
public interface DialogHistoryManager {

    /**
     * 追加一条 ASR 事件到通话历史。
     * 超出 max-size 时自动裁剪最旧的条目；每次调用刷新 TTL。
     *
     * @param callId 通话 ID，不可为 null/空
     * @param event  ASR 事件，不可为 null
     * @throws IllegalArgumentException 若 callId 或 event 为 null/空
     */
    void append(String callId, AsrSentenceEvent event);

    /**
     * 返回指定通话的完整历史列表，按追加顺序排列。
     * Redis 不可达时返回空列表，不抛异常。callId 无历史记录时同样返回空列表。
     *
     * @param callId 通话 ID，不可为 null/空
     * @return 对话消息列表（可能为空，不为 null）
     * @throws IllegalArgumentException 若 callId 为 null/空
     */
    List<DialogMessage> getHistory(String callId);

    /**
     * 删除通话历史（通话结束时调用）。
     * Redis 不可达时仅记录 WARN，不抛异常。
     *
     * @param callId 通话 ID，不可为 null/空
     * @throws IllegalArgumentException 若 callId 为 null/空
     */
    void cleanup(String callId);
}
