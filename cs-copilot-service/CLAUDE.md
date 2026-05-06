# Claude Code 项目指引（cs-copilot-service）

> 本文件是 Claude Code 处理本项目的入口。每次开始新对话时阅读本文件后再执行任务。

## 项目身份

- **项目名**：客服工作台 AI Copilot Service
- **业务**：基于 ASR + AI 意图识别，为信用卡客服坐席推荐功能入口
- **当前阶段**：MVP（Top 30 功能）详细设计 DD-V1.2
- **设计文档**：`docs/dd-v1.2.md`（每次实现新模块前必读对应章节）
- **任务卡片**：`docs/module-tasks.md`（M01-M17 各模块的实现指引）

---

## 关键技术约束（不要踩坑）

### Spring Boot 2.7 + JDK 8 — 这是项目的根本约束

| 项 | 必须 | 禁止 |
|---|------|------|
| Java 版本 | JDK 8 | JDK 9+ |
| Spring Boot | 2.7.18 | 3.x |
| 命名空间 | `javax.*` | `jakarta.*` |
| 语法限制 | 传统 switch / instanceof / class | record / sealed / switch 表达式 / instanceof pattern |

**常见错误示例**：

```java
// ❌ 错误：JDK 14+ record 语法
public record DialogMessage(String id, String content) {}

// ✅ 正确
@Data
@Builder
public class DialogMessage {
    private String id;
    private String content;
}

// ❌ 错误：JDK 14+ switch 表达式
return switch (type) { case A -> 1; default -> 0; };

// ✅ 正确
switch (type) {
    case A: return 1;
    default: return 0;
}

// ❌ 错误：jakarta.* 命名空间（Spring Boot 3.x）
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

// ✅ 正确：javax.* 命名空间（Spring Boot 2.x）
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
```

### 技术栈选型（不要替换）

| 项 | 必须用 | 不要用 |
|---|--------|--------|
| Redis 客户端 | **Jedis** + JedisPool | Lettuce |
| JSON 库 | **FastJSON 2.x**（`com.alibaba.fastjson2`） | Jackson、Gson |
| Kafka 客户端 | **Spring Kafka**（`spring-kafka`） | 原生 Kafka Client |
| HTTP 客户端 | **Spring Cloud OpenFeign** | RestTemplate、Apache HttpClient |
| 熔断库 | **Resilience4j** | Hystrix、Sentinel |
| 日志门面 | **SLF4J + Lombok @Slf4j** | 直接用 LoggerFactory |

**FastJSON 2.x 用法**：

```java
// ✅ 正确
import com.alibaba.fastjson2.JSON;
String json = JSON.toJSONString(obj);
DialogMessage msg = JSON.parseObject(json, DialogMessage.class);

// ❌ 错误（FastJSON 1.x 包名）
import com.alibaba.fastjson.JSON;
```

---

## 编码规范

| 项 | 规范 |
|---|------|
| 缩进 | 4 空格（不是 Tab，不是 2 空格） |
| 行宽 | 120 字符 |
| 文件头 | 不需要许可证注释 |
| 包名 | `com.cmbchina.cs.assitsvc.{layer}` |
| 接口命名 | `XxxService`（**不要** `IXxxService`） |
| 实现命名 | `XxxServiceImpl`（**不要** `DefaultXxxService`） |
| Lombok | 允许使用 |
| 字符编码 | UTF-8 |
| 日志注解 | 用 `@Slf4j`，不要手写 `LoggerFactory` |

### 包结构（必须遵循）

```
com.cmbchina.cs.assitsvc
├── CopilotApplication              # Spring Boot 入口
├── api                             # 接口层
│   ├── controller                  # REST Controller
│   └── dto                         # 接口 DTO
├── core                            # 核心业务编排
│   ├── intent                      # 意图识别相关
│   ├── match                       # 意图-功能匹配
│   ├── param                       # 参数解析
│   ├── directive                   # 指令构建
│   └── feedback                    # 反馈处理
├── asr                             # ASR 接入与处理
├── session                         # 通话会话管理
├── push                            # WebSocket 推送
├── config                          # 配置缓存
├── infra                           # 基础设施
│   ├── feign                       # Feign Client
│   ├── kafka                       # Kafka 配置
│   ├── redis                       # Redis 配置
│   └── metrics                     # 监控埋点
├── domain                          # 领域模型（POJO）
└── extension                       # 扩展点（接口 + NoOp 默认实现）
```

---

## 开发原则（重要）

### 不要做

- ❌ **不写单元测试**（用户明确决定不写，节省工时；`src/test` 目录暂时空着）
- ❌ **不执行 mvn 命令**（用户在本地构建）
- ❌ **不主动实现"本期不做"清单中的功能**（详见 `docs/dd-v1.2.md` 第 2.2 节）
- ❌ **不引入未声明的依赖**（要先在 `pom.xml` 声明）
- ❌ **不过度优化**（保持简单清晰，不发明设计模式）
- ❌ **不静默猜测**（详细设计中没明确的细节，先问用户）

### 要做

- ✅ **每个模块开发前先读 DD-V1.2 对应章节和任务卡片**
- ✅ **遵循"本期实现"清单**（M01-M17，参考 `docs/module-tasks.md`）
- ✅ **预留扩展点**：用接口 + `@ConditionalOnMissingBean` 默认空实现
- ✅ **写好 Javadoc**：类和公共方法都要有
- ✅ **关键日志含 callId / traceId / directiveId**
- ✅ **遵循 SonarQube 主流规则**：避免 Code Smell

