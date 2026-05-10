# M03 对话历史管理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 `DialogHistoryManager`，将 ASR 事件追加到 Redis List，供 M06 意图识别读取完整对话历史。

**Architecture:** 接口 `DialogHistoryManager` + 实现 `DialogHistoryManagerImpl`（`@Service`），配置属性通过 `HistoryProperties` 外置；Redis 操作用 Jedis Pipeline 批发（RPUSH + LTRIM + EXPIRE），`JedisPool` bean 在新增的 `RedisConfig` 中显式声明。

**Tech Stack:** Spring Boot 2.7.18 / JDK 8 / Jedis 3.8.0 / FastJSON 2.x / `@ConfigurationProperties`

---

## 文件清单

| 路径 | 动作 |
|---|---|
| `infra/redis/RedisConfig.java` | 新建 — 声明 `JedisPool` bean |
| `asr/config/HistoryProperties.java` | 新建 — 绑定 `copilot.history.*` |
| `asr/DialogHistoryManager.java` | 新建 — 接口 |
| `asr/DialogHistoryManagerImpl.java` | 新建 — `@Service` 实现 |
| `resources/application.yml` | 修改 — 追加 `copilot.history` 块 |
| `CopilotApplication.java` | 修改 — 追加 `@EnableConfigurationProperties` |

---

## Task 1：Redis 基础配置 + HistoryProperties

**Files:**
- Create: `src/main/java/com/cmbchina/cs/assitsvc/infra/redis/RedisConfig.java`
- Create: `src/main/java/com/cmbchina/cs/assitsvc/asr/config/HistoryProperties.java`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/java/com/cmbchina/cs/assitsvc/CopilotApplication.java`

- [ ] **Step 1：创建 `RedisConfig.java`**

```java
package com.cmbchina.cs.assitsvc.infra.redis;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Jedis 连接池配置。
 * 显式声明 JedisPool bean，供需要 Pipeline 的组件直接注入。
 */
@Configuration
public class RedisConfig {

    @Bean
    public JedisPool jedisPool(RedisProperties redisProperties) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        RedisProperties.Pool pool = redisProperties.getJedis().getPool();
        if (pool != null) {
            poolConfig.setMaxTotal(pool.getMaxActive());
            poolConfig.setMaxIdle(pool.getMaxIdle());
            poolConfig.setMinIdle(pool.getMinIdle());
            if (pool.getMaxWait() != null) {
                poolConfig.setMaxWaitMillis(pool.getMaxWait().toMillis());
            }
        }
        String password = StringUtils.hasText(redisProperties.getPassword())
                ? redisProperties.getPassword() : null;
        return new JedisPool(
                poolConfig,
                redisProperties.getHost(),
                redisProperties.getPort(),
                (int) redisProperties.getTimeout().toMillis(),
                password,
                redisProperties.getDatabase()
        );
    }
}
```

- [ ] **Step 2：创建 `HistoryProperties.java`**

```java
package com.cmbchina.cs.assitsvc.asr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 对话历史 Redis 存储参数，绑定 copilot.history.*。
 */
@Data
@ConfigurationProperties(prefix = "copilot.history")
public class HistoryProperties {

    /** Redis List 最大条数，超出时裁剪最旧的；对应 DD-V1.2 §10.2 MAX_HISTORY */
    private int maxSize = 50;

    /** Key 有效期（小时），每次 append 刷新；对应 DD-V1.2 §10.2 TTL */
    private int ttlHours = 1;
}
```

- [ ] **Step 3：在 `application.yml` 追加 `copilot.history` 块**

在文件末尾 `copilot:` 下添加（与其他 copilot 配置平级）：

```yaml
  # ============= M03 对话历史管理 =============
  history:
    max-size: 50
    ttl-hours: 1
```

完整位置示意（插入到 `config-refresh` 块之后）：

```yaml
  config-refresh:
    polling-interval-ms: 30000

  # ============= M03 对话历史管理 =============
  history:
    max-size: 50
    ttl-hours: 1
