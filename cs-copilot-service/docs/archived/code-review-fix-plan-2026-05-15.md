# Code Review 修复方案

> **文档版本**：v1.0 / 2026-05-15
> **来源 review**：基于本仓库 main 分支 commit `537904c` 的全量审查（CODE_REVIEW_REPORT.md，33 个问题）
> **执行人**：Claude Code（superpowers 工作流）
> **执行模式**：4 批次，全自动 subagent-driven-development，不需要中间确认

---

## 概述

本文档是 cs-copilot-service code review 后的**最终修复任务清单**。已经过业务方（Xiaowei）的全量决策评审，每个问题都有明确的"修复"或"不修复"决定，**Claude Code 按本文档执行即可，无需重新 brainstorm 决策**。

### 决策原则速览

- ✅ **修复**：本文档列出具体修改方案，按批次执行
- ❌ **不修复**：在"不修复清单"中列明原因和后续路径
- ⚠️ **修文档不修代码**：DD-4 / CR-1 / CR-4 等属此类，只调整 CLAUDE.md 或 dd-v1.2.md

### 执行范围统计

| 状态 | 数量 | 备注 |
|------|------|------|
| 修复（含文档调整） | 17 | CR-3 / CR-5 / CR-6 / CR-7 / CR-8 / CR-9 / CR-11 / CR-14 / CR-15 / CR-19 / CR-20 / CR-21 / DD-4 / DD-5 / CR-1（文档）/ CR-4（文档）/ CR-7（文档）|
| 不修复 | 16 | 见末尾"不修复清单" |

---

## 接受/拒绝总览表

| 编号 | 类型 | 决策 | 修复要点 |
|------|------|------|---------|
| DD-1 | 设计文档 | ❌ 不修复 | 后续可能还会改伪代码，暂保留 |
| DD-2 | 设计文档 | ❌ 不修复 | 同上 |
| DD-3 | 设计文档 | ❌ 不修复 | 同上 |
| DD-4 | 设计文档 | ✅ 修复 | 关闭 Q1-Q5 五个待澄清问题 |
| DD-5 | 设计文档 | ✅ 修复 | 补齐错误码清单 |
| DD-6 | 设计文档 | ❌ 不修复 | 上游 ASR 契约问题，本期接受 |
| DD-7 | 设计文档 | ❌ 不修复 | 现有 PUBLISHED 行最后插入已规避中间态可见 |
| DD-8 | 设计文档 | ❌ 不修复 | MENU_ITEM 模式不进入参数解析，实际不触发 |
| DD-9 | 设计文档 | ❌ 不修复 | 鉴权后续用生产逻辑 |
| DD-10 | 设计文档 | ❌ 不修复 | 不考虑字段脱敏与加密 |
| DD-11 | 设计文档 | ❌ 不修复 | 接受 30s 配置 lag 窗口 |
| DD-12 | 设计文档 | ❌ 不修复 | 反馈枚举语义不细分 |
| CR-1 | 代码约束 | ✅ 修复 | **只改 CLAUDE.md**：撤销 Jedis 硬约束，明示业务代码用 StringRedisTemplate 抽象层即可 |
| CR-2 | 代码安全 | ❌ 不修复 | WebSocket 用工作台已有封装组件 |
| CR-3 | 代码 | ✅ 修复 | 所有 `cleanup` 改为 no-op + 日志，完全依赖 TTL |
| CR-4 | 代码 | ✅ 修复（文档） | 方案 B：保留单 slot 实现，在 dd-v1.2.md §9.6/§32 明示风险与扩容触发点 |
| CR-5 | 代码 | ✅ 修复 | 短期方案（仅后端）：方法重命名 + 状态语义对齐 + 预留 `markDirectiveDelivered` |
| CR-6 | 代码 | ✅ 修复 | 区分 AI 失败类型，新增 reason_code 常量 |
| CR-7 | 文档 | ✅ 修复 | CLAUDE.md 增加 MyBatis-Plus 行 |
| CR-8 | 配置 | ✅ 修复 | application.yml 主配置剥离 UAT 域名，挪到 application-uat.yml |
| CR-9 | 代码 | ✅ 修复 | FeedbackService 加 @Transactional + 捕获 DuplicateKeyException |
| CR-10 | 配置 | ❌ 不修复 | AI URL 保持 http |
| CR-11 | 代码 | ✅ 修复 | SentenceDedupService 去掉冗余 hash tag |
| CR-12 | 代码 | ❌ 不修复 | ConfigVersionPoller 不加 leader election |
| CR-13 | 代码 | ❌ 不修复 | Admin token 比较后续用生产逻辑 |
| CR-14 | 代码 | ✅ 修复 | AI 调用计数挪到成功后 INCR |
| CR-15 | 代码 | ✅ 修复 | MybatisCopilotConfigRepository 收集所有错误后一次性抛 |
| CR-16 | 代码 | ❌ 不修复 | 不做脱敏 |
| CR-17 | 代码 | ❌ 不修复 | 词典硬编码可接受 |
| CR-18 | 代码 | ❌ 不修复 | 内部信任配置 |
| CR-19 | 代码 | ✅ 修复 | ParamResolver lookup 加 last-segment fallback |
| CR-20 | 代码 | ✅ 修复 | IntentTreeLoader 改用 @ConfigurationProperties |
| CR-21 | 配置 | ✅ 修复 | 选 A：移除未使用的 wiremock / spring-kafka-test 依赖 |

---

## 修复任务（按批次）

### 批次划分原则

- **批次 1**：纯配置 / 文档调整，零业务风险
- **批次 2**：单文件代码改动，影响范围明确
- **批次 3**：跨模块改动，需仔细处理依赖
- **批次 4**：文档补充（dd-v1.2.md 风险说明）

每个批次结束后 Claude Code **不需要等待业务方确认**，直接进入下一批次。

---

## 批次 1：配置与文档调整

### Task 1.1 — CR-1 撤销 Jedis 硬约束

**类型**：CLAUDE.md 文档调整

**涉及文件**：

- `CLAUDE.md`（第 82-92 行附近）

**改动方案**：

把"技术栈选型"表中 Redis 客户端那一行从：

```markdown
| Redis 客户端 | **Jedis** + JedisPool | Lettuce |
```

改为：

```markdown
| Redis 抽象层 | **StringRedisTemplate** | RedisTemplate<Object,Object>（避免序列化坑） |
| Redis 连接驱动 | 由 spring-boot-starter-data-redis 自动选择（默认 Lettuce） | 直接使用 Jedis / Lettuce 原生 API |
```

并在表后增加一段说明：

```markdown
### Redis 使用约定

- 业务代码统一通过 `StringRedisTemplate` 操作 Redis，不直接接触底层连接驱动
- 连接驱动默认走 Spring Boot 自动配置（当前为 Lettuce），无需在 `RedisConfig` 中自定义 `JedisConnectionFactory`
- 不允许在代码中显式调用阻塞命令（如 `redisTemplate.delete(...)`，见 §"Redis 清理策略"）
- 通话级临时数据全部依赖 TTL 自动过期清理
```

**验证标准**：

- `CLAUDE.md` 中不再有"Jedis（不是 Lettuce）"措辞
- 增加了 Redis 使用约定章节
- `git diff` 只动了 CLAUDE.md，未改任何代码

**风险与回滚**：无业务风险，单文档修改

---

### Task 1.2 — CR-7 CLAUDE.md 增加 MyBatis-Plus 声明

**类型**：CLAUDE.md 文档调整

**涉及文件**：

- `CLAUDE.md`（技术栈选型表附近）

**改动方案**：

在技术栈选型表中增加一行：

```markdown
| ORM 框架 | **MyBatis-Plus** 3.5.7（com.baomidou） | 原生 MyBatis、JPA、Hibernate |
```

并在表后增加 MyBatis-Plus 使用约定：

