# Claude Code 协作最佳实践

> 本文档面向**用户**（你），提供与 Claude Code 高效协作的方法。

## 核心原则

### 原则 1：先让 Claude 列计划，再让它写代码

Claude 一上来就刷一大堆代码 → 难以审查、修改成本高。

**推荐做法**：

```
你：参考 DD-V1.2 第 9 章，实现 M02 句间合并器。

Claude：
我打算实现以下类：
1. SentenceMerger（com.cmbchina.cs.assitsvc.asr 包）
   - 字段：debounceTimers / silenceTimers
   - 方法：handleSentence / cleanup
2. SentenceContinuityDetector（同包）
   - 词典硬编码
   - 方法：detect

是否需要新增 pom 依赖？不需要。
是否需要新增配置项？需要 ScheduledExecutorService Bean。

确认后我开始写代码。

你：确认。

Claude：[开始写代码]
```

### 原则 2：每次对话只关注一个模块

不要让 Claude 一次实现 5 个模块。原因：

- Context 不够（DD-V1.2 全文 4500 行已经吃掉很多）
- 难以验证质量
- 出错后修复链长

**推荐做法**：一个模块一次对话，完成后开新对话做下一个。

### 原则 3：用 @ 引用文件，不要让 Claude 凭记忆

```
✅ 推荐
@CLAUDE.md @docs/module-tasks.md @docs/dd-v1.2.md
请实现 Task M02

❌ 不推荐
请实现 M02 句间合并器（Claude 可能记不准 DD-V1.2 细节）
```

### 原则 4：每次都让 Claude 先读 CLAUDE.md

`CLAUDE.md` 是入口，含项目身份、约束、规范。Claude 读了之后才不会犯低级错误（用 jakarta.* 命名空间、写单测等）。

## 提示词模板

### 模板 1：开始新模块

```
@CLAUDE.md @docs/module-tasks.md @docs/dd-v1.2.md

请帮我实现 [模块编号] [模块名]。

要求：
1. 严格按 docs/module-tasks.md 中 [Task XX] 的清单
2. 先列实现计划（涉及的类、方法签名、依赖、配置）
3. 我确认后再写代码
4. 不要写单元测试
5. 不要主动实现"本期不做"清单中的功能
```

### 模板 2：调试问题

```
@CLAUDE.md

我在 [模块名] 中遇到问题：
[贴出错误日志或代码片段]

请帮我分析原因，并提供修复方案。
不要直接改代码，先告诉我问题在哪、怎么修。
```

### 模板 3：重构

```
@CLAUDE.md @docs/coding-standards.md

请帮我 review 这段代码并重构：
[贴代码]

要求：
1. 保持业务逻辑不变
2. 符合 docs/coding-standards.md
3. 列出修改点和原因
```

### 模板 4：Code Review

```
@CLAUDE.md @docs/coding-standards.md

请帮我 review 这个 PR 中的代码：
[贴代码或文件]

关注以下方面：
1. 是否符合 Spring Boot 2.7 + JDK 8 约束
2. 是否符合编码规范
3. 是否有过度设计
4. 是否符合 DD-V1.2 设计意图
```

### 模板 5：跨模块设计讨论

```
@CLAUDE.md @docs/dd-v1.2.md

我在思考 [设计问题]，比如 [具体场景]。

DD-V1.2 中 [章节] 是这么设计的，但我有以下顾虑：
- 顾虑 1：...
- 顾虑 2：...

请帮我分析：
1. DD-V1.2 的设计是否合理
2. 我的顾虑是否成立
3. 如果需要调整，有哪些选项

不要直接给方案，先帮我理清思路。
```

## 常见误区

### 误区 1：让 Claude 自己决定一切

❌ "Claude，你看着实现 M02 吧"

✅ "Claude，按 Task M02 清单实现，先列计划"

### 误区 2：CLAUDE.md 越详细越好

CLAUDE.md 每次对话都加载，太长占 context。**保持 ≤ 200 行**，详细规范放独立文档。

### 误区 3：不验证 Claude 的输出

Claude 偶尔会：
- 用 jakarta.* 命名空间（Spring Boot 3.x 习惯）
- 主动写单元测试
- 实现"本期不做"清单中的功能

每次拿到代码后**至少检查这三点**。

### 误区 4：在一个对话里反复改同一个模块

如果一个对话里改了 5-10 次代码，**开新对话**，把最终代码 paste 进去再继续。
原因：Claude 容易在长对话中"漂移"，不再严格遵守 CLAUDE.md。

## 高效工作流（推荐）

### 单模块开发循环

```
1. 开新对话，引用 @CLAUDE.md @docs/module-tasks.md
2. 让 Claude 列计划
3. 你 review 计划，确认或调整
4. 让 Claude 写代码
5. 复制到 IDE，本地编译
6. 编译错误 → 截图发给 Claude 修
7. 编译通过 → 提交到 feature 分支
8. 关闭对话（避免漂移）
```

### 多模块依赖时

```
1. 先做被依赖的模块（如 domain POJO）
2. paste 到 CLAUDE.md 末尾或单独的 docs/current-progress.md
3. 下个模块的 Claude 对话能看到这些已有代码
```

### 调试模式

```
1. 开新对话
2. paste 错误日志 + 相关代码
3. 让 Claude 分析（不要直接让改）
4. 你确认根本原因后，再让 Claude 写修复
```

## Claude Code 限制

| 限制 | 应对 |
|------|------|
| 每次对话 context 有限 | 一个对话只做一个模块 |
| 不能直接执行 mvn | 你在本地构建，错误截图给 Claude |
| 可能用错语法（jakarta vs javax） | CLAUDE.md 强调 + 你 review |
| 长对话会"漂移" | 完成后开新对话 |
| 对详细设计的细节记不准 | 用 @ 引用文档 |

## 迭代过程中维护 CLAUDE.md

随着项目进展，可能需要更新 `CLAUDE.md`：

- 新增重要约定 → 加到 CLAUDE.md
- 完成某个阶段 → 勾选"当前进度跟踪"
- 发现 Claude 反复犯的错 → 加到 CLAUDE.md 的"不要做"清单

但保持 CLAUDE.md ≤ 200 行的总原则。

## 推荐的 Claude Code 设置

```bash
# 项目根目录创建 .claude/ 目录
mkdir -p .claude

# 设置 .claude/settings.json（如果 Claude Code 支持）
cat > .claude/settings.json <<'EOF'
{
  "defaultModel": "claude-opus-4-7",
  "maxTokensPerResponse": 4000,
  "autoReadFiles": ["CLAUDE.md"]
}
EOF
```

## 其他建议

- **保留对话历史**：复杂模块的对话存档，作为后续审计参考
- **不要怕开新对话**：每个模块一次对话，效率最高
- **遇到难题先描述清楚**：给 Claude 完整上下文（错误日志 + 相关代码 + 期望行为）
- **批判性使用 Claude**：Claude 输出仅参考，最终代码必须你自己 review
