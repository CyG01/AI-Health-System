# AI-Health-System

> **AI-Native 健康管理与运动指导系统** — 基于大语言模型的个性化健康计划生成、饮食分析、心理激励与健康咨询服务。

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7-red)](https://redis.io/)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-1.0.0--beta2-purple)](https://docs.langchain4j.dev/)
[![DeepSeek](https://img.shields.io/badge/AI-DeepSeek-536DFE)](https://www.deepseek.com/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)

---

## 目录

- [项目概述](#项目概述)
- [核心特性](#核心特性)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [架构设计](#架构设计)
  - [分层架构](#分层架构)
  - [Multi-Agent 智能体架构](#multi-agent-智能体架构)
  - [SDUI 服务端驱动 UI](#sdui-服务端驱动-ui)
  - [容灾与模型路由](#容灾与模型路由)
- [功能模块](#功能模块)
  - [AI 智能服务](#ai-智能服务)
  - [健康管理](#健康管理)
  - [社区互动](#社区互动)
  - [商业化计费](#商业化计费)
  - [管理后台](#管理后台)
  - [LLMOps 运维](#llmops-运维)
- [安全与合规](#安全与合规)
- [API 概览](#api-概览)
- [数据库设计](#数据库设计)
- [部署指南](#部署指南)
- [相关文档](#相关文档)

---

## 项目概述

AI-Health-System 是一个 **AI-Native 健康管理与运动指导系统**，通过接入 DeepSeek 等大语言模型，为用户提供：

- **个性化健康计划生成**：基于用户身体数据、健康目标和偏好，AI 自动生成分天的运动/饮食/冥想计划
- **智能饮食分析**：食物图像识别、营养计算、饮食建议
- **动态计划调整**：根据用户打卡反馈自动调整计划强度与内容
- **24/7 健康咨询**：AI 健康顾问实时对话，解答运动、营养、心理相关问题
- **情绪管理与激励**：情绪记录与分析，AI 心理激励

系统采用 **Multi-Agent 多智能体协作架构**，由 HealthCoachAgent（健康教练）、NutritionAgent（营养师）、PsychologyAgent（心理咨询师）和 SafetyReviewAgent（安全审查）四大 Agent 协同工作，并通过 SDUI 协议实现前后端分离的高效协作。

---

## 核心特性

### AI 能力
- **Multi-Agent 协作**：4 个专业 Agent 分工协作，覆盖健康计划、营养、心理、安全审查
- **Function Calling**：模型自主决策调用工具，自动落库、查询、调整计划
- **SDUI 协议**：11 种 UI Widget，服务端驱动前端渲染，支持运动卡片、营养图表、进度环等
- **意图路由**：根据用户意图自动路由到合适的 Agent 和模型
- **知识库 RAG**：基于 Prompt 模板和知识文档的检索增强生成

### 安全合规
- **数据分级脱敏**：L1-L4 四级数据分级，敏感信息泛化后上传 AI
- **Prompt 注入防护**：6 类注入模式检测与过滤
- **医疗免责声明**：所有 AI 回复自动追加医疗免责
- **安全审查 Agent**：输出内容合规校验，禁止绝对化医疗用语
- **全链路审计日志**：AI 调用全链路记录，支持问题追溯
- **XSS 防护**：请求参数 HTML 转义 + JSON 反序列化过滤

### 高可用
- **多模型路由**：DeepSeek / Qwen-Max / GLM-4 / Moonshot-v1 四模型智能切换
- **熔断降级**：Resilience4j 熔断器 + 安全熔断器 + 规则引擎兜底
- **模型健康检查**：定时探测模型可用性，自动隔离故障模型
- **异步队列**：AI 任务异步执行，避免长时间阻塞

### 商业化
- **订阅计费**：分级订阅（Basic / Pro / Enterprise）
- **用量追踪**：按用户维度的 AI 调用用量统计
- **成本监控**：按模型/场景维度的 Token 消耗和费用核算
- **开票退款**：完整的订单-发票-退款流程

### LLMOps
- **Prompt 版本管理**：Prompt 模板外置数据库，支持热更新和 A/B 测试
- **自动化评测**：LLM-as-a-Judge + RAGAS 评测体系
- **监控告警**：Prometheus + Grafana 指标监控，Webhook 告警
- **安全采样**：定时采样 AI 输出进行安全审查

---

## 技术栈

| 层次 | 技术 | 说明 |
|------|------|------|
| **后端框架** | Spring Boot 3.3.0 | 主框架 |
| **语言** | Java 17 | LTS 版本 |
| **ORM** | MyBatis-Plus 3.5.7 | 增强型 MyBatis |
| **数据库** | MySQL 8.0 | 主数据库，utf8mb4 编码 |
| **缓存** | Redis 7 + Lettuce | 缓存与会话管理 |
| **AI 框架** | LangChain4j 1.0.0-beta2 | Agent/Tool/Chain 编排 |
| **AI 模型** | DeepSeek API（主力）/ Qwen-Max / GLM-4 / Moonshot-v1 | 多模型路由降级 |
| **安全** | Spring Security Crypto + JWT（jjwt 0.11.5） | 密码加密 + 无状态认证 |
| **容灾** | Resilience4j 2.2.0 | 熔断、重试、限流 |
| **对象映射** | MapStruct 1.5.5 | DTO/Entity/VO 转换 |
| **API 文档** | SpringDoc OpenAPI 2.5.0 | Swagger UI |
| **响应式调用** | Spring WebFlux（WebClient） | AI API 异步调用 |
| **监控** | Micrometer + Prometheus + Actuator | 指标采集与健康检查 |
| **本地缓存** | Caffeine 3.1.8 | Prompt 去重与语义缓存 |
| **容器化** | Docker + Docker Compose | 一键部署 |
| **代码规范** | Checkstyle | 代码风格检查 |
| **导出** | Apache POI 5.2.5 | Excel 数据导出 |

---

## 项目结构

```
AI-Health-System/
├── backend/                                # 后端 Spring Boot 工程
│   ├── pom.xml                             # Maven 依赖配置
│   ├── checkstyle.xml                      # 代码风格检查
│   └── src/main/java/com/example/
│       ├── AiHealthSystemApplication.java  # 启动入口
│       ├── agent/                          # Multi-Agent 智能体模块
│       │   ├── HealthCoachAgent.java       # 健康教练 Agent
│       │   ├── NutritionAgent.java         # 营养师 Agent
│       │   ├── PsychologyAgent.java        # 心理咨询 Agent
│       │   ├── SafetyReviewAgent.java      # 安全审查 Agent
│       │   ├── model/                      # Agent 数据模型
│       │   ├── orchestrator/               # Agent 编排器 + 意图路由
│       │   └── tool/                       # Agent 工具集 (5 个 Tool)
│       ├── annotation/                     # 自定义注解 (4 个)
│       │   ├── AdminOnly.java              # 管理员权限
│       │   ├── NoRepeatSubmit.java         # 防重复提交
│       │   ├── RateLimit.java              # 接口限流
│       │   └── RequiresSubscription.java   # 订阅要求
│       ├── aspect/                         # AOP 切面 (5 个)
│       ├── billing/                        # 计费引擎 (6 个类)
│       ├── common/                         # 公共类（异常、响应体）
│       ├── config/                         # 配置类 (10 个)
│       ├── controller/                     # REST 控制器 (34 个)
│       ├── convert/                        # MapStruct 转换器 (8 个)
│       ├── dto/                            # 数据传输对象 (30+ 个)
│       ├── entity/                         # 数据库实体 (40+ 个)
│       ├── evaluation/                     # LLM 评测引擎 (4 个类)
│       ├── event/                          # 事件定义 (4 个)
│       ├── filter/                         # Servlet 过滤器 (3 个)
│       ├── interceptor/                    # JWT 拦截器
│       ├── listener/                       # 事件监听器
│       ├── llmops/                         # LLMOps 运维模块 (6 个类)
│       ├── mapper/                         # MyBatis Mapper (36+ 个)
│       ├── monitor/                        # 成本监控 (2 个类)
│       ├── properties/                     # 配置属性类 (3 个)
│       ├── resilience/                     # 容灾模块 (8 个类)
│       ├── scheduler/                      # 定时任务 (5 个)
│       ├── sdui/                           # SDUI 组件协议 (11 个 Widget)
│       ├── service/                        # 业务接口 & 实现 (60+ 个类)
│       ├── util/                           # 工具类 (8 个)
│       └── vo/                             # 视图对象 (40+ 个)
├── ai_health_system.sql                    # 数据库初始化脚本
├── docker-compose.yml                      # 容器编排（MySQL + Redis + Backend + Frontend）
├── Dockerfile                              # 后端镜像构建
├── .env.example                            # 环境变量模板
├── .gitignore
├── .cursorrules
├── PROJECT_README.md                       # 项目详细文档
├── ARCHITECTURE_OPTIMIZATION_PLAN.md       # 架构优化计划
└── DEVELOPMENT_PLAN.md                     # 开发完善计划
```

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7+
- Docker & Docker Compose（可选）

### 方式一：Docker Compose 一键部署

```bash
# 1. 克隆项目
git clone <your-repo-url>
cd AI-Health-System

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env，填入你的 DeepSeek API Key 和其他配置

# 3. 启动所有服务（MySQL + Redis + Backend + Frontend）
docker-compose up -d

# 4. 初始化数据库
docker exec -i ai-health-mysql mysql -uroot -p${DB_PASSWORD} ai_health_system < ai_health_system.sql
```

### 方式二：本地开发

```bash
# 1. 启动 MySQL 和 Redis
docker-compose up -d mysql redis

# 2. 初始化数据库
mysql -uroot -p < ai_health_system.sql

# 3. 配置环境变量（或直接修改 application.yml）
set DB_PASSWORD=your-db-password
set REDIS_PASSWORD=your-redis-password
set JWT_SECRET=your-jwt-secret
set DEEPSEEK_API_KEY=sk-your-api-key

# 4. 启动后端
cd backend
mvn spring-boot:run

# 5. 访问 API 文档
# Swagger UI: http://localhost:8080/swagger-ui.html
# Actuator:   http://localhost:8081/actuator/health
```

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DB_URL` | 数据库连接地址 | `jdbc:mysql://localhost:3306/ai_health_system` |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | - |
| `REDIS_HOST` | Redis 主机 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码 | - |
| `JWT_SECRET` | JWT 签名密钥（生产环境用 `openssl rand -base64 64` 生成） | - |
| `DEEPSEEK_API_KEY` | DeepSeek API Key | - |
| `CORS_ORIGINS` | CORS 允许的来源（逗号分隔） | `http://localhost:5173` |

---

## 架构设计

### 分层架构

```
┌──────────────────────────────────────────────────┐
│                   Controller 层                    │
│     接收 HTTP 请求，参数校验，调用 Service          │
├──────────────────────────────────────────────────┤
│                    Service 层                      │
│     核心业务逻辑，Agent 编排，AI 调用              │
├──────────────────────────────────────────────────┤
│                    Mapper 层                       │
│     数据库访问，MyBatis-Plus BaseMapper            │
├──────────────────────────────────────────────────┤
│                    Entity 层                       │
│     数据库实体映射                                  │
└──────────────────────────────────────────────────┘
```

- **Filter / Interceptor**：安全头注入、XSS 过滤、JWT 认证
- **AOP**：权限校验（`@AdminOnly`）、防重复提交（`@NoRepeatSubmit`）、限流（`@RateLimit`）、订阅校验（`@RequiresSubscription`）
- **GlobalExceptionHandler**：统一异常处理，返回 `Result<T>` 格式

### Multi-Agent 智能体架构

```
                         ┌──────────────────┐
                         │ AgentOrchestrator │  ← 编排器
                         │   + IntentRouter  │
                         └────────┬─────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          │                       │                       │
          ▼                       ▼                       ▼
 ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
 │ HealthCoachAgent │   │ NutritionAgent  │   │ PsychologyAgent │
 │   (健康教练)     │   │   (营养师)      │   │   (心理咨询师)  │
 │                 │   │                 │   │                 │
 │ - 运动计划生成  │   │ - 饮食分析      │   │ - 情绪管理      │
 │ - 饮食计划定制  │   │ - 营养计算      │   │ - 压力缓解      │
 │ - 冥想指导      │   │ - 食物推荐      │   │ - 行为激励      │
 │ - 数据追踪      │   │ - 食物识别      │   │ - 心理评估      │
 └─────────────────┘   └─────────────────┘   └─────────────────┘
                                  │
                                  ▼
                   ┌──────────────────────┐
                   │  SafetyReviewAgent   │  ← 安全审查（最后防线）
                   │  - 医疗合规审查      │
                   │  - 运动安全评估      │
                   │  - 内容安全过滤      │
                   └──────────────────────┘
```

**Agent 工作流程**：

1. **IntentRouter** 分析用户输入，判定意图类型（运动/饮食/心理/闲聊）
2. **AgentOrchestrator** 根据意图路由到对应 Agent
3. **Agent** 通过 Function Calling 调用 Tools（写计划、查数据、调整强度等）
4. **SafetyReviewAgent** 审查所有 AI 输出，确保安全合规
5. 结果通过 SDUI 协议返回前端

### SDUI 服务端驱动 UI

所有 AI 接口通过 `AiAgentResponse` 返回结构化数据，前端根据 `type` 字段动态渲染：

| Widget 类型 | 说明 | 示例场景 |
|------------|------|----------|
| `exercise_card` | 运动任务卡片 | 每日运动计划，含动作名称、时长、视频链接、打卡回调 |
| `exercise_phase` | 运动阶段分组 | 按热身/主体/拉伸阶段组织运动任务 |
| `meal_chart` | 饮食营养图表 | 热量/蛋白质/碳水/脂肪分布雷达图 |
| `progress_ring` | 进度环 | 计划完成度百分比环形图 |
| `timer` | 计时器组件 | 运动计时、冥想倒计时 |
| `notification` | 通知卡片 | 健康提醒、系统消息 |
| `quiz` | 互动问答 | 健康知识问答 |
| `stat_card` | 统计数据卡 | 今日步数、消耗卡路里等关键指标 |
| `comparison` | 对比图表 | 本周 vs 上周数据对比 |
| `tip` | 健康小贴士 | AI 根据用户状态生成的个性化建议 |
| `text_block` | 纯文本块 | AI 对话回复、报告文字 |

### 容灾与模型路由

```
正常状态: DeepSeek API → 返回结果
                ↓ (失败/超时/安全分低)
降级状态: ModelRouter → Qwen-Max / GLM-4 / Moonshot-v1
                ↓ (全部模型不可用)
兜底状态: FallbackService → 规则引擎降级方案（exercise_rules 表）
```

核心组件：

| 组件 | 功能 |
|------|------|
| **ModelRouter** | 多模型智能路由，按意图分类（LOW/MEDIUM/HIGH/CRITICAL）和成本自动选模型 |
| **ModelHealthChecker** | 定时探测模型健康状态，自动隔离故障模型 |
| **OnlineSafetyCircuitBreaker** | 安全分连续低于阈值时自动熔断，防止批量不安全输出 |
| **FallbackService** | 规则引擎兜底，基于 `exercise_rules` 表生成降级计划 |
| **AiCallQueueService** | AI 任务异步队列，支持排队、重试、超时处理 |

---

## 功能模块

### AI 智能服务

| 功能 | 说明 | 核心类 |
|------|------|--------|
| **AI 计划生成** | 基于用户画像生成分天运动/饮食/冥想计划 | `AiPlanServiceImpl`, `PlanGenerateV2Service` |
| **AI 计划调整** | 根据打卡反馈动态调整计划 | `PlanAdjustServiceImpl`, `AutoPlanAdjustService` |
| **AI 健康咨询** | 多轮对话式健康顾问 | `ChatServiceImpl`, `MemoryService` |
| **食物识别** | 上传图片识别食物并估算营养 | `FoodRecognitionServiceImpl` |
| **运动指导** | 个性化运动方案推荐 | `ExerciseGuidanceServiceImpl` |
| **个性化推荐** | 基于用户行为推荐健康内容 | `RecommendationServiceImpl` |
| **AI 反馈** | 收集并处理用户对 AI 服务的反馈 | `AiFeedbackServiceImpl` |
| **引导上手** | 新用户引导流程 | `OnboardingServiceImpl` |

### 健康管理

| 功能 | 说明 |
|------|------|
| **身体测量** | 体重、BMI、围度等基础指标记录与趋势 |
| **血糖管理** | 血糖记录、异常告警、趋势分析 |
| **饮食记录** | 每日饮食摄入记录与营养分析 |
| **运动记录** | 运动执行记录、卡路里消耗统计 |
| **睡眠记录** | 睡眠时长、质量记录与分析 |
| **饮水记录** | 每日饮水追踪 |
| **情绪记录** | 情绪状态记录与情绪趋势分析 |
| **每日打卡** | 健康任务打卡、补签、连续打卡统计 |
| **健康报告** | 周期性健康综合报告生成 |
| **目标里程碑** | 健康目标设定与里程碑追踪 |
| **仪表盘** | 今日/本周/本月健康数据汇总 |
| **数据统计** | 多维度的健康数据趋势分析 |

### 社区互动

| 功能 | 说明 |
|------|------|
| **帖子发布** | 社区健康话题讨论 |
| **评论回复** | 帖子评论与互动 |
| **点赞** | 内容点赞 |

### 商业化计费

| 功能 | 说明 |
|------|------|
| **订阅管理** | 分级订阅（Basic / Pro / Enterprise） |
| **用量追踪** | AI 调用次数/Token 用量统计 |
| **账单汇总** | 按周期生成的费用明细 |
| **发票服务** | 电子发票申请与管理 |
| **退款处理** | 退款申请与审核 |
| **企业方案** | 企业批量采购与定制方案 |

### 管理后台

| 功能 | 说明 |
|------|------|
| **用户管理** | 用户列表、详情、状态管理 |
| **食物管理** | 食物字典维护 |
| **运动管理** | 运动字典维护 |
| **公告管理** | 系统公告发布与管理 |
| **通知推送** | 系统通知推送 |
| **审核管理** | 管理员审批流程 |
| **审计日志** | 管理员操作审计 |
| **规则建议** | 安全/合规规则建议管理 |
| **计划反馈** | 查看与处理用户计划反馈 |

### LLMOps 运维

| 功能 | 说明 |
|------|------|
| **Prompt 版本管理** | 模板热更新、版本回滚、A/B 测试 |
| **自动化评测** | LLM-as-a-Judge 质量评测 + RAGAS 检索评测 |
| **指标导出** | Prometheus 指标上报（延迟、成功率、幻觉率、Token 消耗） |
| **智能告警** | 基于阈值的实时告警 + Webhook 通知 |
| **运维 API** | 熔断器状态、模型健康、成本统计查询接口 |
| **安全采样** | 定时采样 AI 输出，自动化安全审查 |

---

## 安全与合规

### 数据分级策略

| 级别 | 示例数据 | 处理方式 |
|------|----------|----------|
| **L1 公开** | 运动类型、食物名称 | 直接上传 AI |
| **L2 一般** | 身高、体重、BMI | 直接上传 AI |
| **L3 敏感** | 疾病史、过敏史 | `DataMaskingService` 泛化后上传（如 "HIV" → "严重免疫系统疾病"） |
| **L4 极度敏感** | 身份证、手机号、基因数据 | **禁止上传**，仅本地处理 |

### Prompt 注入防护

`PromptSanitizer` 检测并过滤以下注入模式：
- 指令覆盖：`忽略前面所有指令` → `[已过滤]`
- 角色劫持：`你现在是xxx` → `[已过滤]`
- 恶意输出诱导：`输出一段xxx` → `[已过滤]`
- 特殊标记注入：`[DONE]`, `[SYSTEM]`, `<|` → `[已过滤]`

### 医疗合规

- **MedicalDisclaimerFilter**：所有 AI 回复末尾自动追加免责声明
- **SafetyCheckerService**：规则引擎 + 合规校验 + AI 二次审查三重保障
- **ComplianceRule**：禁止绝对化医疗用语（"治愈"、"根治"、"特效药"）
- **SafetyReviewAgent**：Multi-Agent 架构中的最后一环，审查所有输出内容

### 其他安全措施

- **XSS 防护**：`XssFilter` + `JacksonXssConfig` 双重过滤
- **JWT 认证**：无状态 token，支持过期刷新
- **BCrypt 密码加密**：用户密码使用 Spring Security Crypto 加密存储
- **接口限流**：`@RateLimit` 注解支持自定义限流策略
- **防重复提交**：`@NoRepeatSubmit` 注解防止表单重复提交
- **CORS 配置**：可控的跨域来源白名单

---

## API 概览

### 用户端 API（25 个 Controller）

| Controller | 路径前缀 | 功能 |
|------------|----------|------|
| `AuthController` | `/api/auth` | 登录/注册/验证码/密码重置 |
| `UserController` | `/api/user` | 个人资料管理 |
| `HealthController` | `/api/health` | 健康数据 CRUD |
| `BodyMeasurementController` | `/api/body-measurement` | 身体测量记录 |
| `CheckinController` | `/api/checkin` | 每日打卡 |
| `FoodController` | `/api/food` | 饮食记录管理 |
| `ExerciseController` | `/api/exercise` | 运动记录管理 |
| `SleepController` | `/api/sleep` | 睡眠记录 |
| `WaterController` | `/api/water` | 饮水记录 |
| `BloodSugarController` | `/api/blood-sugar` | 血糖记录 |
| `AiPlanController` | `/api/ai/plan` | AI 计划生成/管理 |
| `AiFeedbackController` | `/api/ai/feedback` | AI 反馈 |
| `ChatController` | `/api/chat` | AI 健康咨询对话 |
| `FoodRecognitionController` | `/api/food-recognition` | 食物图像识别 |
| `ExerciseGuidanceController` | `/api/exercise-guidance` | 运动指导 |
| `PlanAdjustController` | `/api/plan-adjust` | 计划动态调整 |
| `PlanFeedbackController` | `/api/plan-feedback` | 计划反馈 |
| `HealthReportController` | `/api/health-report` | 健康报告 |
| `StatisticsController` | `/api/statistics` | 数据统计/趋势 |
| `RecommendationController` | `/api/recommendation` | 个性化推荐 |
| `DashboardController` | `/api/dashboard` | 仪表盘 |
| `GoalMilestoneController` | `/api/goal-milestone` | 目标里程碑 |
| `CommunityController` | `/api/community` | 社区互动 |
| `NotificationController` | `/api/notification` | 消息通知 |
| `NotificationPreferenceController` | `/api/notification-preference` | 通知偏好设置 |

### 管理端 API（9 个 Controller）

| Controller | 路径前缀 | 功能 |
|------------|----------|------|
| `AdminUserController` | `/api/admin/user` | 用户管理 |
| `AdminFoodController` | `/api/admin/food` | 食物字典管理 |
| `AdminExerciseController` | `/api/admin/exercise` | 运动字典管理 |
| `AdminAnnouncementController` | `/api/admin/announcement` | 公告管理 |
| `AdminNotificationController` | `/api/admin/notification` | 通知推送 |
| `AdminApprovalController` | `/api/admin/approval` | 审批管理 |
| `AdminAuditLogController` | `/api/admin/audit-log` | 审计日志 |
| `AdminPlanFeedbackController` | `/api/admin/plan-feedback` | 计划反馈管理 |
| `AdminRuleSuggestionController` | `/api/admin/rule-suggestion` | 规则建议管理 |

### 商业与运维 API（4 个 Controller）

| Controller | 路径前缀 | 功能 |
|------------|----------|------|
| `BillingController` | `/api/billing` | 订阅/计费 |
| `EnterprisePlanController` | `/api/enterprise` | 企业方案 |
| `RefundAndInvoiceController` | `/api/billing` | 退款/发票 |
| `DataExportController` | `/api/export` | 数据导出 |
| `LlmOpsController` | `/api/llmops` | LLMOps 运维管理 |

---

## 数据库设计

系统包含 **40+ 张数据表**，按业务域划分：

### 用户体系
| 表名 | 说明 |
|------|------|
| `sys_user` | 用户主表 |
| `user_profile` | 用户健康画像（病史、过敏史等） |
| `user_memory` | 用户记忆（长期偏好、习惯） |
| `user_usage` | 用户 AI 调用用量 |
| `subscription` | 订阅信息 |

### AI 计划体系
| 表名 | 说明 |
|------|------|
| `ai_plan` | AI 主计划 |
| `ai_plan_detail` | 计划每日明细（运动/饮食任务项） |
| `ai_plan_feedback` | 用户反馈（难度/强度/满意度） |
| `ai_feedback` | AI 服务反馈 |
| `prompt_template` | Prompt 模板（支持 A/B 测试和热更新） |

### 健康记录体系
| 表名 | 说明 |
|------|------|
| `daily_checkin` | 每日打卡 |
| `diet_record` | 饮食执行记录 |
| `exercise_record` | 运动执行记录 |
| `sleep_record` | 睡眠记录 |
| `water_record` | 饮水记录 |
| `blood_sugar` | 血糖记录 |
| `body_measurement` | 身体测量（体重、围度等） |
| `health_record` | 健康主记录 |
| `health_report` | 健康报告 |
| `emotion_record` | 情绪记录 |

### 安全合规体系
| 表名 | 说明 |
|------|------|
| `safety_rule` | 安全规则（疾病-禁忌-替代方案） |
| `compliance_rule` | 合规校验规则 |
| `ai_call_audit_log` | AI 调用全链路审计日志 |
| `admin_audit_log` | 管理员操作审计日志 |
| `safety_review_log` | 安全审查日志 |
| `sampling_result` | 安全采样结果 |

### 社区与互动
| 表名 | 说明 |
|------|------|
| `community_post` | 社区帖子 |
| `community_comment` | 评论 |
| `community_like` | 点赞 |

### 字典与管理
| 表名 | 说明 |
|------|------|
| `food_item` | 食物字典 |
| `exercise_item` | 运动字典 |
| `exercise_rule` | 运动推荐规则（降级引擎数据源） |
| `goal_milestone` | 目标里程碑 |
| `sys_announcement` | 系统公告 |
| `sys_notification` | 系统通知 |
| `knowledge_doc` | 知识库文档 |
| `llm_test_case` | LLM 评测用例 |
| `ragas_test_case` | RAGAS 评测用例 |
| `invoice` | 发票信息 |
| `admin_approval` | 管理员审批 |
| `rule_suggestion` | 规则建议 |
| `chat_session` / `chat_message` | AI 对话会话与消息 |

---

## 部署指南

### 生产环境检查清单

- [ ] 修改 `.env` 中所有默认密码和密钥
- [ ] 使用 `openssl rand -base64 64` 生成 JWT 密钥
- [ ] 配置 HTTPS 证书（建议使用 Nginx 反向代理）
- [ ] 开启 MySQL `useSSL=true` 和 `allowPublicKeyRetrieval=false`
- [ ] 配置 Redis 持久化（AOF + RDB）
- [ ] 设置合理的 JVM 参数（建议 `-Xms1g -Xmx2g`）
- [ ] 配置日志收集（ELK / 阿里云 SLS）
- [ ] 配置 Prometheus + Grafana 监控面板
- [ ] 配置数据库定时备份
- [ ] 部署 Nginx 反向代理，配置 CORS 白名单

### Docker 部署

```bash
# 构建镜像
docker build -t ai-health-system:latest .

# 启动全部服务
docker-compose -f docker-compose.yml up -d

# 查看日志
docker-compose logs -f backend

# 停止服务
docker-compose down
```



---

## 相关文档

| 文档 | 说明 |
|------|------|
| [PROJECT_README.md](PROJECT_README.md) | 项目详细技术文档 |
| [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md) | 开发完善计划（Phase 0-3） |
| [ARCHITECTURE_OPTIMIZATION_PLAN.md](ARCHITECTURE_OPTIMIZATION_PLAN.md) | 架构优化计划（v2.0） |

---

## License

MIT License