```

- [ ] **Step 4：在 `CopilotApplication.java` 追加 `@EnableConfigurationProperties`**

在现有注解列表中加入：

```java
import com.cmbchina.cs.assitsvc.asr.config.HistoryProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.cmbchina.cs.assitsvc.infra.feign")
@EnableKafka
@EnableScheduling
@EnableConfigurationProperties(HistoryProperties.class)
public class CopilotApplication {
    public static void main(String[] args) {
        SpringApplication.run(CopilotApplication.class, args);
    }
}
```

- [ ] **Step 5：提交**

```bash
git add src/main/java/com/cmbchina/cs/assitsvc/infra/redis/RedisConfig.java
git add src/main/java/com/cmbchina/cs/assitsvc/asr/config/HistoryProperties.java
git add src/main/resources/application.yml
git add src/main/java/com/cmbchina/cs/assitsvc/CopilotApplication.java
git commit -m "[M03] 添加 RedisConfig(JedisPool) 与 HistoryProperties 配置

- RedisConfig 显式声明 JedisPool bean，读取 spring.redis.* 参数
- HistoryProperties 绑定 copilot.history.max-size / ttl-hours
- application.yml 追加 history 配置块（默认 50 条 / 1 小时）
- CopilotApplication 注册 @EnableConfigurationProperties

Refs: DD-V1.2 第10章"
```

---

## Task 2：DialogHistoryManager 接口

**Files:**
- Create: `src/main/java/com/cmbchina/cs/assitsvc/asr/DialogHistoryManager.java`

- [ ] **Step 1：创建接口**

```java
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
     * Redis 不可达时返回空列表，不抛异常。
     *
     * @param callId 通话 ID，不可为 null/空
     * @return 对话消息列表（可能为空，不为 null）
     */
    List<DialogMessage> getHistory(String callId);

    /**
     * 删除通话历史（通话结束时调用）。
     * Redis 不可达时仅记录 WARN，不抛异常。
     *
     * @param callId 通话 ID，不可为 null/空
     */
    void cleanup(String callId);
}
```

- [ ] **Step 2：提交**

```bash
git add src/main/java/com/cmbchina/cs/assitsvc/asr/DialogHistoryManager.java
git commit -m "[M03] 添加 DialogHistoryManager 接口

- 定义 append / getHistory / cleanup 三个方法
- Javadoc 明确错误处理契约（Redis 不可达返回空列表，不抛异常）

Refs: DD-V1.2 第10章"
```

---

## Task 3：DialogHistoryManagerImpl 实现

**Files:**
- Create: `src/main/java/com/cmbchina/cs/assitsvc/asr/DialogHistoryManagerImpl.java`

- [ ] **Step 1：创建实现类**

```java
package com.cmbchina.cs.assitsvc.asr;

