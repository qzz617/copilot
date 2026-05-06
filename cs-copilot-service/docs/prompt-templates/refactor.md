# Prompt 模板：代码重构

> 用于 Claude Code 协助重构代码。

## 模板（推荐使用）

```
@CLAUDE.md @docs/coding-standards.md

请帮我 review 并重构以下代码：

【文件】
@{{文件路径}}

【重构目标】
- {{目标 1，如：拆分长方法}}
- {{目标 2，如：消除重复代码}}

【约束】
1. 业务逻辑不变
2. 符合 coding-standards.md
3. 不引入新依赖
4. 不写单元测试

【请你】
1. 列出当前代码的问题（按优先级）
2. 给出重构方案（不直接改代码）
3. 我确认后开始改

【提醒】
- Spring Boot 2.7 + JDK 8（不要用 record / switch 表达式 / pattern matching）
- 用 Lombok 而不是手写 getter/setter
```

## 示例 1：长方法拆分

```
@CLAUDE.md @docs/coding-standards.md

请帮我 review 并重构 M09 DirectiveBuilderServiceImpl。

【文件】
@src/main/java/com/cmbchina/cs/assitsvc/core/directive/DirectiveBuilderServiceImpl.java

【重构目标】
- build() 方法 80 行太长，拆分
- 派生 actionType 的逻辑应该独立成方法
- 风险等级派生应该独立成方法

【约束】
1. 业务逻辑不变
2. 符合 coding-standards.md
3. SonarQube 圈复杂度 ≤ 15

【请你】
1. 列出当前代码问题
2. 给出重构方案
3. 我确认后改
```

## 示例 2：消除重复代码

```
@CLAUDE.md @docs/coding-standards.md

我注意到 M03 DialogHistoryManagerImpl 和 M04 CallSessionManagerImpl 有重复的 Redis 操作模板。

【文件】
@src/main/java/com/cmbchina/cs/assitsvc/asr/DialogHistoryManagerImpl.java
@src/main/java/com/cmbchina/cs/assitsvc/session/CallSessionManagerImpl.java

【重复点】
- key 拼接逻辑
- 异常处理逻辑
- TTL 设置

【请你】
1. 评估是否值得抽象（不要为抽象而抽象）
2. 如果值得，给出抽象方案
3. 不直接改代码

【约束】
- 不引入新设计模式（如 Template Method）除非必要
- 不要让代码变得难懂
```

## 示例 3：包结构调整

```
@CLAUDE.md @docs/coding-standards.md

我发现一些类放错了包。

【现状】
@src/main/java/com/cmbchina/cs/assitsvc/api/util/UrlBuilder.java

【应该在】
core.directive 包

【请你】
1. 列出所有放错位置的类
2. 给出调整方案（含原位置 / 新位置 / 影响范围）
3. 我确认后改

【约束】
- 用 IDE 重构功能（不要手动改 import）
- 一次只调整一个包，便于验证
```

## 重构原则

### 1. 不要为重构而重构

只在以下情况重构：
- ✅ 长方法（> 100 行）
- ✅ 重复代码出现 ≥ 3 次
- ✅ 不符合 coding-standards
- ✅ 准备增加新功能但当前结构不支持

避免：
- ❌ 觉得"看起来不优雅"
- ❌ 想用某个设计模式
- ❌ 没有具体收益

### 2. 不要一次改太多

每次重构只关注一个目标。否则：
- 难以验证业务逻辑不变
- Code Review 困难
- 出问题难定位

### 3. 重构前先有验证手段

理想情况：有单元测试覆盖。
但本项目不写单测，所以重构前确保有集成测试或手工验证场景。

### 4. 谨慎抽象

✅ 推荐抽象的：
- 重复出现 3 次以上的代码
- 跨模块的通用逻辑

❌ 不推荐抽象的：
- 看起来"可能"会重用的代码
- 为引入设计模式而抽象
- 增加理解成本但不增加收益的抽象