---

## 模块开发顺序（不要按编号顺序）

DD-V1.2 中的模块编号 M01-M17 是**业务编号**，不是开发顺序。按"依赖+风险"排序：

| 阶段 | 内容 | 关键产出 |
|------|------|---------|
| **阶段 0** | 脚手架 | `pom.xml`, `application.yml`, `CopilotApplication.java` |
| **阶段 1** | 领域模型 | `domain/` 包下所有 POJO（DTO 类） |
| **阶段 2** | 基础设施 | Redis 配置、Feign 配置、Kafka 配置 |
| **阶段 3** | 工具类 | `StandardParamType`, `RuleEvaluator`, `UrlBuilder` |
| **阶段 4** | 单功能模块 | M03 历史 + M04 callSession + M05 意图树 |
| **阶段 5** | 核心链路 | M01 → M02 → M06 → M07 → M08 → M09 → M10 |
| **阶段 6** | 闭环模块 | M11 反馈 + M16 监控 + M17 配置一致性 |
| **阶段 7** | CLOB 扩展 | M12（涉及存量代码协作） |
| **阶段 8** | 配置后台 | M13（后端 + 前端） |

> **里程碑**：阶段 5 完成 = 端到端 Demo 可跑通，第一个可演示节点。

---

## 待实现清单（不要主动实现）

下面这些功能本期**不实现**，但代码结构要预留扩展点：

| 编号 | 功能 | 预留方式 |
|------|------|---------|
| F02 | ASR 实体抽取（正则） | `EntityExtractor` 接口 + `NoOpEntityExtractor` 默认实现 |
| F03 | LLM 兜底实体抽取 | 同上 |
| F04 | 多卡列表澄清 | `ClarificationStrategy` 接口 + `NoOpClarificationStrategy` |
| F05 | 数据脱敏框架 | 文档明示风险（DD-V1.2 第 2.3 节），代码不实现 |
| F07 | 完整灰度策略 | `GrayPolicy` 接口（本期已实现最简白名单 `OperatorWhitelistGrayPolicy`） |
| F13 | 服务端权限校验 | `PermissionChecker` 接口 + `AllowAllPermissionChecker` |

> 如果你看到我写的代码主动实现了这些功能，**请立即停下并告诉用户**，让用户决定。

---

## 协作流程

### 每次接到模块任务的标准流程

1. **读对应文档**
   - `docs/dd-v1.2.md` 中该模块的章节
   - `docs/module-tasks.md` 中的任务卡片
2. **列实现计划**（让用户确认）
   - 涉及的类（新增/修改）
   - 类签名和方法签名
   - 是否需要新增 pom 依赖
   - 是否需要新增配置项
   - 与详细设计的差异（如有）
3. **用户确认后开始写代码**
4. **交付清单**
   - 文件列表
   - 完整代码（**不省略**）
   - pom.xml / application.yml 变更
   - 风险点 / 待确认问题

### 不确定时的处理原则

- ✅ **优先选择简单方案**（不要发明设计模式）
- ✅ **明确告诉用户存在不确定**，给 2-3 个选项让用户选
- ❌ **不要静默猜测后实现**

例如：

- ✅ "DD-V1.2 中没有明确 Redis 连接池大小，我建议 maxTotal=20，需要确认吗？"
- ❌ 直接写 `maxTotal=50` 不告诉用户

---

## Git 工作流（GitLab Flow）

- **主分支**：`main`（受保护，不允许直接 push）
- **功能分支**：`feature/m{XX}-{short-name}`，例：`feature/m02-sentence-merger`
- **Hotfix 分支**：`hotfix/{issue-id}-{short-name}`
- **提交信息格式**：

```
[M02] 实现句间合并器

- 添加 SentenceMerger 类
- 添加 SentenceContinuityDetector 类
- 实现防抖 + 沉默 timer 机制

Refs: DD-V1.2 第 9 章
```

---

## 关键文档索引

| 文档 | 用途 | 何时读 |
|------|------|-------|
| `docs/dd-v1.2.md` | 详细设计稿 | 每次实现新模块前 |
| `docs/coding-standards.md` | 完整编码规范 | 不确定规范时 |
| `docs/module-tasks.md` | M01-M17 任务卡片 | 每次开始新模块时 |
| `docs/development-sop.md` | 开发流程 SOP | 项目初期一次性读 |
| `docs/claude-code-best-practices.md` | Claude 协作最佳实践 | 用户参考 |
| `docs/prompt-templates/` | Prompt 模板 | 用户参考 |
| `pom.xml` | Maven 依赖清单 | 引入新依赖时 |
| `src/main/resources/application.yml` | 应用配置 | 添加配置项时 |
| `src/main/resources/intent-tree.json` | 意图树配置 | 修改意图树时 |

---

## 当前进度跟踪

> 实施过程中维护，每完成一个模块勾选

- [ ] 阶段 0：脚手架完成
- [ ] 阶段 1：领域模型完成
- [ ] 阶段 2：基础设施完成
- [ ] 阶段 3：工具类完成
- [ ] 阶段 4：单功能模块完成
- [ ] 阶段 5：核心链路完成（**端到端 Demo 里程碑**）
- [ ] 阶段 6：闭环模块完成
- [ ] 阶段 7：CLOB 扩展完成
- [ ] 阶段 8：配置后台完成
