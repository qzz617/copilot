package com.cmbchina.cs.assitsvc.asr;

import com.cmbchina.cs.assitsvc.core.intent.IntentRecognitionTrigger;
import com.cmbchina.cs.assitsvc.domain.AsrSentenceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 句间合并防抖器。
 *
 * <p>同一 callId 的新客户句子到达时，会重置防抖 timer 和沉默 timer。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SentenceMerger {

    private final Map<String, ScheduledFuture<?>> debounceTimers = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> silenceTimers = new ConcurrentHashMap<>();
    private final Map<String, Long> activeRounds = new ConcurrentHashMap<>();
    private final AtomicLong roundSequence = new AtomicLong();

    private final DebounceProperties props;
    private final SentenceContinuityDetector detector;
    private final ScheduledExecutorService scheduler;
    private final IntentRecognitionTrigger trigger;

    /**
     * 处理一条可触发意图识别的客户 ASR 句子。
     *
     * @param event ASR 事件，callId 不可为空
     */
    public void handleSentence(AsrSentenceEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (!StringUtils.hasText(event.getCallId())) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }

        String callId = event.getCallId();
        SentenceContinuity continuity = detector.detect(event.getContent());
        long debounceMs = debounceMs(continuity);
        long silenceMs = Math.max(0L, props.getSilenceMs());
        long roundId = roundSequence.incrementAndGet();

        activeRounds.put(callId, roundId);
        cancelTimer(debounceTimers.remove(callId));
        cancelTimer(silenceTimers.remove(callId));

        debounceTimers.put(callId, scheduler.schedule(
                new TriggerTask(callId, roundId, "DEBOUNCE"), debounceMs, TimeUnit.MILLISECONDS));
        silenceTimers.put(callId, scheduler.schedule(
                new TriggerTask(callId, roundId, "SILENCE"), silenceMs, TimeUnit.MILLISECONDS));

        log.debug("[M02] Sentence timer reset, callId={}, continuity={}, debounceMs={}, silenceMs={}",
                callId, continuity, debounceMs, silenceMs);
    }

    /**
     * 清理指定通话的防抖状态。
     *
     * @param callId 通话 ID
     */
    public void cleanup(String callId) {
        if (!StringUtils.hasText(callId)) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }

        activeRounds.remove(callId);
        cancelTimer(debounceTimers.remove(callId));
        cancelTimer(silenceTimers.remove(callId));
    }

    private void triggerIfCurrent(String callId, long roundId, String source) {
        if (!activeRounds.remove(callId, roundId)) {
            return;
        }

        cancelTimer(debounceTimers.remove(callId));
        cancelTimer(silenceTimers.remove(callId));

        try {
            log.debug("[M02] Intent recognition triggered, callId={}, source={}", callId, source);
            trigger.fire(callId);
        } catch (Exception e) {
            log.error("[M02] Intent recognition trigger failed, callId={}, source={}", callId, source, e);
        }
    }

    private long debounceMs(SentenceContinuity continuity) {
        if (SentenceContinuity.COMPLETE == continuity) {
            return Math.max(0L, props.getCompleteMs());
        }
        if (SentenceContinuity.INCOMPLETE == continuity) {
            return Math.max(0L, props.getIncompleteMs());
        }
        return Math.max(0L, props.getNeutralMs());
    }

    private static void cancelTimer(ScheduledFuture<?> future) {
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }

    private class TriggerTask implements Runnable {
        private final String callId;
        private final long roundId;
        private final String source;

        private TriggerTask(String callId, long roundId, String source) {
            this.callId = callId;
            this.roundId = roundId;
            this.source = source;
        }

        @Override
        public void run() {
            triggerIfCurrent(callId, roundId, source);
        }
    }
}
