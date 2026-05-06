# 编码规范

> 本规范作为 CLAUDE.md 的补充，包含完整的编码细则。Claude 不需要每次都读，仅在不确定规范时翻阅。

## 1. 基础约束

| 项 | 规范 |
|---|------|
| 缩进 | 4 空格（不是 Tab，不是 2 空格） |
| 行宽 | 120 字符 |
| 字符编码 | UTF-8 |
| 换行符 | LF（Unix 风格） |
| 文件结尾 | 必须以换行符结尾 |
| 文件头注释 | 不需要许可证声明 |

## 2. 命名规范

### 2.1 包命名

```
com.cmbchina.cs.assitsvc.{layer}[.{sublayer}]

layer 必须是以下之一：
- api          # 接口层
- core         # 核心业务
- asr          # ASR 接入
- session      # 通话会话
- push         # 推送
- config       # 配置缓存
- infra        # 基础设施
- domain       # 领域模型
- extension    # 扩展点
```

### 2.2 类命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 接口 | `XxxService` | `IntentRecognitionService` |
| 实现类 | `XxxServiceImpl` | `IntentRecognitionServiceImpl` |
| 抽象类 | `AbstractXxx` | `AbstractRuleEvaluator` |
| 枚举 | `XxxType` / `XxxStatus` | `StandardParamType` / `ResultStatus` |
| DTO | `XxxDTO` / `XxxRequest` / `XxxResponse` | `DirectiveDTO` |
| 异常 | `XxxException` | `UrlValidationException` |
| 配置类 | `XxxConfig` | `AiFeignConfig` |
| 常量类 | `XxxConstants` | `ReasonCodeConstants` |
| 工具类 | `XxxUtils` | `JsonUtils` |
| Controller | `XxxController` | `FeedbackController` |
| Feign Client | `XxxFeignClient` | `AiIntentFeignClient` |

### 2.3 方法命名

```java
// ✅ 推荐：动词开头
public DirectiveDTO buildDirective(...) {}
public boolean isExpired(...) {}
public void cleanupCallSession(String callId) {}

// ❌ 不推荐
public DirectiveDTO directive(...) {}     // 不是动词
public boolean expired(...) {}            // 缺谓词
```

### 2.4 变量命名

```java
// ✅ 推荐
private String operatorId;
private List<DialogMessage> recentMessages;
private static final int MAX_HISTORY = 50;

// ❌ 不推荐
private String oper;            // 缩写不清
private String opId;            // 误导（不是 OperationId）
private List<DialogMessage> l;  // 单字母
```

## 3. Lombok 使用规范

### 3.1 推荐使用

```java
// DTO/POJO 类
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DialogMessage {
    private String id;
    private String content;
}

// 服务类
@Service
@Slf4j
@RequiredArgsConstructor
public class IntentRecognitionServiceImpl implements IntentRecognitionService {

    private final DialogHistoryManager historyManager;
    private final AiIntentFeignClient feignClient;

    public IntentResult recognize(String callId) {
        log.info("Start recognize: callId={}", callId);
        // ...
    }
}
```

### 3.2 避免使用

- 不要用 `@Slf4j` 之外的日志注解（统一 SLF4J）
- 不要用 `@SneakyThrows`（异常应明确处理或声明）
- 不要用 `@NonNull`（用 javax.validation 注解或显式判空）

## 4. 异常处理

### 4.1 异常分类

```java
// 业务异常：继承 RuntimeException
public class UrlValidationException extends RuntimeException {
    private final String reasonCode;

    public UrlValidationException(String reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }
}

// 不要随意 throw Exception
public void method() throws Exception {  // ❌
    // ...
}

// 应该具体声明
public void method() throws UrlValidationException, IOException {  // ✅
    // ...
}
```

### 4.2 异常捕获原则

```java
// ❌ 错误：吞异常
try {
    feignClient.recognize(req);
} catch (Exception e) {
    // 什么都不做
}

// ❌ 错误：捕获后只打日志，不处理
try {
    feignClient.recognize(req);
} catch (Exception e) {
    log.error("error", e);
}

// ✅ 正确：捕获后明确处理
try {
    return feignClient.recognize(req);
} catch (FeignException e) {
    log.warn("AI failed: callId={}, status={}", callId, e.status(), e);
    metricsService.recordAiFailure(classifyReason(e));
    return null;  // 由调用方决定如何降级
}
```

### 4.3 防御性编程

```java
// ✅ 入口处校验参数
public DirectiveDTO build(BuildContext bc) {
    if (bc == null || bc.getCallId() == null) {
        throw new IllegalArgumentException("BuildContext or callId cannot be null");
    }
    // ...
}

// ✅ 评估器异常默认返回安全值（DD-V1.2 P1-19）
public boolean evaluate(ConditionRule rule, EvaluationContext ctx) {
    if (rule == null) return true;
    try {
        return doEvaluate(rule, ctx);
    } catch (Exception e) {
        log.error("Rule evaluation failed: {}", JSON.toJSONString(rule), e);
        return false;  // 异常时不展示推荐
    }
}
```

## 5. 日志规范

### 5.1 日志级别

