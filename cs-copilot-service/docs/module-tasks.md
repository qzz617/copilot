# 模块开发任务卡片

> 本文档为 M01-M17 各模块的开发任务卡。每开始新模块前读对应卡片。

## 任务卡片格式

每张卡片包含：

```
- 输入：DD-V1.2 章节、依赖模块
- 实现范围：要实现的类
- 关键约束：DD-V1.2 中的关键设计点
- 验收标准：完成判定
- 不要做：明确不实现的部分
```

---

## 阶段 0：脚手架（已就绪）

✅ 项目初始化已完成，包含：
- `pom.xml`（Spring Boot 2.7.18 + JDK 8）
- `application.yml` + 多环境配置
- `CopilotApplication.java`（含 @EnableFeignClients、@EnableKafka、@EnableScheduling）
- `intent-tree.json` 示例
- `V1__init.sql` DDL

---

## 阶段 1：领域模型（建议第一批做）

不依赖任何业务逻辑，可独立完成。

### Task L1-1：Domain POJO

**输入**：DD-V1.2 各章节中提到的所有 DTO/POJO

**实现范围**：
```
com.cmbchina.cs.assitsvc.domain
├── AsrSentenceEvent           # ASR 事件
├── DialogMessage              # 对话消息
├── CallSession                # 通话会话
├── IntentResult               # 意图识别结果
├── IntentTreeNode             # 意图树节点
├── ItemFullConfig             # 功能完整配置（含 CopilotExt）
├── CopilotExt                 # Copilot 扩展配置
├── IntentMapping              # 意图映射
├── ItemReference              # 功能引用（itemId + priority）
├── ItemParam                  # 功能参数
├── ItemCandidate              # 候选项
├── ConditionRule              # 条件规则
├── ConditionItem              # 条件项
├── EvaluationContext          # 评估上下文
├── ParamContext               # 参数上下文
├── BuildContext               # 指令构建上下文
├── ExecutedStep               # 已执行步骤
├── DirectiveDTO               # 推送指令
├── IntentInfo                 # 指令-意图信息
├── FunctionInfo               # 指令-功能信息
├── DisplayInfo                # 指令-展示信息
├── ActionInfo                 # 指令-动作信息
├── RiskInfo                   # 指令-风险信息
├── FeedbackRequest            # 反馈请求
├── MenuVersionData            # CLOB 数据
└── CopilotIndex               # CLOB 反向索引
```

**关键约束**：
- 都是 POJO，使用 `@Data + @Builder`
- 不含业务逻辑
- 字段类型与 DD-V1.2 章节描述一致
- 使用 `@JSONField(name = "xxx")` 适配 JSON 字段命名（如有）

**验收标准**：
- [ ] 所有 POJO 创建完成
- [ ] 字段类型与 DD-V1.2 一致
- [ ] 使用 Lombok 注解
- [ ] 无业务方法

**不要做**：
- 不要在 POJO 中加业务方法
- 不要加 JPA 注解（本期不用 ORM）

---

## 阶段 2：基础设施

### Task I-1：Redis 配置

**输入**：DD-V1.2 第 33 章 Spring 配置项

**实现范围**：
```
com.cmbchina.cs.assitsvc.infra.redis
└── RedisConfig                # JedisConnectionFactory + RedisTemplate
```

**关键约束**：
- 用 `JedisConnectionFactory`（不是 `LettuceConnectionFactory`）
- 配置 `JedisPool` 参数
- `RedisTemplate<String, String>` 用 StringRedisSerializer
- pom.xml 已排除 Lettuce

**验收标准**：
- [ ] 启动应用时连接 Redis 成功
- [ ] 用 `redis-cli` 能看到 `RedisTemplate` 写入的 key

### Task I-2：AI Feign Client 配置

**输入**：DD-V1.2 第 13 章

**实现范围**：
```
com.cmbchina.cs.assitsvc.infra.feign
├── AiFeignConfig              # Feign 配置（超时、Logger、ErrorDecoder）
├── AiIntentFeignClient        # @FeignClient 接口
├── AiErrorDecoder             # 错误解析
├── IntentRecognitionRequest   # 请求 DTO
└── IntentRecognitionResponse  # 响应 DTO
```

**关键约束**：
- `@FeignClient(name = "ai-intent-client", url = "${copilot.ai.url}")`
- 配合 Resilience4j 熔断（在调用层加 `@CircuitBreaker`，不在 Feign 配置里）

