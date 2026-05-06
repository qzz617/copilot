# Prompt 模板：Code Review

> 用于让 Claude Code 帮助 review 代码（自检 + Pre-MR）。

## 模板（推荐使用）

```
@CLAUDE.md @docs/coding-standards.md @docs/dd-v1.2.md

请 review 以下代码：

【文件】
@{{文件路径}}

【对应模块】
{{M01 / M02 等}}，DD-V1.2 第 {{章节}}

【Review 重点】
1. 是否符合 Spring Boot 2.7 + JDK 8 约束
2. 是否符合 coding-standards.md
3. 是否符合 DD-V1.2 设计意图
4. 是否有过度设计
5. 是否主动实现了"本期不做"的功能
6. SonarQube 主流规则（圈复杂度、方法长度等）
7. 关键日志是否含 callId / traceId
8. 异常处理是否合理
9. 命名是否规范

【输出格式】
- 严重问题（必须改）
- 一般问题（建议改）
- 风格问题（可选改）
- 优点（值得保留的）
```

## 示例 1：单个模块 Review

```
@CLAUDE.md @docs/coding-standards.md @docs/dd-v1.2.md

请 review 以下代码：

【文件】
@src/main/java/com/cmbchina/cs/assitsvc/asr/SentenceMergerImpl.java
@src/main/java/com/cmbchina/cs/assitsvc/asr/SentenceContinuityDetectorImpl.java

【对应模块】
M02 句间合并器，DD-V1.2 第 9 章

【Review 重点】
1. 防抖 timer 实现是否正确
2. 沉默 timer 是否独立（DD-V1.2 P1-3 设计）
3. 多 Pod 一致性靠 Kafka 分区，本地 timer 是否安全
4. 是否符合 coding-standards
5. 关键日志是否含 callId

【输出格式】
（同上模板）
```

## 示例 2：MR Review

```
@CLAUDE.md @docs/coding-standards.md @docs/dd-v1.2.md

请 review 这个 MR 的代码：

【MR 描述】
[M11] 实现反馈接口（含 P0-6 修订）

【涉及文件】
@src/main/java/com/cmbchina/cs/assitsvc/api/controller/FeedbackController.java
@src/main/java/com/cmbchina/cs/assitsvc/core/feedback/FeedbackServiceImpl.java
@src/main/java/com/cmbchina/cs/assitsvc/core/feedback/MuteListManagerImpl.java

【Review 重点】
1. 是否实现了 DD-V1.2 P0-6 的指令校验（directive_id 反查）
2. 是否实现了 P1-14 的 is_effective 幂等
3. 是否实现了 P1-15 的 trigger_log_id 反查
4. 业务异常处理是否合理
5. 是否符合 coding-standards

【输出格式】
（同上模板）
```

## 示例 3：架构一致性 Review

```
@CLAUDE.md @docs/dd-v1.2.md

我已经完成了阶段 5（核心链路），帮我做一次架构一致性 review。

【涉及包】
@src/main/java/com/cmbchina/cs/assitsvc/asr/
@src/main/java/com/cmbchina/cs/assitsvc/core/intent/
@src/main/java/com/cmbchina/cs/assitsvc/core/match/
@src/main/java/com/cmbchina/cs/assitsvc/core/param/
@src/main/java/com/cmbchina/cs/assitsvc/core/directive/
@src/main/java/com/cmbchina/cs/assitsvc/push/

【Review 重点】
1. 包结构是否符合 CLAUDE.md 规范
2. 类的职责划分是否合理（单一职责）
3. 模块间依赖是否清晰
4. 是否有循环依赖
5. 数据流是否清晰（从 Kafka 到 WebSocket）

【输出格式】
- 整体评估
- 具体问题（按文件列出）
- 改进建议
```

## Review 检查清单（人工 check）

每次拿到 Claude 生成的代码，至少 check 以下点：

### 基础规范

- [ ] 用 `javax.*` 不是 `jakarta.*`
- [ ] 没有 JDK 9+ 语法（record / switch 表达式 / instanceof pattern / sealed）
- [ ] 缩进 4 空格
- [ ] 行宽 ≤ 120
- [ ] 包名 `com.cmbchina.cs.assitsvc.{layer}`
- [ ] 接口 `XxxService` / 实现 `XxxServiceImpl`

### Lombok 使用

- [ ] DTO 用 `@Data + @Builder`
- [ ] Service 用 `@Service + @RequiredArgsConstructor + @Slf4j`
- [ ] 没用 `@SneakyThrows`

### 依赖

- [ ] 引入的依赖都在 pom.xml 中
- [ ] 用 FastJSON 2.x（`com.alibaba.fastjson2`）不是 1.x
- [ ] 用 Jedis 不是 Lettuce
- [ ] 没有意外引入新依赖

### 业务约束

- [ ] 没有写单元测试
- [ ] 没有主动实现"本期不做"清单中的功能
- [ ] 关键日志含 callId / directiveId

### 异常处理

- [ ] 没有空 catch 块
- [ ] 没有 `catch(Exception e) { log.error(); }` 之后忘了处理
- [ ] 业务异常类继承 RuntimeException
- [ ] 不抛通用 Exception

### 设计

- [ ] 不发明设计模式
- [ ] 不预留过多扩展点
- [ ] 简单优于复杂

### 安全

- [ ] 没打印敏感信息（卡号、身份证、手机号原文）
- [ ] 没硬编码 IP / 密钥
- [ ] URL 走 UrlBuilder（不直接拼接）
