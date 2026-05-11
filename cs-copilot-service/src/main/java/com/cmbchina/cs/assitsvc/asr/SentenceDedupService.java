package com.cmbchina.cs.assitsvc.asr;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

/**
 * ASR sentenceId 去重服务。
 *
 * <p>Redis 不可达时 fail open：不把消息判为重复，避免误丢 ASR 内容。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SentenceDedupService {

    private static final String KEY_PREFIX = "copilot:asr_dedup:";

    private final JedisPool jedisPool;
    private final AsrProperties props;

    /**
     * 判断 sentenceId 是否已经处理过。
     *
     * @param sentenceId ASR 句子 ID
     * @return true 表示重复，应跳过；false 表示首次出现或去重服务不可用
     */
    public boolean isDuplicate(String sentenceId) {
        if (!StringUtils.hasText(sentenceId)) {
            throw new IllegalArgumentException("sentenceId must not be null or empty");
        }

        String key = KEY_PREFIX + sentenceId;
        try (Jedis jedis = jedisPool.getResource()) {
            Long inserted = jedis.setnx(key, "1");
            if (inserted != null && inserted == 1L) {
                jedis.expire(key, dedupTtlSeconds());
                return false;
            }
            return true;
        } catch (JedisException e) {
            log.warn("[M01] Redis sentence dedup failed, sentenceId={}", sentenceId, e);
            return false;
        }
    }

    private int dedupTtlSeconds() {
        return Math.max(1, props.getDedupTtlHours()) * 3600;
    }
}
