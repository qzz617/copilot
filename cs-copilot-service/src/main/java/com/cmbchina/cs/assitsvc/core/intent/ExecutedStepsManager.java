package com.cmbchina.cs.assitsvc.core.intent;

import com.alibaba.fastjson2.JSON;
import com.cmbchina.cs.assitsvc.domain.ExecutedStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 已执行步骤管理器。
 *
 * <p>记录同一通话内坐席 ACCEPTED 过的 (intentCode, actionId) 组合，供下次意图识别时
 * 传给 AI 作为"已处理过"的提示，避免 AI 重复推荐同一步骤。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutedStepsManager {

    private static final String KEY_PREFIX = "copilot:steps:{";
    private static final String KEY_SUFFIX = "}";
    private static final int STEPS_TTL_SECONDS = 3600;

    /**
     * Lua 脚本：RPUSH + EXPIRE 原子化。
     * <pre>
     * KEYS[1] = list key
     * ARGV[1] = JSON 步骤
     * ARGV[2] = TTL（秒）
     * </pre>
     */
    private static final DefaultRedisScript<Long> APPEND_SCRIPT = new DefaultRedisScript<>(
            "redis.call('RPUSH', KEYS[1], ARGV[1]); "
                    + "redis.call('EXPIRE', KEYS[1], ARGV[2]); "
                    + "return 1;",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * 追加已执行步骤。
     *
     * <p>幂等：若 (intentCode, actionId) 组合已存在，本次调用不追加，仅刷新 TTL。
     * 避免坐席多次点击 ACCEPTED 导致 AI 收到重复噪音。
     *
     * @param callId     通话 ID
     * @param intentCode 意图代码
     * @param intentName 意图名称（可为空）
     * @param actionId   动作 ID
     * @param actionName 动作名称（可为空）
     */
    public void appendStep(String callId, String intentCode, String intentName,
                           String actionId, String actionName) {
        if (!StringUtils.hasText(callId) || !StringUtils.hasText(intentCode)) {
            throw new IllegalArgumentException("callId and intentCode must not be null or empty");
        }

        String key = key(callId);
        try {
            // 先检查是否已存在相同 (intentCode, actionId)，避免重复追加
            if (existsStep(key, intentCode, actionId)) {
                log.debug("[M06] Executed step already exists, skip append, callId={}, intentCode={}, actionId={}",
                        callId, intentCode, actionId);
                // 仍刷新 TTL：坐席持续操作 = 通话仍活跃
                redisTemplate.expire(key, STEPS_TTL_SECONDS, TimeUnit.SECONDS);
                return;
            }

            ExecutedStep step = ExecutedStep.builder()
                    .intentCode(intentCode)
                    .intentName(intentName)
                    .actionId(actionId)
                    .actionName(actionName)
                    .timestamp(System.currentTimeMillis())
                    .build();
            redisTemplate.execute(APPEND_SCRIPT,
                    Collections.singletonList(key),
                    JSON.toJSONString(step),
                    String.valueOf(STEPS_TTL_SECONDS));
        } catch (DataAccessException e) {
            log.warn("[M06] Redis append executed step failed, callId={}, intentCode={}, actionId={}",
                    callId, intentCode, actionId, e);
        }
    }

    /**
     * 查询已执行步骤。
     *
     * @param callId 通话 ID
     * @return 已执行步骤列表
     */
    public List<ExecutedStep> getSteps(String callId) {
        if (!StringUtils.hasText(callId)) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }

        List<String> rawList;
        try {
            rawList = redisTemplate.opsForList().range(key(callId), 0, -1);
        } catch (DataAccessException e) {
            log.warn("[M06] Redis get executed steps failed, callId={}", callId, e);
            return Collections.emptyList();
        }
        if (rawList == null) {
            return Collections.emptyList();
        }

        List<ExecutedStep> result = new ArrayList<>();
        for (String raw : rawList) {
            try {
                result.add(JSON.parseObject(raw, ExecutedStep.class));
            } catch (Exception e) {
                log.warn("[M06] Parse executed step failed, callId={}, raw={}", callId, raw, e);
            }
        }
        return result;
    }

    /**
     * 通话结束时调用。
     *
     * <p><b>行内规范</b>：Redis 不使用 delete 等阻塞命令，临时数据完全依赖 TTL 自动过期清理。
     * 本方法仅保留日志和方法签名，作为通话生命周期事件钩子；如未来引入其他清理动作可在此扩展。
     */
    public void cleanup(String callId) {
        if (!StringUtils.hasText(callId)) {
            log.debug("[M06] Cleanup skipped on blank callId");
            return;
        }
        log.debug("[M06] Cleanup invoked, relying on TTL expiration, callId={}", callId);
    }

    /**
     * 检查 List 中是否已存在 (intentCode, actionId) 相同的步骤。
     */
    private boolean existsStep(String key, String intentCode, String actionId) {
        List<String> rawList = redisTemplate.opsForList().range(key, 0, -1);
        if (rawList == null || rawList.isEmpty()) {
            return false;
        }
        for (String raw : rawList) {
            try {
                ExecutedStep existing = JSON.parseObject(raw, ExecutedStep.class);
                if (existing != null
                        && Objects.equals(existing.getIntentCode(), intentCode)
                        && Objects.equals(existing.getActionId(), actionId)) {
                    return true;
                }
            } catch (Exception e) {
                // 解析异常忽略，让上层继续 append；解析问题由 getSteps 时统一日志
            }
        }
        return false;
    }

    private static String key(String callId) {
        return KEY_PREFIX + callId + KEY_SUFFIX;
    }
}