```markdown
### MyBatis-Plus 使用约定

- 使用 mybatis-plus-boot-starter，版本固定 3.5.7
- 配置文件中如有自动行为（逻辑删除、乐观锁、自动填充等），必须在 `application.yml` 显式声明，禁止依赖默认值
- 当前项目未启用 `@TableLogic` / `@Version` / `MetaObjectHandler`，新增模块如需启用必须先更新本文档
- ID 生成策略：业务主键由代码生成（UUID 去横线 / 业务自定义），不依赖 MyBatis-Plus 雪花 ID
- Mapper 接口使用 `@Mapper` 注解 + `@Select` 内联 SQL，**不写 mapper xml**（与项目体量匹配）
```

**验证标准**：

- CLAUDE.md 技术栈表新增 MyBatis-Plus 一行
- 新增 MyBatis-Plus 使用约定段落
- pom.xml 不动

**风险与回滚**：无

---

### Task 1.3 — CR-8 剥离 application.yml 主配置中的 UAT 域名

**类型**：配置文件调整

**涉及文件**：

- `src/main/resources/application.yml`
- `src/main/resources/application-uat.yml`

**改动方案**：

**步骤 1**：`application.yml` 第 110-116 行的 `copilot.url-whitelist` 删除 UAT 域名：

```yaml
# 修改前
copilot:
  url-whitelist:
    - frdctrfront.paas.cmbchina.cn
    - mccusweb.paas.cmbchina.cn
    - cccsweb.paas.cmbchina.cn
    - frdctrfront.paasuat.cmbchina.cn
    - mccusweb.paasuat.cmbchina.cn

# 修改后
copilot:
  url-whitelist:
    - frdctrfront.paas.cmbchina.cn
    - mccusweb.paas.cmbchina.cn
    - cccsweb.paas.cmbchina.cn
```

`copilot.url-builder.uat-domains` 配置保留不动（用于生产环境拦截判断）。

**步骤 2**：`application-uat.yml` 中追加 UAT 域名到 url-whitelist（如该文件原本没有该配置块则新增）：

```yaml
copilot:
  url-whitelist:
    - frdctrfront.paas.cmbchina.cn
    - mccusweb.paas.cmbchina.cn
    - cccsweb.paas.cmbchina.cn
    - frdctrfront.paasuat.cmbchina.cn
    - mccusweb.paasuat.cmbchina.cn
  url-builder:
    uat-domains: []   # UAT 环境不拦截 UAT 域名
```

**步骤 3**：`application-prod.yml` 已经只有 prod 域名（与现状一致），无需改动。

**验证标准**：

- 默认 profile 启动时（spring.profiles.active=dev），url-whitelist 只含 prod 域名（dev 环境通常不需要访问真实跳转目标）
- 启动时 `spring.profiles.active=uat`，url-whitelist 含 UAT 域名且不拦截
- 启动时 `spring.profiles.active=prod`，url-whitelist 只含 prod 域名（继承 application.yml 主配置，prod profile 自身也再次列出）

**风险与回滚**：低。UAT 环境若漏覆盖 url-whitelist 会导致 UAT 域名跳转被拒，属可控失败。

---

### Task 1.4 — CR-11 SentenceDedupService 去掉冗余 hash tag

**类型**：代码调整

**涉及文件**：

- `src/main/java/com/cmbchina/cs/assitsvc/asr/SentenceDedupService.java`

**改动方案**：

修改第 22-23 行：

```java
// 修改前
private static final String KEY_PREFIX = "copilot:asr_dedup:{";
private static final String KEY_SUFFIX = "}";

// 修改后
private static final String KEY_PREFIX = "copilot:asr_dedup:";
private static final String KEY_SUFFIX = "";
```

第 39 行的 key 拼接逻辑无需改动（变量名和拼接方式不变）。

**注意**：上线后 Redis 中残留的旧 key（含 `{...}`）会因 dedup TTL（2 小时）自动过期，无需手工清理。但**部署期间会有 2 小时窗口期**新旧 key 并存，理论上不影响功能（旧 sentenceId 已处理过的也不会重复消费，新 sentenceId 走新 key 格式）。

**验证标准**：

- key 格式从 `copilot:asr_dedup:{ASR_SEG_10086}` 变为 `copilot:asr_dedup:ASR_SEG_10086`
- 代码改动只此一处常量定义

**风险与回滚**：极低。回滚直接还原常量定义。

---

### Task 1.5 — CR-20 IntentTreeLoader 改用 @ConfigurationProperties

**类型**：代码调整

**涉及文件**：

- 新增 `src/main/java/com/cmbchina/cs/assitsvc/core/intent/IntentTreeProperties.java`
- 修改 `src/main/java/com/cmbchina/cs/assitsvc/core/intent/IntentTreeLoaderImpl.java`

**改动方案**：

**新增** `IntentTreeProperties.java`：

```java
package com.cmbchina.cs.assitsvc.core.intent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * 意图树加载配置，绑定 copilot.intent-tree.*
 */
@Data
@Component
@ConfigurationProperties(prefix = "copilot.intent-tree")
public class IntentTreeProperties {

    /** 意图树 JSON 文件位置，Spring Resource 表达式（如 classpath:intent-tree.json）。 */
    private Resource file;

    /** 意图树版本号，传给 AI 接口用。 */
    private String version;
}
```

**修改** `IntentTreeLoaderImpl.java`：

```java
// 修改前（第 27-31 行）
@Value("${copilot.intent-tree.file}")
private Resource intentTreeFile;

@Value("${copilot.intent-tree.version}")
private String version;

// 修改后：改为构造注入
private final IntentTreeProperties props;

public IntentTreeLoaderImpl(IntentTreeProperties props) {
    this.props = props;
}

// 后续代码用 props.getFile() / props.getVersion() 替换 intentTreeFile / version
```

类注解上加上 `@RequiredArgsConstructor` 也可（与其他模块风格保持一致）：

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentTreeLoaderImpl implements IntentTreeLoader {
    private final IntentTreeProperties props;
    private volatile IntentTreeSnapshot snapshot = new IntentTreeSnapshot(null, 0, null);

    @PostConstruct
    public void init() {
        reload();
    }

    @Override
    public String getVersion() {
        return props.getVersion();
    }

    // ... loadTree 内 intentTreeFile.getInputStream() → props.getFile().getInputStream()
    // ... log.info 中 version → props.getVersion()
}
```

**验证标准**：

- 启动正常（Resource 注入成功）
- `/copilot/admin/intent-tree/reload` 接口可正常调用
- 与项目其他模块（AsrProperties / DebounceProperties / HistoryProperties）风格一致

**风险与回滚**：低。本地启动验证一次即可。

---

### Task 1.6 — CR-21 移除未使用的测试依赖

**类型**：pom.xml 调整

**涉及文件**：

- `pom.xml`

**改动方案**：

删除 pom.xml 第 142-159 行的三个 test scope 依赖：

```xml
<!-- 删除以下三个依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.github.tomakehurst</groupId>
    <artifactId>wiremock-jre8</artifactId>
    <version>2.35.0</version>
    <scope>test</scope>
</dependency>
```

并把"<!-- ============= 集成测试..." 注释行也一起删除。

**验证标准**：

- `mvn dependency:tree` 不再含 wiremock / spring-kafka-test
- `src/test/` 目录保持为空
- 编译通过

**风险与回滚**：无。test scope 依赖不影响打包。

---

### Task 1.7 — DD-4 关闭 dd-v1.2.md §22.6 待澄清问题

**类型**：dd-v1.2.md 文档调整

**涉及文件**：

- `docs/dd-v1.2.md`（§22.6，第 3002-3013 行附近）

**改动方案**：

把 §22.6 标题从"已确认与待澄清问题"改为"已确认问题"，并把表格中 Q1-Q5 改为确定结论：

```markdown
### 22.6 已确认问题

