# M03 对话历史管理 — 设计规范

**日期：** 2026-05-10  
**模块：** M03 DialogHistoryManager  
**参考：** DD-V1.2 第10章  
**状态：** 已批准

---

## 1. 目标

实现对话历史的 Redis 存取，为 M06（意图识别）提供完整的 customer+agent 消息列表。本模块职责边界：仅存取，不过滤，不分析。

---

## 2. 架构与组件

### 包结构

```
com.cmbchina.cs.assitsvc.asr
├── DialogHistoryManager          (接口)
├── DialogHistoryManagerImpl      (实现，@Service)
└── config/
    └── HistoryProperties         (配置绑定，@ConfigurationProperties)
```

### 接口方法

| 方法签名 | 说明 |
|---|---|
| `void append(String callId, AsrSentenceEvent event)` | 追加一条消息，超出 max-size 时裁剪最旧的 |
| `List<DialogMessage> getHistory(String callId)` | 返回全部历史（含 customer + agent） |
| `void cleanup(String callId)` | 通话结束后删除 Redis key |

### 配置绑定

```yaml
# application.yml
copilot:
  history:
    max-size: 50      # DD-V1.2 §10.2 MAX_HISTORY
    ttl-hours: 1      # DD-V1.2 §10.2 TTL
```

`HistoryProperties` 用 `@ConfigurationProperties(prefix = "copilot.history")` 绑定，通过构造注入 `DialogHistoryManagerImpl`。

**Redis key 格式：** `copilot:history:{callId}`（与 DD-V1.2 §10.2 一致）

---

## 3. 数据流

### append

```
AsrSentenceEvent
    │
    ▼
toDialogMessage()
    │  speakerRole 映射：
    │    "customer" → role="user"
    │    "agent"   → role="assistant"
    │    speakerRole 字段原样保留（供 M06 过滤用）
    ▼
FastJSON 2.x 序列化 → JSON 字符串
    │
    ▼
Jedis Pipeline:
    RPUSH copilot:history:{callId} <json>
    LTRIM copilot:history:{callId} -(max-size) -1   ← 超限时裁剪最旧
    EXPIRE copilot:history:{callId} ttl-hours×3600  ← 每次刷新 TTL
```

> LTRIM 使用负索引（`-(max-size) -1`）保留尾部最新的 max-size 条，等价于先 RPUSH 再裁头部。三条命令用 Jedis Pipeline 批发，减少网络往返。

### getHistory

```
LRANGE copilot:history:{callId} 0 -1
    │
    ▼
List<String>（JSON 串）
    │
    ▼
FastJSON 反序列化 → List<DialogMessage>（逐条，损坏条目跳过）
    │
    ▼
原序返回（不过滤，不排序）
```

### cleanup

```
DEL copilot:history:{callId}
```

---

## 4. 错误处理

| 场景 | 处理方式 |
|---|---|
| Redis 连接失败（`JedisException`） | catch，记录 WARN 日志（含 callId），`getHistory` 返回空列表，不向上抛 |
| JSON 序列化失败 | catch `JSONException`，记录 ERROR 日志，跳过本条 append |
| JSON 反序列化失败（单条损坏） | 逐条 try-catch，损坏条目跳过，其余正常返回 |
| `callId` 为 null / 空串 | 方法入口 fast-fail，抛 `IllegalArgumentException` |
| `event` 为 null | 方法入口 fast-fail，抛 `IllegalArgumentException` |

**不做的事：**
- 不重试（重试由连接池/上层决定）
- 不降级到内存缓存（破坏多实例一致性）

---

## 5. 完整类结构

### HistoryProperties.java

```java
@ConfigurationProperties(prefix = "copilot.history")
@Data
public class HistoryProperties {
    private int maxSize = 50;
    private int ttlHours = 1;
}
```

### DialogHistoryManager.java

```java
public interface DialogHistoryManager {
    void append(String callId, AsrSentenceEvent event);
    List<DialogMessage> getHistory(String callId);
    void cleanup(String callId);
}
```

### DialogHistoryManagerImpl.java（骨架）

```java
@Service
@RequiredArgsConstructor
public class DialogHistoryManagerImpl implements DialogHistoryManager {

    private final JedisPool jedisPool;
    private final HistoryProperties props;

    @Override
    public void append(String callId, AsrSentenceEvent event) { ... }

    @Override
    public List<DialogMessage> getHistory(String callId) { ... }

    @Override
    public void cleanup(String callId) { ... }

    private static DialogMessage toDialogMessage(AsrSentenceEvent event) { ... }
}
```

---

## 6. 新增/修改文件清单

| 文件 | 动作 |
|---|---|
| `asr/DialogHistoryManager.java` | 新建 |
| `asr/DialogHistoryManagerImpl.java` | 新建 |
| `asr/config/HistoryProperties.java` | 新建 |
| `resources/application.yml` | 追加 `copilot.history` 块 |
| 启动类或某 `@Configuration` | 添加 `@EnableConfigurationProperties(HistoryProperties.class)` |

**不涉及：** 其余 domain POJO、M06 过滤逻辑、Redis 连接池配置（假设连接池已在项目中配置）。

---

## 7. 技术约束

- Spring Boot 2.7.18 + JDK 8，无 records/sealed/switch expressions
- 序列化：FastJSON 2.x（`com.alibaba.fastjson2`），不使用 Jackson
- Redis 客户端：Jedis（不使用 Lettuce）
- 无 `jakarta.*`，仅 `javax.*`
- 无单元测试（项目约定）