import com.alibaba.fastjson2.JSON;
import com.cmbchina.cs.assitsvc.asr.config.HistoryProperties;
import com.cmbchina.cs.assitsvc.domain.AsrSentenceEvent;
import com.cmbchina.cs.assitsvc.domain.DialogMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link DialogHistoryManager} 的 Redis 实现。
 *
 * <p>Redis key 格式：{@code copilot:history:{callId}}，类型 List。
 * 每次 append 用 Pipeline 批发 RPUSH + LTRIM + EXPIRE，减少网络往返。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DialogHistoryManagerImpl implements DialogHistoryManager {

    private static final String KEY_PREFIX = "copilot:history:";

    private final JedisPool jedisPool;
    private final HistoryProperties props;

    @Override
    public void append(String callId, AsrSentenceEvent event) {
        if (callId == null || callId.isEmpty()) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }

        String json;
        try {
            json = JSON.toJSONString(toDialogMessage(event));
        } catch (Exception e) {
            log.error("[M03] JSON serialize failed, callId={}, sentenceId={}", callId, event.getSentenceId(), e);
            return;
        }

        String key = KEY_PREFIX + callId;
        try (Jedis jedis = jedisPool.getResource()) {
            Pipeline pipeline = jedis.pipelined();
            pipeline.rpush(key, json);
            // 保留尾部最新的 maxSize 条，超出时裁剪头部旧数据
            pipeline.ltrim(key, -props.getMaxSize(), -1);
            pipeline.expire(key, (long) props.getTtlHours() * 3600);
            pipeline.sync();
        } catch (JedisException e) {
            log.warn("[M03] Redis append failed, callId={}", callId, e);
        }
    }

    @Override
    public List<DialogMessage> getHistory(String callId) {
        if (callId == null || callId.isEmpty()) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }

        String key = KEY_PREFIX + callId;
        List<String> jsonList;
        try (Jedis jedis = jedisPool.getResource()) {
            jsonList = jedis.lrange(key, 0, -1);
        } catch (JedisException e) {
            log.warn("[M03] Redis getHistory failed, callId={}", callId, e);
            return Collections.emptyList();
        }

        List<DialogMessage> result = new ArrayList<>(jsonList.size());
        for (String json : jsonList) {
            try {
                result.add(JSON.parseObject(json, DialogMessage.class));
            } catch (Exception e) {
                log.error("[M03] JSON deserialize failed, callId={}, json={}", callId, json, e);
            }
        }
        return result;
    }

    @Override
    public void cleanup(String callId) {
        if (callId == null || callId.isEmpty()) {
            throw new IllegalArgumentException("callId must not be null or empty");
        }

        String key = KEY_PREFIX + callId;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        } catch (JedisException e) {
            log.warn("[M03] Redis cleanup failed, callId={}", callId, e);
        }
    }

    /**
     * 将 ASR 事件转换为对话消息。
     * speakerRole 映射：CUSTOMER → user，其余（AGENT）→ assistant。
     * speakerRole 字段原样保留，供 M06 按需过滤。
     */
    private static DialogMessage toDialogMessage(AsrSentenceEvent event) {
        String role = "CUSTOMER".equalsIgnoreCase(event.getSpeakerRole()) ? "user" : "assistant";
        return DialogMessage.builder()
                .id(event.getSentenceId())
                .role(role)
                .content(event.getContent())
                .contentType("text")
                .createTime(event.getBeginTime())
                .speakerRole(event.getSpeakerRole())
                .build();
    }
}
```

- [ ] **Step 2：提交**

```bash
git add src/main/java/com/cmbchina/cs/assitsvc/asr/DialogHistoryManagerImpl.java
git commit -m "[M03] 实现 DialogHistoryManagerImpl

- append: Jedis Pipeline 批发 RPUSH + LTRIM + EXPIRE
- getHistory: LRANGE 全量读取，逐条反序列化，损坏条目跳过
- cleanup: DEL 指定 key
- toDialogMessage: CUSTOMER→user / AGENT→assistant，保留 speakerRole

Refs: DD-V1.2 第10章"
```

---

## 自检清单

实现完成后逐项确认：

- [ ] `RedisConfig.java` 中 `JedisPool` bean 能正常注入（启动不报 `NoSuchBeanDefinitionException`）
- [ ] `HistoryProperties` 绑定生效（`maxSize=50, ttlHours=1` 或 yml 中覆盖值）
- [ ] `append` 调用后 `redis-cli LRANGE copilot:history:{callId} 0 -1` 能看到 JSON 条目
- [ ] `append` 50 次后再 append 1 次，List 长度仍为 50（LTRIM 生效）
- [ ] `getHistory` 返回顺序与 append 顺序一致
- [ ] `cleanup` 后 `redis-cli EXISTS copilot:history:{callId}` 返回 0
- [ ] Redis 断开时 `append` / `getHistory` 不抛异常（`getHistory` 返回空列表）
- [ ] `callId=null` 时抛 `IllegalArgumentException`