| 编号 | 问题 | 决策 |
|------|------|------|
| C1 | 前端是否支持仅凭 `menuItemId` 调用现有快捷导航打开逻辑？ | 已确认支持 |
| C2 | `cs_menu_item.enabled` 的真实启用值是什么？ | 已确认启用值为 `Y` |
| C3 | `item_snapshot_json` 是否必填？ | **MVP 非必填**；非空时必须做快照一致性校验（已实现于 MybatisCopilotConfigRepository.validateSnapshot） |
| C4 | 菜单项变化后是否自动触发 Copilot 版本发布? | **手工触发**，不做自动联动；自动联动改造较大，列为 F16 后续待实现 |
| C5 | 运行时单 action 关联 item 失效时整版失败还是跳过？ | **整版失败**，与当前 `MybatisCopilotConfigRepository` 实现一致 |
| C6 | 纯 action 的 `target_kind/open_mode` 是否允许扩展 COMPONENT/POPUP/DRAWER？ | **本期严格限制四种**：URL / ROUTE / IFRAME / NEW_WINDOW；配置后台校验拒绝其他值（已实现于 CopilotConfigValidationServiceImpl.validateActionCombination） |
| C7 | 纯 action 没有 `menuItemId` 时业务权限如何判断？ | **复用 URL 白名单 + 灰度白名单**，前端不做 menuItemId 权限校验；记录为已知风险（§2.3） |
```

在 §4.2 "后续待实现模块"表格中追加：

```markdown
| F16 | 菜单变更自动触发 Copilot 配置发布 | P3 |
```

**验证标准**：

- §22.6 不再有"Q-编号"的待澄清问题
- §4.2 新增 F16
- 文档语义与代码实现一致

**风险与回滚**：纯文档调整，无业务风险。

---

### Task 1.8 — DD-5 补齐 dd-v1.2.md §29.2 错误码清单

**类型**：dd-v1.2.md 文档调整

**涉及文件**：

- `docs/dd-v1.2.md`（§29.2，第 3389-3400 行附近）

**改动方案**：

在 §29.2 "业务错误码"表格中追加以下错误码（保留原有 7 个错误码）：

```markdown
### 29.2 业务错误码

| code | 含义 |
|------|------|
| COP_INTENT_NOT_MAPPED | AI 返回的 intentCode 在映射表中找不到 |
| COP_RISK_DISABLED | 风险等级 DISABLED，不展示 |
| COP_PARAM_MISSING | 必填参数缺失 |
| COP_URL_VALIDATION_FAIL | URL 白名单校验失败 |
| COP_AI_TIMEOUT | AI 接口超时 |
| COP_AI_FAILED | AI 接口业务失败 |
| COP_CONFIG_VERSION_STALE | 配置版本不一致 |
| COP_MENU_ITEM_NOT_FOUND | 菜单项不存在（关联 menu_item_id 校验失败） |
| COP_MENU_ITEM_DISABLED | 菜单项已禁用 |
| COP_SNAPSHOT_MISMATCH | 菜单项快照与当前菜单项不一致 |
| COP_DIRECTIVE_NOT_FOUND | 反馈对应的指令不存在（trigger_log 查不到） |
| COP_DIRECTIVE_EXPIRED | 指令已过期 |
| COP_FEEDBACK_CONTEXT_MISMATCH | 反馈上下文与指令记录不一致（callId/operatorId/intentCode/actionId） |
| COP_FEEDBACK_DUPLICATE | 反馈已被首次处理（重复反馈，仅记录不影响业务状态） |
| COP_ADMIN_TOKEN_INVALID | Admin 接口鉴权失败 |
```

**验证标准**：

- §29.2 表格条目数 = 15
- 与代码中实际使用的错误码字符串一致（FeedbackServiceImpl 等）

**风险与回滚**：纯文档调整，无业务风险。

---

## 批次 2：单文件代码改动

### Task 2.1 — CR-3 所有 cleanup 方法改为 no-op + 日志

**类型**：代码调整

**涉及文件**：

- `src/main/java/com/cmbchina/cs/assitsvc/asr/DialogHistoryManagerImpl.java`
- `src/main/java/com/cmbchina/cs/assitsvc/asr/SentenceMerger.java`
- `src/main/java/com/cmbchina/cs/assitsvc/session/CallSessionManagerImpl.java`
- `src/main/java/com/cmbchina/cs/assitsvc/core/intent/ExecutedStepsManager.java`
- `src/main/java/com/cmbchina/cs/assitsvc/core/feedback/MuteListManager.java`

**改动方案**：

**通用模板**（每个 cleanup 方法都按这个改）：

```java
// 修改前
public void cleanup(String callId) {
    if (!StringUtils.hasText(callId)) {
        throw new IllegalArgumentException("callId must not be null or empty");
    }
    try {
        redisTemplate.delete(key(callId));
    } catch (DataAccessException e) {
        log.warn("[Mxx] Redis cleanup failed, callId={}", callId, e);
    }
}

// 修改后
/**
 * 通话结束时调用。
 *
 * <p><b>行内规范</b>：Redis 不使用 delete 等阻塞命令，临时数据完全依赖 TTL 自动过期清理。
 * 本方法仅保留日志和方法签名，作为通话生命周期事件钩子；如未来引入其他清理动作可在此扩展。
 */