**验收标准**：
- [ ] 用 WireMock 模拟 AI 接口能调通
- [ ] 超时配置生效

### Task I-3：Kafka Consumer 配置

**输入**：DD-V1.2 第 8 章

**实现范围**：
```
com.cmbchina.cs.assitsvc.infra.kafka
└── KafkaConsumerConfig        # ConcurrentKafkaListenerContainerFactory
```

**关键约束**：
- `enable-auto-commit: false`，手动 ack
- 按 callId 分区（与 ASR 团队确认 key 契约）

**验收标准**：
- [ ] 能消费 Kafka 消息
- [ ] 多 Pod 时同 callId 路由到同一 Pod

### Task I-4：业务监控埋点

**输入**：DD-V1.2 第 18 章 + 第 35 章

**实现范围**：
```
com.cmbchina.cs.assitsvc.infra.metrics
├── MetricsService             # 指标统计
└── ReasonCodeConstants        # 原因码枚举
```

**关键约束**：
- 不做完整看板，只做埋点
- 业务面：触发日志、反馈日志（落库）
- 不做接口监控（行内已有）

---

## 阶段 3：工具类

### Task U-1：StandardParamType 枚举

**输入**：DD-V1.2 第 15 章

**实现范围**：
```
com.cmbchina.cs.assitsvc.core.param
├── StandardParamType          # 枚举（13 个标准参数 + COOKIE_PLACEHOLDER）
└── ParamSourceType            # 枚举（SESSION/CALL_META/LITERAL/COOKIE）
```

**关键约束**：
- 14 个枚举值（含 DD-V1.1 新增 COOKIE_PLACEHOLDER）
- 每个枚举含 displayName / sourceType / defaultSourceKey / sensitive

**验收标准**：
- [ ] 枚举值与 DD-V1.2 第 15.1 节一致

### Task U-2：RuleEvaluator

**输入**：DD-V1.2 第 14.3 节

**实现范围**：
```
com.cmbchina.cs.assitsvc.core.match
├── RuleEvaluator              # 接口
└── JsonRuleEvaluatorImpl      # 实现
```

**关键约束**：
- 支持 `eq/not_eq/in/not_in/exists/not_exists/gt/gte/lt/lte`
- DD-V1.2 P1-19：异常默认返回 false
- 支持 `all`（AND）和 `any`（OR）组合

**验收标准**：
- [ ] 所有运算符正确
- [ ] 异常时返回 false

### Task U-3：UrlBuilder（DD-V1.2 增强）

**输入**：DD-V1.2 第 16.4 节

**实现范围**：
```
com.cmbchina.cs.assitsvc.core.directive
├── UrlBuilder                 # 接口
└── UrlBuilderImpl             # 实现
```

**关键约束**：
- DD-V1.2 P0-5：协议必须 https
- DD-V1.2 P0-5：生产环境拦截 UAT 域名
- DD-V1.2 P0-5：同名参数冲突策略（OVERRIDE/PRESERVE/ERROR）
- DD-V1.2 P1-12：修复尾部分隔符 bug
- `${COOKIE.xxx}` 占位符不做 URL 编码

**验收标准**：
- [ ] 所有空参数被过滤
- [ ] 不会出现尾部 `?`/`&`
- [ ] Cookie 占位符保留原样

---

## 阶段 4：单功能模块

### Task M03：对话历史管理

**输入**：DD-V1.2 第 10 章

**实现范围**：
```
com.cmbchina.cs.assitsvc.asr
├── DialogHistoryManager       # 接口
└── DialogHistoryManagerImpl   # Redis List 实现
```

**关键约束**：
- 全量保存（客户 + 坐席）
- Redis List，`copilot:history:{callId}`
- 最大 50 条，TTL 1 小时
- 过滤逻辑不在这里做（在 M06）

**验收标准**：
- [ ] append 后能 getHistory 取出
- [ ] 超过 50 条自动 trim
- [ ] cleanup 删除 key

### Task M04：callId-operatorId 绑定

**输入**：DD-V1.2 第 11 章

**实现范围**：
```
com.cmbchina.cs.assitsvc.session
├── CallSessionManager         # 接口
└── CallSessionManagerImpl     # Redis Hash 实现
```

**关键约束**：
- Redis Hash，`copilot:call_session:{callId}`
- TTL 通话结束 + 30 分钟
- DD-V1.2 P0-8：缺失时由调用方 fail closed（这里只返回 null）
- DD-V1.2 P2-4：用 HashMap 逐项 put（不用 Map.of，因为可能有 null）

