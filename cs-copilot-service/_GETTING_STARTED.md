# 项目启动总指引

> 本文件是你（项目负责人）的入门第一步。从这里开始按顺序看。

## 这套资产里有什么

```
cs-copilot-service/                        ← 这就是你的项目根目录
│
├── 📘 _GETTING_STARTED.md                 ← 你正在看这个文件
├── 📘 CLAUDE.md                           ← Claude Code 入口（最重要）
├── 📗 README.md                           ← 团队成员看
├── 📦 pom.xml                             ← Maven 配置
├── 🔒 .gitignore
│
├── 📁 docs/
│   ├── 📄 dd-v1.2.md                     ← 详细设计稿（4500+ 行）
│   ├── 📄 coding-standards.md             ← 编码规范
│   ├── 📄 module-tasks.md                 ← M01-M17 任务卡片（最常用）
│   ├── 📄 development-sop.md              ← 开发流程 SOP
│   ├── 📄 claude-code-best-practices.md   ← Claude 协作技巧
│   └── 📁 prompt-templates/
│       ├── new-module.md                  ← 新模块 prompt 模板
│       ├── debug.md                       ← 调试 prompt 模板
│       ├── refactor.md                    ← 重构 prompt 模板
│       └── code-review.md                 ← review prompt 模板
│
├── 📁 sql/
│   └── V1__init.sql                       ← 数据库初始化（4 张新表 DDL）
│
└── 📁 src/main/
    ├── java/com/cmbchina/cs/assitsvc/
    │   └── CopilotApplication.java         ← Spring Boot 入口
    └── resources/
        ├── application.yml                 ← 主配置
        ├── application-dev.yml             ← 开发环境
        ├── application-uat.yml             ← UAT 环境
        ├── application-prod.yml            ← 生产环境
        ├── intent-tree.json                ← 意图树配置
        └── logback-spring.xml              ← 日志配置
```

## 第 1 步：确认资产可用（约 30 分钟）

### 1.1 解压 / 复制到本地工作目录

```bash
unzip cs-copilot-service-template.zip -d ~/workspace/
cd ~/workspace/cs-copilot-service
```

### 1.2 创建 GitLab 项目

```bash
# 在 GitLab 上新建 cs-copilot-service 项目
# 然后：
git init
git add .
git commit -m "chore: 项目初始化（来自 DD-V1.2 模板）"
git remote add origin <your-gitlab-url>
git branch -M main
git push -u origin main

# 设置分支保护
# GitLab 中 main 分支设置为受保护，禁止直接 push
```

### 1.3 验证本地能编译（不依赖外部资源）

```bash
mvn clean compile
```

应该看到 `BUILD SUCCESS`。

如果失败，常见原因：

- 内部 Maven 仓库未配置（行外环境忽略）
- Spring Cloud 版本不兼容（确认 spring-cloud 2021.0.9 + Spring Boot 2.7.18）
- JDK 版本（确认 JDK 8）

## 第 2 步：理解开发流程（约 1 小时）

按以下顺序阅读：

1. **`CLAUDE.md`**（10 分钟）— 必读，理解项目身份和约束
2. **`docs/development-sop.md`**（20 分钟）— 理解整体流程
3. **`docs/module-tasks.md`**（30 分钟）— 理解每个模块的范围
4. **`docs/claude-code-best-practices.md`**（15 分钟）— 学习如何用好 Claude Code

不需要一次读完 `dd-v1.2.md`（4500+ 行），按需查阅即可。

## 第 3 步：开始第一个模块（约 半天）

按 `development-sop.md` 中"阶段 1：领域模型"开始。

### 3.1 创建 feature 分支

```bash
git checkout -b feature/l1-domain-models
```

### 3.2 打开 Claude Code，使用 prompt 模板

参考 `docs/prompt-templates/new-module.md`，发以下消息给 Claude Code：

```
@CLAUDE.md @docs/module-tasks.md @docs/dd-v1.2.md

请帮我实现 Task L1-1：领域模型 POJO

要求：
1. 按 docs/module-tasks.md 中 Task L1-1 的清单实现
2. 字段类型与 DD-V1.2 一致
3. 用 Lombok @Data + @Builder
4. 不需要业务方法

请直接生成代码（POJO 不需要列计划）。
最后告诉我哪些文件创建了。
```

### 3.3 review Claude 输出

按 `docs/prompt-templates/code-review.md` 中的检查清单 review。

### 3.4 本地验证

```bash
mvn compile
```

如果失败，截图发给 Claude（用 debug.md 模板）。

### 3.5 提交