public void cleanup(String callId) {
    if (!StringUtils.hasText(callId)) {
        throw new IllegalArgumentException("callId must not be null or empty");
    }
    log.debug("[Mxx] Cleanup invoked, relying on TTL expiration, callId={}", callId);
}
```

**对应到每个文件**：

| 文件 | 模块编号 | TTL | 当前 cleanup 行为 |
|------|---------|-----|------------------|
| DialogHistoryManagerImpl | M03 | 1 小时（HistoryProperties.ttlHours） | 删除 history list key |
| SentenceMerger | M02 | 防抖时间+10 分钟缓冲（STATE_TTL_BUFFER_MS） | 删除 state key |
| CallSessionManagerImpl | M04 | 30 分钟（SESSION_TTL_SECONDS） | 删除 session hash key |
| ExecutedStepsManager | M06 | 1 小时（STEPS_TTL_SECONDS） | 删除 steps list key |
| MuteListManager | M11 | 2 小时（CALL_MUTE_TTL_SECONDS） | 删除两个 set keys |

**具体改法**：每个文件中的 `cleanup` 方法体替换为模板中的"修改后"内容，模块编号占位符 `Mxx` 替换为对应实际模块号（M02/M03/M04/M06/M11）。

**特别说明 SentenceMerger**：cleanup 方法外，其他地方（如 `pollDueTasks` 中的 `claimTask` Lua 脚本里的 `redis.call('DEL', KEYS[2])`）也用了 DEL 命令，但这是脚本内部逻辑必须的（实现 CAS claim 语义），**不在本次修改范围**，保留不动。Lua 脚本内部的 DEL 在 Redis 看来是单 key 操作，不构成行内禁止的"应用层显式调用 delete"问题。

**特别说明 MuteListManager**：cleanup 原本 `redisTemplate.delete(Arrays.asList(...))` 删两个 key，现在改为 no-op + 日志，原有的两个 key 路径（intentKey、actionKey）不再清理，由 2 小时 TTL 自动过期。

**注意 import 清理**：cleanup 方法改为 no-op 后，如果 `org.springframework.dao.DataAccessException` 在该文件其他地方还有使用就保留；如果只在 cleanup 中用了就可以删除该 import。其他模块如 DialogHistoryManagerImpl 的 append/getHistory 方法仍在用 DataAccessException，**保留 import**。

**验证标准**：

- 五个 cleanup 方法都不再调用 `redisTemplate.delete(...)`
- 每个 cleanup 方法有 debug 级别日志
- 业务接口 `/copilot/session/unbind` 仍可正常调用（SessionController 中 5 个 cleanup 调用都返回成功，只是不再有 Redis 写入）
- import 列表清理干净（无未使用 import）

**风险与回滚**：

- 风险点：callId 复用场景下旧数据残留。但 callId 来自 CTI 一般全局唯一不复用，且 TTL 最长 2 小时，实际不构成问题。
- 回滚：恢复 cleanup 方法原状。

---

### Task 2.2 — CR-9 FeedbackService 加事务和异常捕获

**类型**：代码调整

**涉及文件**：

- `src/main/java/com/cmbchina/cs/assitsvc/core/feedback/FeedbackServiceImpl.java`

**改动方案**：

**步骤 1**：类上加 `@Transactional`（默认配置）和异常 import：

```java
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
```

**步骤 2**：`handleFeedback` 方法加 `@Transactional`，并在 trigger_log CAS + feedback_log insert 段落用 try-catch 包裹 DB 唯一索引冲突：

```java
@Override
@Transactional
public FeedbackResult handleFeedback(FeedbackRequest request) {
    FeedbackResult validationResult = validateBasic(request);
    if (validationResult != null) {
        return validationResult;
    }

    TriggerLogRecord triggerLog = triggerLogDao.findByDirectiveId(request.getDirectiveId());
    FeedbackResult directiveResult = validateDirective(request, triggerLog);
    if (directiveResult != null) {
        return directiveResult;
    }

    try {
        boolean alreadyEffective = feedbackLogDao.existsEffective(request.getDirectiveId());
        boolean effective = !alreadyEffective
                && triggerLogDao.markDirectiveConsumedIfOpen(request.getDirectiveId());
        metricsService.recordFeedback(request, triggerLog, effective);

        if (!effective) {
            return FeedbackResult.success("DUPLICATE_RECORDED");
        }

        applyEffectiveFeedback(request);
        return FeedbackResult.success("EFFECTIVE");
    } catch (DuplicateKeyException | DataIntegrityViolationException e) {
        // 并发反馈，被另一线程作为有效反馈先插入，本次按重复反馈处理
        log.info("[M11] Concurrent feedback collapsed as duplicate, directiveId={}",
                request.getDirectiveId());
        return FeedbackResult.success("DUPLICATE_RECORDED");
    }
}
```

**注意**：因为加了 `@Transactional`，如果 `applyEffectiveFeedback` 中的 Redis 操作失败（DataAccessException），会触发事务回滚（DB 已有的 trigger_log markDirectiveConsumedIfOpen 也会回滚）。但 Redis 写入失败时 MuteListManager / ExecutedStepsManager 内部都有 try-catch 把异常吞掉只打 warn 日志，**实际上不会抛出到 handleFeedback**，所以事务边界与 Redis 失败处理逻辑相容。

**步骤 3**：在 `CopilotApplication.java` 主类上确认有 `@EnableTransactionManagement`，若没有则添加。检查一下：

```java
// CopilotApplication.java
@SpringBootApplication
@EnableFeignClients(basePackages = "com.cmbchina.cs.assitsvc.infra.feign")
@EnableKafka
@EnableScheduling
@EnableTransactionManagement   // 如果没有则添加
public class CopilotApplication { ... }
```

> 备注：Spring Boot 自动配置默认会启用 transaction management（当 classpath 有 `spring-boot-starter-jdbc` 或 MyBatis-Plus 时），所以 `@EnableTransactionManagement` 通常可省略。但显式声明更稳妥。

**验证标准**：

- FeedbackServiceImpl 类导入了 `@Transactional`、`DuplicateKeyException`、`DataIntegrityViolationException`
- `handleFeedback` 方法有 `@Transactional` 注解
- 并发反馈测试场景（手工 / postman）下，重复 directiveId 不会返回 500，正确返回 `DUPLICATE_RECORDED`
- 编译通过

**风险与回滚**：

- 风险点：加了事务可能让某些边界情况下原本能"部分成功"的状态变为"全部回滚"。但当前实现中 Redis 操作都有内部异常吞噬，DB 操作集中在 markDirectiveConsumedIfOpen + insert，加事务符合预期。
- 回滚：去掉 `@Transactional` 和 try-catch。

---

### Task 2.3 — CR-19 ParamResolver lookup 加 last-segment fallback

**类型**：代码调整

**涉及文件**：

- `src/main/java/com/cmbchina/cs/assitsvc/core/param/ParamResolverServiceImpl.java`
- `src/main/java/com/cmbchina/cs/assitsvc/core/intent/IntentRecognitionTriggerImpl.java`

**改动方案**：

**步骤 1**：修改 `ParamResolverServiceImpl.lookup` 增加 fallback 逻辑（约第 82-108 行）：

```java
private static Object lookup(Map<String, Object> source, String path) {
    if (source == null || !StringUtils.hasText(path)) {
        return null;
    }
    if (source.containsKey(path)) {
        return source.get(path);
    }

    Object current = source;
    String normalizedPath = path.replaceAll("\\[(\\d+)\\]", ".$1");
    String[] parts = normalizedPath.split("\\.");
    for (String part : parts) {
        if (current instanceof Map) {
            current = ((Map<?, ?>) current).get(part);
        } else if (current instanceof List) {
            current = getListValue((List<?>) current, part);
        } else if (current != null && current.getClass().isArray()) {
            current = getArrayValue(current, part);
        } else {
            current = null;
        }
        if (current == null) {
            break;   // 改为 break，进入 fallback 路径
        }
    }
    if (current != null) {
        return current;
    }

    // last-segment fallback：完整路径找不到时，尝试用最后一段作为顶层 key
    if (parts.length > 1) {
        String lastSegment = parts[parts.length - 1];
        return source.get(lastSegment);
    }
    return null;
}
```

**步骤 2**：修改 `IntentRecognitionTriggerImpl.buildParamContext` 删除重复的 customerId 注入（约第 144-145 行）：

```java
// 修改前
putIfHasText(sessionData, "customer.customerId", session.getCustomerId());
putIfHasText(sessionData, "customer.customerType", session.getCustomerType());
...
putIfHasText(sessionData, "customerId", session.getCustomerId());      // ★ 删除
putIfHasText(sessionData, "customerType", session.getCustomerType());  // ★ 删除

// 修改后
putIfHasText(sessionData, "customer.customerId", session.getCustomerId());
putIfHasText(sessionData, "customer.customerType", session.getCustomerType());
putIfHasText(sessionData, "customer.idNo", session.getIdNo());
putIfHasText(sessionData, "customer.noIdType", session.getNoIdType());
putIfHasText(sessionData, "customer.palmLifeUserId", session.getPalmLifeUserId());
putIfHasText(sessionData, "customer.phoneNo", session.getPhoneNo());
putIfHasText(sessionData, "customer.phoneNoNoZero", session.getPhoneNoNoZero());
putIfHasText(sessionData, "accounts[0].accountNo", session.getAccountNo());
putIfHasText(sessionData, "customer.address", session.getAddress());
putIfHasText(sessionData, "customer.addressEncode", session.getAddressEncode());
// 不再注入 "customerId" / "customerType" 顶层重复 key
```

**验证标准**：

- ParamResolverServiceImpl.lookup 在 path 完整匹配失败时会用最后一段再查一次
- IntentRecognitionTriggerImpl.buildParamContext 不再有 `putIfHasText(sessionData, "customerId", ...)` 等重复
- 配置中 paramKey 为 `customerId`（无前缀）的 ItemParam 仍能正常解析（通过 last-segment fallback 命中 `customer.customerId`）
- 配置中 paramKey 为 `customer.customerId` 的 ItemParam 仍能直接命中（fallback 路径不触发）

**风险与回滚**：

- 风险点：last-segment fallback 可能在多个 key 共享相同最后一段时产生歧义。例如 `customer.id` 和 `account.id`，paramKey 写 `id` 时会命中第一个找到的。当前 sessionData 中没有这种命名冲突。
- 回滚：还原两处修改即可。

---

## 批次 3：跨模块改动

### Task 3.1 — CR-5 推送服务短期方案（仅后端）

**类型**：代码调整

**涉及文件**：

- `src/main/java/com/cmbchina/cs/assitsvc/push/CopilotPushService.java`
- `src/main/java/com/cmbchina/cs/assitsvc/core/intent/IntentRecognitionTriggerImpl.java`
- `src/main/java/com/cmbchina/cs/assitsvc/infra/metrics/MetricsService.java`
- `src/main/java/com/cmbchina/cs/assitsvc/infra/metrics/TriggerLogDao.java`（或 MybatisTriggerLogDao）
- `src/main/java/com/cmbchina/cs/assitsvc/infra/metrics/MybatisTriggerLogDao.java`（确认实现）

**改动方案**：

**步骤 1**：CopilotPushService 重命名方法 + 修改语义：

```java
// 修改前
public boolean pushDirective(DirectiveDTO directive) {
    if (directive == null || !StringUtils.hasText(directive.getOperatorId())) {
        throw new IllegalArgumentException("directive and operatorId must not be empty");
    }
    try {
        redisTemplate.convertAndSend(properties.getClusterPushChannel(), JSON.toJSONString(directive));
        log.info("[M10] Directive published, directiveId={}, operatorId={}",
            directive.getDirectiveId(), directive.getOperatorId());
        return true;
    } catch (Exception e) {
        log.error("[M10] Directive publish failed, directiveId={}, operatorId={}",
            directive.getDirectiveId(), directive.getOperatorId(), e);
        return false;
    }
}