| 级别 | 使用场景 |
|------|---------|
| ERROR | 系统异常（影响功能） |
| WARN | 外部接口失败、配置异常等可恢复问题 |
| INFO | 关键流程节点（意图识别成功、推荐推送、反馈采纳） |
| DEBUG | 详细参数、中间结果（生产默认关闭） |

### 5.2 日志格式

```java
// ✅ 推荐：结构化字段，便于 Splunk/ELK 查询
log.info("Intent recognized: callId={}, intentCode={}, durationMs={}",
    callId, intentCode, durationMs);

// ✅ 推荐：异常带堆栈
log.warn("AI failed: callId={}", callId, e);

// ❌ 不推荐：字符串拼接
log.info("Intent recognized for " + callId + " is " + intentCode);

// ❌ 不推荐：无上下文
log.info("Intent recognized");
```

### 5.3 MDC 上下文

关键日志应含 `traceId` / `callId` / `directiveId`：

```java
// 在请求入口或 Kafka 消费入口设置 MDC
MDC.put("callId", callId);
try {
    // 业务逻辑
} finally {
    MDC.clear();
}
```

`logback-spring.xml` 已配置自动输出 `[traceId,callId]`。

### 5.4 不要打印敏感信息

```java
// ❌ 错误
log.info("ASR full content: {}", content);

// ✅ 正确：只打印长度或哈希
log.debug("ASR content length: {}, hash: {}",
    content.length(), DigestUtils.md5Hex(content));
```

## 6. 注解使用

### 6.1 Spring 注解

```java
// ✅ 推荐：构造器注入（搭配 Lombok @RequiredArgsConstructor）
@Service
@RequiredArgsConstructor
public class XxxServiceImpl {
    private final YyyService yyyService;  // final + 构造注入
}

// ❌ 不推荐：字段注入
@Service
public class XxxServiceImpl {
    @Autowired
    private YyyService yyyService;  // 不推荐
}
```

### 6.2 Bean 注解选择

| 场景 | 注解 |
|------|------|
| Service 层 | `@Service` |
| Repository 层 | `@Repository` |
| Controller | `@RestController`（API）/ `@Controller`（页面） |
| 配置类 | `@Configuration` |
| 普通 Bean | `@Component` |
| Feign Client | `@FeignClient` |

### 6.3 javax 命名空间（不是 jakarta）

```java
// ✅ Spring Boot 2.7 + JDK 8
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;

// ❌ Spring Boot 3.x（本项目不要用）
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
```

## 7. JSON 处理（FastJSON 2.x）

### 7.1 包名

```java
// ✅ 正确：FastJSON 2.x
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.annotation.JSONField;

// ❌ 错误：FastJSON 1.x
import com.alibaba.fastjson.JSON;
```

### 7.2 常用操作

```java
// 序列化
String json = JSON.toJSONString(dialogMessage);

// 反序列化
DialogMessage msg = JSON.parseObject(json, DialogMessage.class);

// 反序列化集合
List<DialogMessage> list = JSON.parseArray(json, DialogMessage.class);

// 字段名映射（如 intent_code <-> intentCode）
@JSONField(name = "intent_code")
private String intentCode;
```

## 8. Redis 操作（Jedis）

### 8.1 用 RedisTemplate（不直接用 Jedis）

```java
@Component
@RequiredArgsConstructor
public class DialogHistoryManagerImpl implements DialogHistoryManager {

    private final RedisTemplate<String, String> redisTemplate;

    public void append(AsrSentenceEvent event) {
        String key = "copilot:history:" + event.getCallId();
        redisTemplate.opsForList().rightPush(key, JSON.toJSONString(toMessage(event)));
        redisTemplate.opsForList().trim(key, -50, -1);
        redisTemplate.expire(key, Duration.ofHours(1));
    }
}
```

### 8.2 不要直接 inject `JedisPool`

底层用 Jedis（pom.xml 已声明），但代码层面统一用 `RedisTemplate`，便于后续切换/Mock。

## 9. 不要做的事

- ❌ 不写单元测试（用户决定）
- ❌ 不用 JDK 9+ 语法
- ❌ 不引入未在 pom.xml 声明的依赖
- ❌ 不用 `@SuppressWarnings("all")` 大范围抑制告警
- ❌ 不主动实现"本期不做"清单中的功能
- ❌ 不写 `System.out.println` 调试代码
- ❌ 不在生产代码中保留 TODO / FIXME 注释（应该开 issue）

## 10. SonarQube 主要规则

按团队 SonarQube 规范，常见需避免：

- 圈复杂度 ≤ 15
- 方法长度 ≤ 100 行
- 类长度 ≤ 500 行
- 参数个数 ≤ 7
- 嵌套深度 ≤ 4
- 不要捕获 `Exception` 后什么都不做
- 不要返回 `null` 表示集合（返回 `Collections.emptyList()`）
- 不要在循环中创建大量临时对象

## 11. Git 提交信息规范

```
[M{XX}] {简短描述}

- 详细说明 1
- 详细说明 2

Refs: DD-V1.2 第 {章节}
```

例：

```
[M02] 实现句间合并器

- 添加 SentenceMerger 类（防抖 + 沉默 timer）
- 添加 SentenceContinuityDetector 类（句子连续性判定）
- 配置 ScheduledExecutorService

Refs: DD-V1.2 第 9 章
```