```bash
git add src/main/java/com/cmbchina/cs/assitsvc/domain/
git commit -m "[L1] 添加领域模型 POJO

- DialogMessage / IntentResult / DirectiveDTO 等
- 使用 Lombok @Data + @Builder

Refs: DD-V1.2 全文涉及"
git push origin feature/l1-domain-models
# GitLab 创建 MR
```

### 3.6 合并 + 进入下一个模块

MR review 通过后合到 main，然后进入下一个 Task。

## 第 4 步：建立工作节奏

### 推荐节奏

```
每天：
  - 完成 1-2 个模块（小模块）或 0.5 个模块（大模块）
  - 每个模块开新 Claude 对话
  - 每个模块单独提 MR

每周：
  - 周一：planning（确认本周做哪些模块）
  - 周三：mid-check（确认进度）
  - 周五：retrospective（review 当周代码 + 调整下周计划）
```

### 节奏控制点

按 `docs/module-tasks.md` 中的阶段，每完成一个阶段做一次"对齐"：

- 阶段 1 完成 → review 领域模型设计是否需要调整
- 阶段 2 完成 → 验证基础设施能用（Redis 通、Feign 通、Kafka 通）
- 阶段 3 完成 → 工具类被几个模块用过没？API 是否好用？
- **阶段 5 完成 → 端到端 Demo（项目第一个里程碑）★**
- 阶段 6 完成 → 反馈闭环、监控落地
- 阶段 7 完成 → CLOB 扩展（与存量团队协作）
- 阶段 8 完成 → 配置后台
- 联调 → 灰度 → 全量

## 关键提醒

### 1. 不要让 Claude 一次写太多

✅ 一次对话只做一个模块
❌ "Claude，把整个 Copilot Service 写完"

### 2. 每个模块都要 Code Review

Claude 生成的代码不是开箱即用的"黄金代码"，每次都要 review：
- 是否用了 jakarta.* 命名空间（错误的）
- 是否主动写了单元测试（不需要）
- 是否实现了"本期不做"功能（错误的）

详见 `docs/prompt-templates/code-review.md`。

### 3. 遇到设计问题先回 DD-V1.2

如果某个细节 Claude 处理得和你预期不一样：

1. 先查 DD-V1.2 是否明确写了
2. 写了 → 让 Claude 按文档来
3. 没写 → 你做决定，更新 DD-V1.2，再让 Claude 按新版本来

### 4. CLAUDE.md 是活文档

随着项目进展，更新 CLAUDE.md：
- 新增重要约定 → 加进去
- 完成阶段 → 勾选"当前进度跟踪"
- 发现 Claude 反复犯错 → 加进"不要做"清单

但保持 ≤ 200 行的总原则。

## 常见问题

### Q1：DD-V1.2 太长，要不要让 Claude 全部读？

不需要。每次让 Claude 用 `@docs/dd-v1.2.md` 引用，Claude 会自己定位到相关章节。

### Q2：要不要让 Claude 写单元测试？

不要。本期已明确不写单测。如果 Claude 自己加了，删掉。

### Q3：Claude 写的代码用了 record 怎么办？

立即指出"项目用 JDK 8，不能用 record，请改成 @Data + @Builder"。

### Q4：模块编号 M01-M17 的开发顺序是什么？

不是按编号顺序！按 `docs/module-tasks.md` 中的"阶段 1-8"顺序来。

### Q5：阶段 5 是什么里程碑？

阶段 5 完成 = 端到端 Demo 可演示。这是项目最重要的中间里程碑：
- ASR 消息进来 → AI 识别意图 → 匹配功能 → 推送指令 → 前端展示

到此项目从"代码"变成"可演示的产品"。

### Q6：如何与存量团队协作（M12 阶段）？

阶段 7 是和存量发布团队协作的，不是单方能完成。提前 1-2 周和他们对齐：
- 现有发布代码在哪
- 改动方案
- 测试方案

### Q7：如果 DD-V1.2 中描述模糊怎么办？

Claude 会问你（CLAUDE.md 中已规定）。你做决定，并把决定补回 DD-V1.2 或 CLAUDE.md。

## 现在你应该做什么

1. 阅读 `CLAUDE.md`（10 分钟）
2. 阅读 `docs/development-sop.md`（20 分钟）
3. 浏览 `docs/module-tasks.md`（30 分钟）
4. 把项目推到 GitLab，验证 `mvn compile` 通过
5. 创建第一个 feature 分支：`feature/l1-domain-models`
6. 用 Claude Code 实现 Task L1-1
7. 提交、review、合并
8. 进入下一个 Task

祝顺利！