// 修改后
/**
 * 异步发布推荐指令到集群推送通道（Redis Pub/Sub）。
 *
 * <p><b>语义</b>：返回 true 仅表示 Redis publish 调用成功，<b>不保证消息已送达任何 WebSocket 连接</b>。
 * 真实送达状态需要前端 ACK 反馈接口确认（本期未实现，预留方法 markDirectiveDelivered）。
 *
 * @param directive 推荐指令
 * @return true 表示 Redis publish 调用成功
 */
public boolean publishDirectiveAsync(DirectiveDTO directive) {
    if (directive == null || !StringUtils.hasText(directive.getOperatorId())) {
        throw new IllegalArgumentException("directive and operatorId must not be empty");
    }
    try {
        redisTemplate.convertAndSend(properties.getClusterPushChannel(), JSON.toJSONString(directive));
        log.info("[M10] Directive published to cluster channel, directiveId={}, operatorId={}",
            directive.getDirectiveId(), directive.getOperatorId());
        return true;
    } catch (Exception e) {
        log.error("[M10] Directive publish failed, directiveId={}, operatorId={}",
            directive.getDirectiveId(), directive.getOperatorId(), e);
        return false;
    }
}
```

**步骤 2**：CopilotPushService 增加 `markDirectiveDelivered` 预留方法（本次不接入接口，仅预留签名）：

```java
/**
 * 标记指令已送达前端（前端 ACK 反馈调用）。
 *
 * <p><b>本期未实现</b>。当前 directive_status 只有 PUBLISHED 状态。
 * 未来前端 SDK 增加 ACK 反馈后，由反馈接口调用本方法把 trigger_log 状态推进到 DELIVERED。
 *
 * @param directiveId 指令 ID
 */
public void markDirectiveDelivered(String directiveId) {
    if (!StringUtils.hasText(directiveId)) {
        throw new IllegalArgumentException("directiveId must not be null or empty");
    }
    // TODO 前端 ACK 接入后，调用 triggerLogDao.markDirectiveDelivered(directiveId)
    log.debug("[M10] markDirectiveDelivered placeholder, directiveId={}", directiveId);
}
```

**步骤 3**：IntentRecognitionTriggerImpl 调用处更新方法名（约第 99 行）：

```java
// 修改前
boolean pushed = pushService.pushDirective(directive);

// 修改后
boolean published = pushService.publishDirectiveAsync(directive);
if (published) {
    metricsService.recordTriggerSuccess(directive, session, candidate, candidateCount);
} else {
    metricsService.recordTriggerFailure(callId, session, intentResult.getIntentCode(),
            intentResult.getIntentName(), ReasonCodeConstants.PUSH_FAILED,
            FilterStageConstants.PUSH, configCache.getCurrentVersion());
}
return published;
```

**步骤 4**：MetricsService.recordTriggerSuccess 中 `directive_status` 字段从 `"PUSHED"` 改为 `"PUBLISHED"`（约第 48 行）：

```java
// 修改前
.directiveStatus("PUSHED")

