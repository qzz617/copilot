# Customer Service Copilot Service

客服工作台 AI Copilot 服务后端。基于客户语音识别（ASR）+ AI 意图识别，为信用卡客服坐席推荐功能入口。

## 技术栈

- Java 8
- Spring Boot 2.7.18
- Spring Cloud OpenFeign（外部接口）
- Spring Kafka（ASR 消息消费）
- Spring Data Redis（RedisTemplate）
- FastJSON 2.x（JSON 序列化）
- Resilience4j（熔断保护）
- Lombok

## 快速开始

### 环境要求

- JDK 8+
- Maven 3.6+
- Redis 集群（开发环境可用单机）
- Kafka（开发环境可用单机）
- TDSQL（PG 兼容协议）

### 本地运行

```bash
mvn clean compile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 配置文件

| 文件 | 用途 |
|------|------|
| `application.yml` | 通用配置 |
| `application-dev.yml` | 开发环境 |
| `application-uat.yml` | UAT 环境 |
| `application-prod.yml` | 生产环境 |
| `intent-tree.json` | 意图树配置（不存表，本期固定值） |

## 模块结构

```
com.cmbchina.cs.assitsvc
├── api          # 接口层（Controller、DTO）
├── core         # 核心业务（intent、match、param、directive、feedback）
├── asr          # ASR 接入与处理
├── session      # 通话会话管理
├── push         # WebSocket 推送
├── config       # 配置缓存
├── infra        # 基础设施（feign、kafka、redis、metrics）
├── domain       # 领域模型
└── extension    # 扩展点（接口 + NoOp 默认实现）
```

详见 `CLAUDE.md` 中的包结构说明。

## 文档

| 文档 | 用途 |
|------|------|
| `docs/dd-v1.2.md` | 详细设计稿 |
| `docs/coding-standards.md` | 编码规范 |
| `docs/module-tasks.md` | 模块开发任务卡片 |
| `docs/development-sop.md` | 开发流程 SOP |
| `docs/claude-code-best-practices.md` | Claude Code 协作最佳实践 |

## Git 工作流（GitLab Flow）

- 主分支：`main`（受保护）
- 功能分支：`feature/m{XX}-{short-name}`，例：`feature/m02-sentence-merger`

## 数据库

- 5 张新增表，DDL 见 `sql/V1__init.sql`
- 复用存量 `cs_menu_*` 表（结构不变）

## API 接口

| 接口 | 说明 |
|------|------|
| `POST /copilot/feedback` | 反馈采集 |
| `POST /copilot/session/bind` | 来电弹屏绑定 |
| `POST /copilot/session/unbind` | 通话结束解绑 |
| `POST /copilot/admin/config/refresh` | 配置刷新 |
| `POST /copilot/admin/config/validate` | 配置校验 |
| `POST /copilot/admin/intent-tree/reload` | 意图树重新加载 |
| `GET /copilot/health` | 健康检查 |

## 联系人

- 项目负责人：（待填）
- 后端开发：（待填）
- 前端开发：（待填）
- 测试：（待填）
