# Prompt 模板：开发新模块

> 复制以下模板，替换 `{{...}}` 占位符后发给 Claude Code。

## 模板（推荐使用）

```
@CLAUDE.md @docs/module-tasks.md @docs/dd-v1.2.md

请帮我实现 {{模块编号}} {{模块名}}。

【步骤 1】先列实现计划：
- 涉及的类（新增 / 修改）
- 类签名和关键方法签名
- 是否需要新增 pom 依赖
- 是否需要新增配置项
- 与详细设计文档的差异（如有）
- 不确定的地方（如有）

【步骤 2】我确认后再写代码

【约束提醒】
- Spring Boot 2.7 + JDK 8（用 javax.* 不是 jakarta.*）
- 不写单元测试
- 不主动实现"本期不做"清单中的功能
- 用 FastJSON 2.x（com.alibaba.fastjson2）
- 用 Jedis（不是 Lettuce）
- 缩进 4 空格，行宽 120
- 包名 com.cmbchina.cs.assitsvc
- 接口 XxxService，实现 XxxServiceImpl
- 用 Lombok @Data + @Builder + @Slf4j
```

## 示例 1：实现 M02 句间合并器

```
@CLAUDE.md @docs/module-tasks.md @docs/dd-v1.2.md

请帮我实现 M02 句间合并器。

【步骤 1】先列实现计划：
- 涉及的类（新增 / 修改）
- 类签名和关键方法签名
- 是否需要新增 pom 依赖
- 是否需要新增配置项
- 与 DD-V1.2 第 9 章的差异（如有）
- 不确定的地方（如有）

【步骤 2】我确认后再写代码

【约束提醒】
（同上模板）
```

## 示例 2：实现领域模型 POJO

```
@CLAUDE.md @docs/module-tasks.md @docs/dd-v1.2.md

请帮我实现 Task L1-1：领域模型 POJO。

要求：
1. 按 docs/module-tasks.md 中 Task L1-1 的清单实现
2. 字段类型与 DD-V1.2 一致
3. 用 Lombok @Data + @Builder
4. 不需要业务方法

请直接生成代码（POJO 不需要列计划）。
最后告诉我哪些文件创建了。

【约束提醒】
（同上模板）
```

## 示例 3：实现 M11 反馈接口（含 P0-6 修订）

```
@CLAUDE.md @docs/module-tasks.md @docs/dd-v1.2.md

请帮我实现 M11 反馈接口（DD-V1.2 P0-6 关键修订）。

【特别注意】DD-V1.2 第 17 章 + 第 25 章中的 P0-6 修订：
- 通过 directive_id 反查 trigger_log
- 校验 expireAt + callId + operatorId + intentCode + itemId 与原指令一致
- is_effective 字段实现幂等（首次 Y，后续 N）
- feedback_log.trigger_log_id 后端反查填充

【步骤 1】先列实现计划：
- Controller 类
- Service 类（接口 + 实现）
- DAO 类
- Request/Response DTO
- 是否新增依赖

【步骤 2】我确认后写代码

【约束提醒】
（同上模板）
```

## 错误示例

❌ **不好的提示**：

```
帮我写个意图识别接口
```

问题：
- 没引用文档
- 不知道是哪个模块
- 不知道用哪个 API
- 不约束规范

❌ **不好的提示**：

```
@docs/dd-v1.2.md

实现整个 Copilot Service
```

问题：
- 范围太大
- Claude 会输出几千行代码
- 难以审查

✅ **好的提示**：见上面的模板。