### Task M05：意图树加载器

**输入**：DD-V1.2 第 12 章

**实现范围**：
```
com.cmbchina.cs.assitsvc.core.intent
├── IntentTreeLoader           # 接口
└── IntentTreeLoaderImpl       # 配置文件加载
```

**关键约束**：
- 启动时 `@PostConstruct` 加载 `classpath:intent-tree.json`
- 提供 `reload()` 方法供 Admin 接口调用（DD-V1.2 P1-7）
- volatile 保证可见性

**验收标准**：
- [ ] 启动时加载成功
- [ ] 调用 reload 后能切换到新内容

---

## 阶段 5：核心链路（端到端 Demo 里程碑）

完成阶段 5 后，能跑通"ASR → 意图识别 → 推荐推送"的完整链路。

### Task M01：ASR 事件接入器（DD-V1.2 P0-7 关键修订）

**输入**：DD-V1.2 第 8 章

**实现范围**：
```
com.cmbchina.cs.assitsvc.asr
├── AsrSentenceConsumer        # @KafkaListener
├── SentenceDedupService       # sentenceId 去重
└── AsrSentenceEventParser     # 反序列化
```

**关键约束**：
- DD-V1.2 P0-7：先存历史再做触发判断（顺序重要）
- 两层过滤分离：basicValid（决定是否进入系统）vs triggerValid（决定是否触发意图）
- DD-V1.2 P1-1：confidence 缺失策略（按配置项决定）

**验收标准**：
- [ ] 客户消息存入历史 + 触发意图识别
- [ ] 坐席消息存入历史 + 不触发意图识别
- [ ] 短句存入历史 + 不触发意图识别（按 P0-7）
- [ ] 重复 sentenceId 跳过

### Task M02：句间合并器

**输入**：DD-V1.2 第 9 章

**实现范围**：
```
com.cmbchina.cs.assitsvc.asr
├── SentenceMerger             # 防抖 + 沉默 timer
└── SentenceContinuityDetector # 句子连续性判定
```

**关键约束**：
- ConcurrentHashMap 维护防抖 timer 和沉默 timer
- 触发器/抑制器词典硬编码（先简单实现）
- 防抖 ms：completeMs=500/neutralMs=1500/incompleteMs=3000
- 沉默 timer 独立，2000ms

**验收标准**：
- [ ] 完整句 500ms 触发
- [ ] 待续句 3000ms 触发
- [ ] 沉默 2s 触发

### Task M06：AI Feign Client 业务封装（DD-V1.2 P1-5 + P1-4）

**输入**：DD-V1.2 第 13 章

**实现范围**：
```
com.cmbchina.cs.assitsvc.core.intent
├── IntentRecognitionService       # 接口
├── IntentRecognitionServiceImpl   # 实现（含过滤 + 熔断 + 计数）
└── ExecutedStepsManager           # executedSteps 维护
```

**关键约束**：
- DD-V1.2：调用前过滤 `speakerRole=CUSTOMER`
- DD-V1.2 P1-5：`@CircuitBreaker(name = "aiIntentClient", fallbackMethod = "fallback")`
- DD-V1.2 P1-4：单通话 max-ai-calls 计数（Redis incrBy）

**验收标准**：
- [ ] 只传客户消息给 AI
- [ ] AI 失败时熔断打开
- [ ] 单通话超过 50 次调用被拦截

### Task M07：意图-功能匹配（DD-V1.2 P1-9 + P1-19）

**输入**：DD-V1.2 第 14 章

**实现范围**：
```
com.cmbchina.cs.assitsvc.core.match
├── IntentFunctionMatcherService     # 接口
├── IntentFunctionMatcherServiceImpl # 实现
├── CopilotConfigCache               # CLOB 内存缓存
└── MenuVersionDao                   # 数据库读取
```

**关键约束**：
- DD-V1.2 P1-9：先过 GrayPolicy（最简：operator 白名单）
- DD-V1.2 P1-10：mapping_priority 仅在 cs_copilot_intent_mapping 中
- DD-V1.2 P1-19：condition_rule 异常返回 false

**验收标准**：
- [ ] 单 intentCode 多候选按 priority 倒序
- [ ] enabled=N 不返回
- [ ] DISABLED 风险等级不返回

### Task M08：参数解析器（DD-V1.2 P0-4）