// 修改后
.directiveStatus("PUBLISHED")
```

**步骤 5**：dd-v1.2.md 中相关章节同步更新（§21.5 cs_copilot_trigger_log 表字段说明）：

```markdown
| directive_status | varchar(16) | 否 | **DD-V1.2 P0-6** PUBLISHED/EXPIRED/CONSUMED/DELIVERED |
```

把原 `PUSHED` 改为 `PUBLISHED`，新增 `DELIVERED` 表示前端 ACK 后的状态（预留，本期不写入）。

**步骤 6**：在 dd-v1.2.md §16.5 WebSocket 推送章节末尾加一段说明：

```markdown
> **送达保证说明**：CopilotPushService.publishDirectiveAsync 只保证 Redis Pub/Sub 发布成功，不保证指令送达任何 WebSocket 连接。trigger_log.directive_status=PUBLISHED 表示已发布，真实送达需要前端 ACK 反馈机制（本期未实现，预留 markDirectiveDelivered 方法）。BI 看板计算覆盖率时应注意此语义。
```

**验证标准**：

- 全局 grep `pushDirective(` 已被替换为 `publishDirectiveAsync(`
- IntentRecognitionTriggerImpl 中变量名从 `pushed` 改为 `published`
- trigger_log 表新插入数据的 directive_status = `PUBLISHED`（旧数据保留 `PUSHED` 不动）
- CopilotPushService 含 `markDirectiveDelivered` 方法但只有占位实现
- 编译通过，启动正常

**风险与回滚**：

- 数据库中现有的 trigger_log.directive_status='PUSHED' 旧数据不影响新逻辑（语义相通，PUBLISHED 含义更准确）
- BI 看板 SQL 若按 `directive_status='PUSHED'` 过滤需要同步更新为 `IN ('PUSHED','PUBLISHED')` 或只用 PUBLISHED
- 回滚：方法重命名还原、状态字符串还原

---

### Task 3.2 — CR-6 区分 AI 失败类型

**类型**：代码调整

**涉及文件**：

- `src/main/java/com/cmbchina/cs/assitsvc/infra/metrics/ReasonCodeConstants.java`
- `src/main/java/com/cmbchina/cs/assitsvc/core/intent/IntentRecognitionService.java`（接口）
- `src/main/java/com/cmbchina/cs/assitsvc/core/intent/IntentRecognitionServiceImpl.java`
- `src/main/java/com/cmbchina/cs/assitsvc/core/intent/IntentRecognitionTriggerImpl.java`
- 新增 `src/main/java/com/cmbchina/cs/assitsvc/core/intent/IntentRecognitionOutcome.java`

**改动方案**：

**步骤 1**：扩展 ReasonCodeConstants（新增 AI 失败分类常量）：

```java
// ReasonCodeConstants.java 中追加
public static final String AI_CIRCUIT_BREAKER_OPEN = "AI_CIRCUIT_BREAKER_OPEN";
public static final String AI_CALL_LIMIT_EXCEEDED = "AI_CALL_LIMIT_EXCEEDED";
public static final String AI_NETWORK_FAIL = "AI_NETWORK_FAIL";
public static final String AI_BUSINESS_FAIL = "AI_BUSINESS_FAIL";
public static final String NO_CUSTOMER_HISTORY = "NO_CUSTOMER_HISTORY";
// 已有 INTENT_EMPTY 保留
```

**步骤 2**：新增 IntentRecognitionOutcome 包装类：

```java
package com.cmbchina.cs.assitsvc.core.intent;

import com.cmbchina.cs.assitsvc.domain.IntentResult;
import lombok.Builder;
import lombok.Getter;

/**
 * AI 意图识别结果包装，区分成功与多种失败分类。
 */
@Getter
@Builder
public class IntentRecognitionOutcome {

    /** 识别结果；仅 success=true 时有值。 */
    private final IntentResult intent;

    /** 是否成功识别到非空 intentCode。 */
    private final boolean success;

    /** 失败原因码；与 ReasonCodeConstants 对齐。 */
    private final String failReason;

    public static IntentRecognitionOutcome success(IntentResult intent) {
        return IntentRecognitionOutcome.builder()
                .intent(intent)
                .success(true)
                .build();
    }

    public static IntentRecognitionOutcome failure(String failReason) {
        return IntentRecognitionOutcome.builder()
                .success(false)
                .failReason(failReason)
                .build();
    }
}
```

**步骤 3**：修改 IntentRecognitionService 接口：

```java
public interface IntentRecognitionService {
    /**
     * 调用 AI 进行意图识别。
     *
     * @param callId 通话 ID
     * @return 识别结果包装；含成功/失败语义和失败分类
     */
    IntentRecognitionOutcome recognize(String callId);
}
```

**步骤 4**：修改 IntentRecognitionServiceImpl 实现，区分各种失败路径：

```java
@Override
@CircuitBreaker(name = "aiIntentClient", fallbackMethod = "fallback")
public IntentRecognitionOutcome recognize(String callId) {
    if (!StringUtils.hasText(callId)) {
        throw new IllegalArgumentException("callId must not be null or empty");
    }
    if (!allowAiCall(callId)) {
        log.warn("[M06] AI call limit exceeded, callId={}, maxAiCalls={}", callId, maxAiCalls);
        return IntentRecognitionOutcome.failure(ReasonCodeConstants.AI_CALL_LIMIT_EXCEEDED);
    }

    List<AiDialogMessage> customerOnly = customerOnlyHistory(callId);
    if (customerOnly.isEmpty()) {
        log.debug("[M06] No customer history for AI recognition, callId={}", callId);
        return IntentRecognitionOutcome.failure(ReasonCodeConstants.NO_CUSTOMER_HISTORY);
    }

    IntentRecognitionRequest request = IntentRecognitionRequest.builder()
            .sessionId(callId)
            .requestId(generateRequestId())
            .history(customerOnly)
            .executedSteps(stepsManager.getSteps(callId))
            .intentTree(treeLoader.getTree())
            .treeVersion(treeLoader.getVersion())
            .build();

    try {
        IntentRecognitionResponse response = feignClient.recognize(request);
        return parseResponse(callId, request.getRequestId(), response);
    } catch (FeignException e) {
        log.warn("[M06] AI intent recognition failed, callId={}, status={}", callId, e.status(), e);
        throw e;  // 让熔断器捕获，fallback 会区分
    }
}

/**
 * Resilience4j 熔断 fallback。
 *
 * <p>区分熔断打开（CallNotPermittedException）与网络异常（FeignException 等）。
 */
public IntentRecognitionOutcome fallback(String callId, Throwable t) {
    if (t instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
        log.warn("[M06] AI circuit breaker open, callId={}", callId);
        return IntentRecognitionOutcome.failure(ReasonCodeConstants.AI_CIRCUIT_BREAKER_OPEN);
    }
    log.warn("[M06] AI call fallback by network/timeout failure, callId={}", callId, t);
    return IntentRecognitionOutcome.failure(ReasonCodeConstants.AI_NETWORK_FAIL);
}

private IntentRecognitionOutcome parseResponse(String callId, String requestId, IntentRecognitionResponse response) {
    if (response == null) {
        log.warn("[M06] AI response is null, callId={}, requestId={}", callId, requestId);
        return IntentRecognitionOutcome.failure(ReasonCodeConstants.AI_BUSINESS_FAIL);
    }
    if (!"1000".equals(response.getRespCode())) {
        log.warn("[M06] AI response code not success, callId={}, requestId={}, respCode={}, respMsg={}",
                callId, requestId, response.getRespCode(), response.getRespMsg());
        return IntentRecognitionOutcome.failure(ReasonCodeConstants.AI_BUSINESS_FAIL);
    }
    IntentRecognitionResponse.DataNode data = response.getData();
    if (data == null || !StringUtils.hasText(data.getIntentCode())) {
        log.debug("[M06] AI returned empty intent, callId={}, requestId={}", callId, requestId);
        return IntentRecognitionOutcome.failure(ReasonCodeConstants.INTENT_EMPTY);
    }

    return IntentRecognitionOutcome.success(IntentResult.builder()
            .intentCode(data.getIntentCode())
            .intentName(data.getIntentName())
            .build());
}
```

**步骤 5**：IntentRecognitionTriggerImpl 调用处更新（约第 60-67 行）：

```java
// 修改前
IntentResult intentResult = intentRecognitionService.recognize(callId);
if (intentResult == null || !StringUtils.hasText(intentResult.getIntentCode())) {
    metricsService.recordTriggerFailure(callId, session, null, null,
            ReasonCodeConstants.INTENT_EMPTY,
            FilterStageConstants.INTENT_RECOGNITION,
            configCache.getCurrentVersion());
    return;
}

// 修改后
IntentRecognitionOutcome outcome = intentRecognitionService.recognize(callId);
if (!outcome.isSuccess()) {
    metricsService.recordTriggerFailure(callId, session, null, null,
            outcome.getFailReason(),
            FilterStageConstants.INTENT_RECOGNITION,
            configCache.getCurrentVersion());
    return;
}
IntentResult intentResult = outcome.getIntent();
```

**步骤 6**：检查 NoOpIntentRecognitionTrigger（如果是 NoOp 包装 IntentRecognitionService 的话）和 IntentRecognitionService 的任何其他实现，同步更新返回类型。

**验证标准**：

- 全局 grep `IntentRecognitionService.recognize` 都已适配新返回类型
- AI 熔断打开时 trigger_log.reason_code = `AI_CIRCUIT_BREAKER_OPEN`
- AI 配额超限时 reason_code = `AI_CALL_LIMIT_EXCEEDED`
- AI 网络异常时 reason_code = `AI_NETWORK_FAIL`
- AI 返回空意图时 reason_code = `INTENT_EMPTY`
- 编译通过

**风险与回滚**：

- 风险点：接口返回类型变化，所有调用方都要改。已识别的调用方只有 IntentRecognitionTriggerImpl。
- 回滚：还原接口签名 + 实现

---

### Task 3.3 — CR-14 AI 调用计数挪到成功后

**类型**：代码调整

**涉及文件**：

- `src/main/java/com/cmbchina/cs/assitsvc/core/intent/IntentRecognitionServiceImpl.java`

**改动方案**：

把当前 `allowAiCall` 拆分为两步：

1. `precheckAiCall(callId)` 只读 Redis 计数判断是否达上限（不 INCR）
2. `incrementAiCallCount(callId)` 在 Feign 成功返回后 INCR

```java
// 修改前
private static final DefaultRedisScript<Long> AI_COUNT_SCRIPT = new DefaultRedisScript<>(
        "local count = redis.call('INCR', KEYS[1]); "
                + "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; "
                + "return count;",
        Long.class);

private boolean allowAiCall(String callId) {
    String key = AI_COUNT_KEY_PREFIX + callId + AI_COUNT_KEY_SUFFIX;
    try {
        Long count = redisTemplate.execute(AI_COUNT_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(AI_COUNT_TTL_SECONDS));
        return count == null || count <= maxAiCalls;
    } catch (DataAccessException e) {
        log.warn("[M06] Redis AI call count failed, callId={}", callId, e);
        return true;
    }
}

// 修改后
// 保留 AI_COUNT_SCRIPT 不变（仍用 INCR + EXPIRE 原子操作）

private boolean precheckAiCall(String callId) {
    String key = AI_COUNT_KEY_PREFIX + callId + AI_COUNT_KEY_SUFFIX;
    try {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return true;
        }
        long count = Long.parseLong(value);
        return count < maxAiCalls;
    } catch (NumberFormatException | DataAccessException e) {
        log.warn("[M06] Redis AI call precheck failed, callId={}", callId, e);
        return true;  // fail open
    }
}

private void incrementAiCallCount(String callId) {
    String key = AI_COUNT_KEY_PREFIX + callId + AI_COUNT_KEY_SUFFIX;
    try {
        Long count = redisTemplate.execute(AI_COUNT_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(AI_COUNT_TTL_SECONDS));
        if (count != null && count > maxAiCalls) {
            log.warn("[M06] AI call count exceeded after success, callId={}, count={}", callId, count);
        }
    } catch (DataAccessException e) {
        log.warn("[M06] Redis AI call count increment failed, callId={}", callId, e);
    }
}
```

调整 `recognize` 方法：

```java
@Override
@CircuitBreaker(name = "aiIntentClient", fallbackMethod = "fallback")
public IntentRecognitionOutcome recognize(String callId) {
    if (!StringUtils.hasText(callId)) {
        throw new IllegalArgumentException("callId must not be null or empty");
    }
    if (!precheckAiCall(callId)) {
        log.warn("[M06] AI call limit exceeded, callId={}, maxAiCalls={}", callId, maxAiCalls);
        return IntentRecognitionOutcome.failure(ReasonCodeConstants.AI_CALL_LIMIT_EXCEEDED);
    }

    List<AiDialogMessage> customerOnly = customerOnlyHistory(callId);
    if (customerOnly.isEmpty()) {
        return IntentRecognitionOutcome.failure(ReasonCodeConstants.NO_CUSTOMER_HISTORY);
    }

    IntentRecognitionRequest request = ... ;

    try {
        IntentRecognitionResponse response = feignClient.recognize(request);
        IntentRecognitionOutcome outcome = parseResponse(callId, request.getRequestId(), response);
        if (outcome.isSuccess()) {
            incrementAiCallCount(callId);  // ★ 仅在成功后 INCR
        }
        return outcome;
    } catch (FeignException e) {
        log.warn("[M06] AI intent recognition failed, callId={}, status={}", callId, e.status(), e);
        throw e;
    }
}
```

**说明**：

- 失败的 AI 调用（熔断 / 网络 / 配额查 / 业务错 / 空意图）都不计数
- 成功调用才 +1，保护"防止恶意循环消耗 AI 资源"的初衷
- 高并发场景下，precheck 和 increment 之间有 TOCTOU 窗口（两个请求都 precheck 通过然后都 INCR），但 AI_COUNT_SCRIPT 的 INCR 是原子的，单通话超限最多多 1-2 次调用，可接受

**验证标准**：

- AI 调用前不再无条件 INCR 计数
- AI 调用熔断/失败时 Redis 计数不变
- AI 调用成功时计数 +1
- 单通话连续 AI 失败不会消耗配额
- 编译通过

**风险与回滚**：

- 风险点：precheck 和 increment 之间的并发窗口可能让计数略超上限
- 回滚：还原 allowAiCall 实现

---

### Task 3.4 — CR-15 MybatisCopilotConfigRepository 收集所有错误后一次性抛

**类型**：代码调整

**涉及文件**：

- `src/main/java/com/cmbchina/cs/assitsvc/config/MybatisCopilotConfigRepository.java`

**改动方案**：

把 `loadSnapshot` 改为收集所有 action 的校验错误后统一抛出：

```java
@Override
public CopilotConfigSnapshot loadSnapshot(String versionId) {
    if (!StringUtils.hasText(versionId)) {
        throw new IllegalStateException("Copilot config version is empty");
    }

    List<CopilotActionRow> actionRows = defaultList(configMapper.selectEnabledActions(versionId));
    List<IntentMapping> mappings = defaultList(configMapper.selectEnabledMappings(versionId));

    Set<Long> menuItemIds = actionRows.stream()
            .map(CopilotActionRow::getMenuItemId)
            .filter(id -> id != null)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    Map<Long, CopilotMenuItemRow> menuItems = loadMenuItems(menuItemIds);

    Map<String, CopilotActionConfig> actionById = new LinkedHashMap<>();
    List<String> errors = new ArrayList<>();
    for (CopilotActionRow row : actionRows) {
        try {
            CopilotActionConfig action = toActionConfig(row, menuItems);
            actionById.put(action.getActionId(), action);
        } catch (IllegalStateException e) {
            errors.add(e.getMessage());
        }
    }

    if (!errors.isEmpty()) {
        throw new IllegalStateException(
                "Copilot config validation failed, versionId=" + versionId
                        + ", errorCount=" + errors.size()
                        + ", errors=" + errors);
    }

    Map<String, List<ActionReference>> intentToActions = buildIntentIndex(mappings);
    return CopilotConfigSnapshot.builder()
            .versionId(versionId)
            .buildTime(Instant.now().toString())
            .intentToActions(intentToActions)
            .actionById(actionById)
            .build();
}
```

**说明**：

- 单个 action 的 toActionConfig 内部仍然抛 IllegalStateException（含 menu_item 缺失、disabled、snapshot 不一致三种错误）
- loadSnapshot 捕获每个 action 的错误，累积到 errors 列表
- 全部 action 跑完后，如果 errors 非空，统一抛出，错误消息含 versionId 和所有错误列表
- 配置后台调用 `/copilot/admin/config/validate` 时一次能看到所有错误，运营改一遍而不是循环改

**验证标准**：

- 单 action 失败时仍然整版加载失败（行为不变）
- 多 action 失败时 errors 列表含全部错误，单条异常消息含 errorCount 和 errors 详情
- 异常消息中含 versionId
- 编译通过

**风险与回滚**：

- 风险点：异常消息变长（含所有错误），日志中堆栈消息较大，对运营友好但占用日志空间
- 回滚：还原 fail-fast 实现

---

## 批次 4：文档补充

### Task 4.1 — CR-4 dd-v1.2.md 增加单 slot 风险说明

**类型**：dd-v1.2.md 文档调整

**涉及文件**：

- `docs/dd-v1.2.md`（§9.6 + §32.1 附近）

**改动方案**：

**步骤 1**：§9.6 末尾追加：

```markdown
### 9.7 Redis Cluster 单 slot 路由（DD-V1.2 决策）

M02 防抖状态使用统一 hash tag `{asr_merge}`，目的是让 `pollDueTasks` 中的 Lua claim 脚本能跨 `stateKey(callId)` 和 `DUE_QUEUE_KEY` 两个 key 执行（Redis Cluster 要求脚本中所有 key 同 slot）。

**已知 trade-off**：

- 所有 callId 的 sentence merger 状态集中在 Redis Cluster 中**单个 master 节点的单个 slot**
- 单 Pod 高峰估算：~50 次/秒 ZADD（每次 handleSentence 三次写）+ 200ms 轮询
- 全集群 N Pod 高峰：约 50N 次/秒 写 + N 次/200ms 轮询，对单 Redis 节点负载有限（典型 Redis 节点 10w QPS 以上）

**扩容触发条件**：

- 单通话日均 > 5 万通（当前 §34 估算 2 万通）
- 或单 Pod 高峰 sentence merger 写入 > 200 次/秒
- 触发后需切换到独立 Redis 实例承载延迟队列，或改用 Redisson DelayedQueue 重新实现 M02

**不在本期范围**：F17 sentence merger 独立 Redis 实例（待实现）
```

并在 §4.2 后续待实现模块表中追加：

```markdown
| F17 | sentence merger 独立 Redis 实例 / Redisson 延迟队列改造 | P3 |
```

**步骤 2**：§32.1 关键运维事项表格增加一行：

```markdown
| Redis 单 slot 监控 | 监控 `{asr_merge}` slot 对应 master 节点的 CPU/QPS，超阈值时启动 F17 改造 |
```

**验证标准**：

- §9.7 新增章节存在
- §4.2 含 F17
- §32.1 含 Redis 单 slot 监控运维项

**风险与回滚**：纯文档调整。

---

## 不修复的项

下表记录本次评审中决定**不修复**的 16 个问题，连同决策理由和后续路径。

| 编号 | 标题 | 不修复理由 | 后续路径 |
|------|------|-----------|---------|
| DD-1 | §16.3 DirectiveBuilder 伪代码引用 getCopilotExt | 伪代码后续可能还需要改，本期不动 | 后续 review 一并处理 |
| DD-2 | §17.4 FeedbackService 伪代码引用 itemId | 同上 | 后续 review 一并处理 |
| DD-3 | §15.3 ParamResolver 伪代码与代码不一致 | 同上 | 后续 review 一并处理 |
| DD-6 | P0-7 dedup 命中后历史不入库 | 上游 ASR 契约问题；上游承诺 sentenceId 全局唯一时不复用 | 上线前与 ASR 团队对齐 |
| DD-7 | 配置发布缺 DRAFT 状态 | 现有 PUBLISHED 行最后插入已规避中间态可见，发布失败回滚由配置后台 M12 实现期负责 | M12 实施时一并设计 |
| DD-8 | Cookie 占位符校验时机循环依赖 | MENU_ITEM 模式不进入参数解析路径，实际不触发此问题 | - |
| DD-9 | 反馈接口未含服务端权限/身份校验 | 鉴权后续用生产逻辑接入工作台 SSO | 上线前接入 |
| DD-10 | §34.1 customer_id 90 天保留期且未脱敏 | 本期不考虑字段的脱敏与加密 | F05 数据脱敏框架（待实现） |
| DD-11 | 多 Pod 轮询 30s lag 与 callId 哈希分区前提冲突 | 接受 30s 配置 lag 窗口，发布期间允许少量反馈状态不一致 | F18 强一致广播刷新（待实现） |
| DD-12 | ACCEPTED 反馈语义与"打开成功"绑定过紧 | 反馈枚举语义不细分，本期使用 4 种枚举 | 后续看板有需求时再细分 |
| CR-2 | WebSocket 握手 operatorId 无身份校验 | WebSocket 用工作台已有封装组件，已具备身份校验 | 已在工作台层面解决 |
| CR-10 | AI URL 协议在所有环境配置为 http | 行内 AI 接口暂时不支持 HTTPS，保持 http | AI 团队改造时同步切换 |
| CR-12 | ConfigVersionPoller 无 leader election | N Pod 并发刷新 DB 负载在容量内可接受 | 容量预警时再评估 |
| CR-13 | AdminController token 比较未用恒定时间算法 | 后续用生产 SSO 鉴权替代自管 token | 上线前接入 SSO |
| CR-16 | trigger_log 含 customer_id 未脱敏 | 不做脱敏 | F05（待实现） |
| CR-17 | SentenceContinuityDetector 词典硬编码 | 词典硬编码可接受，与 DD-V1.2 §9.3 一致 | 后续配置化 |
| CR-18 | UrlBuilderImpl rawQuery 透传未做协议防御 | baseUrl 由运营配置，内部信任，前端 window.location 不执行 javascript: 协议 | - |

---

## 给 Claude Code 的执行指引

### 总体协作模式

1. 启动 Claude Code 时，第一个 prompt 引用本文档 + CLAUDE.md
2. Claude Code 按 4 批次顺序执行，**每批之间不需要业务方确认**
3. 每个 Task 完成后，Claude Code 自检：
   - 编译通过（不执行 `mvn`，但代码层面没有明显错误）
   - 文件改动符合 Task 描述
   - 没有引入未声明的依赖
4. 一批次结束后开始下一批次

### 推荐启动 prompt

```
@docs/code-review-fix-plan.md @CLAUDE.md @docs/dd-v1.2.md

请按 docs/code-review-fix-plan.md 的批次顺序执行所有任务。要求：

1. 使用 superpowers:subagent-driven-development 工作流
2. 每个批次单独 commit，commit 信息格式：
   [Code Review Batch N] 简要描述
   - Task X.Y: ...
   - Task X.Z: ...
   Refs: docs/code-review-fix-plan.md
3. 严格遵守 CLAUDE.md 中的项目约束（JDK 8 / javax.* / FastJSON 2.x / @Slf4j 等）
4. 不写单元测试（CLAUDE.md 明示）
5. 每个 Task 改动控制在该 Task 描述的文件范围，不要顺手改其他地方
6. 全部 4 批次完成后，最后一次 commit 包含本计划文档归档：
   把 docs/code-review-fix-plan.md 重命名为 docs/archived/code-review-fix-plan-2026-05-15.md
   或者保留原位但加上"已执行完成"标记。

开始执行。
```

### 分支命名建议

- `feature/code-review-fix-batch-1`
- `feature/code-review-fix-batch-2`
- `feature/code-review-fix-batch-3`
- `feature/code-review-fix-batch-4`

或统一一个分支 `feature/code-review-fixes-2026-05-15` 包含全部 4 批次 commit。

### subagent 调度建议

每个批次建议作为独立的 plan 启动 SDD：

- 批次 1（8 个 Task）：拆为 1-2 个 subagent 并行执行（纯文档/配置改动，互不冲突）
- 批次 2（3 个 Task）：1 个 subagent 串行（CR-3 涉及多文件统一改法，CR-9/CR-19 涉及不同模块）
- 批次 3（4 个 Task）：1 个 subagent 串行（CR-5/CR-6 之间有依赖，CR-6/CR-14 改同一文件需顺序处理）
- 批次 4（1 个 Task）：直接执行

### 验证标准（每批次完成后必查）

1. **编译标准**：所有改动文件可以通过 IDE 编译（不依赖 mvn）
2. **风格标准**：
   - 4 空格缩进，UTF-8 编码
   - 类有 Javadoc 描述
   - 关键变更含模块编号（[M02]、[M06] 等）的日志
3. **依赖标准**：
   - 没有引入未声明的 import
   - pom.xml 改动符合 Task 描述
4. **回归标准**：
   - 改动文件之外的其他文件没有变化（用 git diff 验证）

### 完成后产出

Claude Code 完成 4 批次后应产出：

- 4 次 commit（或 1 个分支含 4 次 commit）的 push
- 一份简要的执行报告（可作为 PR 描述），列出每个 Task 实际改了哪些文件、是否有偏离本计划的地方
- 如果遇到本计划描述与实际代码不符的情况（比如代码已经被改过、行号不对），停下来在 commit 信息或报告中明示，**不要静默调整**

---

## 附录：本计划与原 review 报告的对应关系

| 本计划 Task | 对应 review 编号 | 决策摘要 |
|------------|------------------|---------|
| Task 1.1 | CR-1 | 撤销 Jedis 硬约束（CLAUDE.md） |
| Task 1.2 | CR-7 | MyBatis-Plus 入文档（CLAUDE.md） |
| Task 1.3 | CR-8 | UAT 域名挪出主配置 |
| Task 1.4 | CR-11 | 去掉冗余 hash tag |
| Task 1.5 | CR-20 | @ConfigurationProperties |
| Task 1.6 | CR-21 | 移除未用测试依赖 |
| Task 1.7 | DD-4 | 关闭 Q1-Q5 |
| Task 1.8 | DD-5 | 补齐错误码 |
| Task 2.1 | CR-3 | cleanup 改 no-op |
| Task 2.2 | CR-9 | 事务 + 异常捕获 |
| Task 2.3 | CR-19 | lookup fallback |
| Task 3.1 | CR-5 | 推送服务短期方案 |
| Task 3.2 | CR-6 | 区分 AI 失败类型 |
| Task 3.3 | CR-14 | 计数挪到成功后 |
| Task 3.4 | CR-15 | 收集所有错误后一次性抛 |
| Task 4.1 | CR-4 | 单 slot 风险说明 |

---

**本文档结束**。Claude Code 现在可以开始执行了。
