# Prompt 模板：调试问题

> 用于 Claude Code 协助调试代码问题。

## 模板（推荐使用）

```
@CLAUDE.md

我在 {{模块名}} 中遇到问题。

【现象】
{{描述异常表现}}

【复现步骤】
1. ...
2. ...

【相关代码】
{{贴代码片段}}

【错误日志】
{{贴日志}}

【我已尝试】
{{说明已经做过的排查}}

【请帮我】
1. 分析问题根因（不要直接改代码）
2. 给出 2-3 个可能的修复方案
3. 推荐其中最合适的方案，说明理由

我确认方案后再让你改代码。
```

## 示例 1：编译错误

```
@CLAUDE.md

我在 M02 SentenceMerger 中遇到编译错误。

【现象】
mvn compile 失败

【相关代码】
@src/main/java/com/cmbchina/cs/assitsvc/asr/SentenceMergerImpl.java

【错误日志】
[ERROR] /src/main/java/com/cmbchina/cs/assitsvc/asr/SentenceMergerImpl.java:[35,16] cannot find symbol
  symbol:   method orElse(java.lang.Long)
  location: class java.util.Optional<java.lang.Long>

【我已尝试】
排查了 import，但找不到原因

【请帮我】
1. 分析问题根因
2. 给出修复方案
3. 不要直接改代码
```

## 示例 2：运行时异常

```
@CLAUDE.md

我在测试 M06 IntentRecognitionServiceImpl 时遇到运行时异常。

【现象】
调用 recognize() 方法时抛 NullPointerException

【相关代码】
@src/main/java/com/cmbchina/cs/assitsvc/core/intent/IntentRecognitionServiceImpl.java

【错误日志】
java.lang.NullPointerException: Cannot invoke "java.util.List.stream()" because "fullHistory" is null
    at IntentRecognitionServiceImpl.recognize(IntentRecognitionServiceImpl.java:42)

【复现步骤】
1. 启动应用
2. 发送 ASR 消息到 Kafka
3. 触发 recognize 时报错

【我已尝试】
检查了 DialogHistoryManager.getHistory，确实有可能返回 null

【请帮我】
1. 分析根因（getHistory 是返回 null 还是空 List？）
2. 给出修复方案
3. 这种空值处理是统一在 getHistory 内做，还是在调用方做？给我建议
```

## 示例 3：测试场景设计

```
@CLAUDE.md

我要本地集成测试 M07 IntentFunctionMatcher，需要怎么准备？

【背景】
M07 已实现完成，需要验证：
1. 单 intentCode 多候选按 priority 倒序
2. condition_rule 命中/未命中
3. 灰度白名单过滤

【已有】
- 应用能启动
- Redis 已连接
- WireMock 模拟 AI 接口

【请帮我】
1. 给出 3 个测试场景（不写代码）
2. 每个场景需要的 Mock 数据
3. 验证点

我确认后让你写测试脚本。
```

## 调试技巧

### 1. 错误日志要完整

❌ 不好：
```
报错了，请帮我看看
```

✅ 好：
```
完整错误堆栈：
[贴完整堆栈，包括 caused by]

发生时间点：调用 recognize() 之后
```

### 2. 给 Claude 完整上下文

❌ 不好：贴几行代码就让 Claude 改

✅ 好：
- 用 @ 引用整个文件
- 说明前置条件
- 说明期望行为

### 3. 不要直接让 Claude 改代码

先让 Claude 分析根因和方案，你确认后再改。

否则 Claude 可能：
- 改错位置
- 引入新 bug
- 过度修改

### 4. 复杂问题分步处理

如果问题涉及多个模块，分步：

```
第 1 步：先排查 M03 的数据是否正确
第 2 步：再排查 M06 的过滤逻辑
第 3 步：最后排查 AI 接口响应
```