**输入**：DD-V1.2 第 15 章

**实现范围**：
```
com.cmbchina.cs.assitsvc.core.param
├── ParamResolverService           # 接口
├── ParamResolverServiceImpl       # 实现
└── CookiePlaceholderValidator     # Cookie 白名单 + 域名绑定
```

**关键约束**：
- DD-V1.2 P0-4：Cookie 白名单 + 域名绑定校验
- COOKIE_PLACEHOLDER 输出 `${COOKIE.xxx}` 占位符
- 必填参数缺失 → 返回缺失列表（不抛异常）

### Task M09：跳转指令构建器

**输入**：DD-V1.2 第 16 章

**实现范围**：
```
com.cmbchina.cs.assitsvc.core.directive
├── DirectiveBuilderService        # 接口
└── DirectiveBuilderServiceImpl    # 实现
```

**关键约束**：
- 调用 UrlBuilder（已含 P0-5 多重校验）
- actionType 派生（targetKind + openMode → actionType）
- expireAt = 当前时间 + 30 秒

### Task M10：WebSocket 推送

**输入**：DD-V1.2 第 16.5 节

**实现范围**：
```
com.cmbchina.cs.assitsvc.push
└── CopilotPushService             # 推送服务
```

**关键约束**：
- 复用工作台已有 WebSocket 基础设施
- 推送目标：`/user/{operatorId}/copilot/directive`
- 用 `SimpMessagingTemplate.convertAndSend`

---

## 阶段 6：闭环模块

### Task M11：反馈接口（DD-V1.2 P0-6 关键修订）

**输入**：DD-V1.2 第 17 章 + 第 25 章

**实现范围**：
```
com.cmbchina.cs.assitsvc.api.controller
└── FeedbackController             # POST /copilot/feedback

com.cmbchina.cs.assitsvc.core.feedback
├── FeedbackService                # 接口
├── FeedbackServiceImpl            # 实现（含指令校验 + 幂等）
└── MuteListManager                # 静默列表
```

**关键约束**：
- DD-V1.2 P0-6：通过 directive_id 反查 trigger_log，校验 expireAt + callId + operatorId 一致
- DD-V1.2 P1-14：is_effective 字段（首次 Y，后续 N）
- DD-V1.2 P1-15：feedback_log.trigger_log_id 后端反查填充（不依赖前端）

### Task M16：业务监控埋点（已在阶段 2 任务 I-4 中初始化）

**输入**：DD-V1.2 第 18 章

**实现**：在各模块中调用 MetricsService 埋点。

### Task M17：多 Pod 配置一致性轮询（DD-V1.2 P1-17 新增）

**输入**：DD-V1.2 模块 M17

**实现范围**：
```
com.cmbchina.cs.assitsvc.config
└── ConfigVersionPoller            # @Scheduled 30s 轮询
```

**关键约束**：
- 每 30 秒查询 cs_menu_version 最新 active 版本
- 与本地 currentVersion 不一致时触发 reload
- 用 `@Scheduled(fixedDelayString = "${copilot.config-refresh.polling-interval-ms}")`

---

## 阶段 7：CLOB 扩展（涉及存量代码）

### Task M12：CLOB 生成扩展

**输入**：DD-V1.2 第 22 章

**关键约束**：
- 改动存量代码（菜单管理后台一键发布逻辑）
- 在 items 节点嵌入 copilotExt
- 新增 copilotIndex 反向索引
- DD-V1.2 P1-8：发布前基础校验

**协调对象**：存量发布团队

---

## 阶段 8：配置后台

### Task M13：Copilot 配置后台

**输入**：DD-V1.2 第 19 章 + 第 27 章

**实现范围**：
```
com.cmbchina.cs.assitsvc.api.controller
└── AdminController            # POST /copilot/admin/config/refresh
                                  POST /copilot/admin/intent-tree/reload
                                  GET  /copilot/health
```

---

## 扩展点（贯穿各阶段）

无论何时实现以下接口，都需要先创建扩展点，再写默认 NoOp 实现：

```
com.cmbchina.cs.assitsvc.extension
├── EntityExtractor                  # F02/F03 用
├── ClarificationStrategy            # F04 用
├── GrayPolicy                       # F07 用（本期 OperatorWhitelistGrayPolicy）
└── PermissionChecker                # F13 用
```

每个扩展点用 `@ConditionalOnMissingBean` 默认空实现。
