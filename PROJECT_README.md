# AI-Health-System - 项目详细阅读文档

> **项目名称**: AI 健康管理与运动指导系统  
> **技术版本**: Spring Boot 3.3.0 / Java 17 / MySQL 8.0 / Redis 7  
> **AI 引擎**: DeepSeek API + LangChain4j  
> **构建工具**: Maven + Docker Compose  

---

## 一、项目概述

AI-Health-System 是一个 **AI-Native 健康管理与运动指导系统**，旨在通过大语言模型（DeepSeek）为终端用户提供个性化的健康计划生成、饮食分析、食物识别、心理激励、健康咨询等智能服务。

系统已从初期的 "大模型 API 包壳" 阶段演进为具备 **Multi-Agent 架构**、**SDUI 协议**、**LLMOps 运维体系**、**安全合规** 和 **商业化计费** 能力的商业级产品。

---

## 二、技术栈

| 层次 | 技术选型 | 说明 |
|------|----------|------|
| **后端框架** | Spring Boot 3.3.0 | 主框架 |
| **语言** | Java 17 | LTS 版本 |
| **ORM** | MyBatis-Plus 3.5.7 | 增强型 MyBatis |
| **数据库** | MySQL 8.0 | 主数据库，utf8mb4 编码 |
| **缓存** | Redis 7 + Lettuce | 缓存与会话管理 |
| **安全** | Spring Security Crypto + JWT (jjwt 0.11.5) | 密码加密 + 无状态认证 |
| **AI 框架** | LangChain4j 1.0.0-beta2 | Agent/Tool/Chain 编排 |
| **AI 模型** | DeepSeek API (deepseek-chat) | 主力模型 |
| **备选模型** | Qwen-Max / GLM-4 / Moonshot-v1 | 多模型路由降级 |
| **容灾** | Resilience4j 2.2.0 | 熔断、重试、限流 |
| **对象映射** | MapStruct 1.5.5 | DTO/Entity/VO 转换 |
| **API 文档** | SpringDoc OpenAPI 2.5.0 | Swagger UI |
| **响应式调用** | Spring WebFlux (WebClient) | AI API 异步调用 |
| **容器化** | Docker + Docker Compose | 一键部署 |
| **数据库版本** | Flyway/Liquibase (可选) | 当前使用 SQL 脚本 |

---

## 三、项目目录结构

```
AI-Health-System/
├── backend/                              # 后端 Spring Boot 工程
│   ├── pom.xml                           # Maven 依赖配置
│   ├── checkstyle.xml                    # 代码风格检查
│   └── src/
│       ├── main/
│       │   ├── java/com/example/
│       │   │   ├── AiHealthSystemApplication.java   # 启动入口
│       │   │   ├── agent/                # Multi-Agent 智能体模块
│       │   │   │   ├── HealthCoachAgent.java        # 健康教练 Agent
│       │   │   │   ├── NutritionAgent.java          # 营养师 Agent
│       │   │   │   ├── PsychologyAgent.java         # 心理咨询 Agent
│       │   │   │   ├── SafetyReviewAgent.java       # 安全审查 Agent
│       │   │   │   ├── model/            # Agent 数据模型
│       │   │   │   ├── orchestrator/     # Agent 编排器
│       │   │   │   └── tool/             # Agent 工具集
│       │   │   ├── annotation/           # 自定义注解
│       │   │   │   ├── AdminOnly.java              # 管理员权限
│       │   │   │   ├── NoRepeatSubmit.java         # 防重复提交
│       │   │   │   ├── RateLimit.java              # 接口限流
│       │   │   │   └── RequiresSubscription.java   # 订阅要求
│       │   │   ├── aspect/               # AOP 切面实现
│       │   │   ├── billing/              # 计费引擎
│       │   │   │   ├── BillingService.java
│       │   │   │   ├── BillingSummary.java
│       │   │   │   └── SubscriptionService.java
│       │   │   ├── common/               # 公共类
│       │   │   │   ├── BusinessException.java      # 业务异常
│       │   │   │   ├── GlobalExceptionHandler.java # 全局异常处理
│       │   │   │   └── Result.java                 # 统一响应体
│       │   │   ├── config/               # 配置类
│       │   │   │   ├── SecurityConfig.java         # BCrypt 密码加密
│       │   │   │   ├── CorsConfig.java             # 跨域配置
│       │   │   │   ├── JacksonXssConfig.java       # Jackson XSS 防护
│       │   │   │   ├── LangChain4jConfig.java      # LangChain4j 配置
│       │   │   │   ├── MybatisPlusConfig.java      # MyBatis-Plus 配置
│       │   │   │   ├── RedisConfig.java            # Redis 配置
│       │   │   │   ├── SpringDocConfig.java        # API 文档
│       │   │   │   └── WebMvcConfig.java           # Web MVC 配置
│       │   │   ├── controller/           # REST 控制器 (28 个)
│       │   │   ├── convert/              # MapStruct 转换器 (8 个)
│       │   │   ├── dto/                  # 数据传输对象 (30+ 个)
│       │   │   ├── entity/               # 数据库实体 (38+ 个)
│       │   │   ├── evaluation/           # LLM 评测引擎
│       │   │   │   ├── LLMEvaluator.java           # LLM-as-a-Judge
│       │   │   │   ├── RagasMonitor.java           # RAGAS 评测
│       │   │   │   ├── EvalMetricsCollector.java   # 指标收集
│       │   │   │   └── EvalResult.java             # 评测结果
│       │   │   ├── event/                # 事件定义
│       │   │   │   ├── CheckinCompletedEvent.java
│       │   │   │   ├── FoodRecognizedEvent.java
│       │   │   │   └── SleepLoggedEvent.java
│       │   │   ├── filter/               # Servlet 过滤器
│       │   │   │   ├── XssFilter.java
│       │   │   │   └── XssHttpServletRequestWrapper.java
│       │   │   ├── interceptor/          # JWT 拦截器
│       │   │   ├── listener/             # 事件监听器
│       │   │   ├── llmops/               # LLMOps 运维模块
│       │   │   │   ├── LlmOpsController.java       # 运维 API
│       │   │   │   ├── AlertManager.java           # 告警管理器
│       │   │   │   ├── MetricsExporter.java        # 指标导出
│       │   │   │   ├── PrometheusMetricsExporter.java  # Prometheus 集成
│       │   │   │   └── PromptVersionManager.java   # Prompt 版本管理
│       │   │   ├── mapper/               # MyBatis Mapper (36+ 个)
│       │   │   ├── monitor/              # 成本监控
│       │   │   │   ├── DeepSeekCostMonitor.java    # API 成本核算
│       │   │   │   └── ModelTier.java              # 模型分层
│       │   │   ├── properties/           # 配置属性类
│       │   │   │   ├── CorsProperties.java
│       │   │   │   ├── DeepSeekProperties.java
│       │   │   │   └── JwtProperties.java
│       │   │   ├── resilience/           # 容灾模块
│       │   │   │   ├── CircuitState.java           # 熔断状态机
│       │   │   │   ├── FallbackService.java        # 降级服务
│       │   │   │   ├── ModelConfig.java            # 模型配置
│       │   │   │   ├── ModelHealthChecker.java     # 模型健康检查
│       │   │   │   ├── ModelRouter.java            # 多模型智能路由
│       │   │   │   ├── OnlineSafetyCircuitBreaker.java # 安全熔断器
│       │   │   │   └── SafetyCircuitConfig.java    # 安全熔断配置
│       │   │   ├── scheduler/            # 定时任务
│       │   │   │   ├── CheckinReminderScheduler.java  # 打卡提醒
│       │   │   │   ├── HealthPushScheduler.java        # 健康推送
│       │   │   │   ├── PushFrequencyController.java    # 推送频率控制
│       │   │   │   └── SafetySamplingTask.java         # 安全采样
│       │   │   ├── sdui/                 # SDUI 组件协议 (11 个)
│       │   │   ├── service/              # 业务接口 (30+ 个)
│       │   │   │   └── impl/             # 业务实现 (28 个)
│       │   │   ├── util/                 # 工具类
│       │   │   │   ├── JwtUtil.java                # JWT 工具
│       │   │   │   ├── AiResponseParser.java       # AI 响应解析
│       │   │   │   ├── PromptSanitizer.java        # Prompt 注入防护
│       │   │   │   ├── DataMaskingService.java     # 数据脱敏
│       │   │   │   ├── MedicalDisclaimerFilter.java # 医疗免责声明
│       │   │   │   ├── ImageCompressor.java        # 图片压缩
│       │   │   │   ├── EmotionAnalyzer.java        # 情绪分析
│       │   │   │   └── PromptCacheService.java     # Prompt 缓存
│       │   │   └── vo/                   # 视图对象 (40+ 个)
│       │   └── resources/
│       │       ├── application.yml       # 主配置文件
│       │       └── logback-spring.xml    # 日志配置
│       └── test/                         # 单元测试
├── docker-compose.yml                    # 容器编排 (MySQL + Redis + Backend + Frontend)
├── Dockerfile                            # 后端镜像构建
├── ai_health_system.sql                  # 数据库初始化脚本
├── .env.example                          # 环境变量模板
├── DEVELOPMENT_PLAN.md                   # 开发完善计划
└── PROJECT_README.md                     # 本文档
```

---

## 四、核心架构设计

### 4.1 分层架构

```
┌─────────────────────────────────────────────────┐
│                  Controller 层                    │
│   接收 HTTP 请求，参数校验，调用 Service          │
├─────────────────────────────────────────────────┤
│                   Service 层                      │
│   核心业务逻辑，Agent 编排，AI 调用              │
├─────────────────────────────────────────────────┤
│                   Mapper 层                       │
│   数据库访问，MyBatis-Plus BaseMapper            │
├─────────────────────────────────────────────────┤
│                   Entity 层                       │
│   数据库实体映射，JPA 注解                       │
└─────────────────────────────────────────────────┘
```

### 4.2 Multi-Agent 架构

系统采用 **多智能体协作** 模式，由 `AgentOrchestrator` 统一编排：

```
                        ┌──────────────────┐
                        │  AgentOrchestrator │  ← 编排器
                        └────────┬─────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│ HealthCoachAgent │   │ NutritionAgent  │   │ PsychologyAgent │
│   (健康教练)     │   │   (营养师)      │   │   (心理咨询师)  │
└─────────────────┘   └─────────────────┘   └─────────────────┘
                                 │
                                 ▼
                  ┌──────────────────────┐
                  │  SafetyReviewAgent   │  ← 安全审查（最后防线）
                  └──────────────────────┘
```

- **HealthCoachAgent**: 生成个性化运动/饮食/冥想计划
- **NutritionAgent**: 饮食分析、营养计算、食物推荐
- **PsychologyAgent**: 情绪管理、压力缓解、行为激励
- **SafetyReviewAgent**: 审查所有输出，确保医疗合规和运动安全

### 4.3 容灾与模型路由

```
正常状态: DeepSeek API → 返回结果
                    ↓ (失败/超时/安全分低)
降级状态: ModelRouter → Qwen-Max / GLM-4 / Moonshot-v1
                    ↓ (全部模型不可用)
兜底状态: FallbackService → 规则引擎降级方案（exercise_rules 表）
```

核心组件：
- **ModelRouter**: 多模型智能路由，按场景/成本自动选模型
- **ModelHealthChecker**: 定时探测模型健康状态
- **OnlineSafetyCircuitBreaker**: 安全分连续低于阈值时自动熔断
- **FallbackService**: 规则引擎兜底，基于 `exercise_rules` 表生成计划

### 4.4 SDUI 协议（Server-Driven UI）

所有 AI 接口返回结构化 `AiAgentResponse`，前端根据 `type` 字段动态渲染组件：

| Widget 类型 | 用途 |
|------------|------|
| `exercise_card` | 运动任务卡片（含视频链接、打卡回调） |
| `meal_chart` | 饮食营养图表（热量/蛋白质/碳水/脂肪） |
| `progress_ring` | 进度环（计划完成度百分比） |
| `timer` | 计时器组件 |
| `notification` | 通知卡片 |
| `quiz` | 互动问答 |
| `stat_card` | 统计数据卡 |
| `comparison` | 对比图表 |
| `tip` | 健康小贴士 |
| `text_block` | 纯文本块 |

### 4.5 LLMOps 运维体系

```
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│  LLMEvaluator │   │  AlertManager │   │  MetricsExporter │
│  (自动化评测)  │   │  (智能告警)   │   │  (指标上报)    │
└──────┬───────┘   └──────┬───────┘   └──────┬───────┘
       │                  │                  │
       ▼                  ▼                  ▼
┌──────────────────────────────────────────────────┐
│              监控指标体系                         │
│  - P99 延迟 / 成功率 / 幻觉率 / Token 消耗       │
│  - 安全评分趋势 / 熔断器状态                     │
│  - 成本核算（按模型/用户/场景维度）               │
└──────────────────────────────────────────────────┘
```

---

## 五、数据库设计概要

系统包含 **38+ 张数据表**，按业务域划分：

### 5.1 用户体系
| 表名 | 说明 |
|------|------|
| `sys_user` | 用户主表 |
| `user_profile` | 用户健康画像（病史、过敏史等） |
| `user_memory` | 用户记忆（长期偏好、习惯） |
| `user_usage` | 用户 AI 调用用量 |
| `subscription` | 订阅信息 |

### 5.2 AI 计划体系
| 表名 | 说明 |
|------|------|
| `ai_plan` | AI 主计划 |
| `ai_plan_detail` | 计划每日明细（运动/饮食任务项） |
| `ai_plan_feedback` | 用户反馈（难度/强度/满意度） |
| `prompt_template` | Prompt 模板（支持 A/B 测试和热更新） |

### 5.3 健康记录体系
| 表名 | 说明 |
|------|------|
| `daily_checkin` | 每日打卡 |
| `diet_record` | 饮食执行记录 |
| `exercise_record` | 运动执行记录 |
| `sleep_record` | 睡眠记录 |
| `water_record` | 饮水记录 |
| `body_measurement` | 身体测量（体重、围度等） |
| `health_record` | 健康主记录 |
| `health_report` | 健康报告 |
| `emotion_record` | 情绪记录 |

### 5.4 安全合规体系
| 表名 | 说明 |
|------|------|
| `safety_rule` | 安全规则（疾病-禁忌-替代方案） |
| `compliance_rule` | 合规校验规则 |
| `ai_call_audit_log` | AI 调用全链路审计日志 |
| `admin_audit_log` | 管理员操作审计日志 |

### 5.5 社区与互动
| 表名 | 说明 |
|------|------|
| `community_post` | 社区帖子 |
| `community_comment` | 评论 |
| `community_like` | 点赞 |

### 5.6 字典与管理
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

---

## 六、安全与合规设计

### 6.1 数据分级策略

| 级别 | 示例 | 处理方式 |
|------|------|----------|
| **L1 公开** | 运动类型、食物名称 | 直接上传 AI |
| **L2 一般** | 身高、体重、BMI | 直接上传 AI |
| **L3 敏感** | 疾病史、过敏史 | `DataMaskingService` 泛化后上传 |
| **L4 极度敏感** | 身份证、手机号、基因数据 | **禁止上传**，仅本地处理 |

### 6.2 Prompt 注入防护

`PromptSanitizer` 工具类检测并过滤以下注入模式：
- 指令覆盖：`忽略前面所有指令` → `[已过滤]`
- 角色劫持：`你现在是xxx` → `[已过滤]`
- 恶意输出诱导：`输出一段xxx` → `[已过滤]`
- 特殊标记注入：`[DONE]`, `[SYSTEM]` → `[已过滤]`

### 6.3 医疗合规

- `MedicalDisclaimerFilter`: 所有 AI 回复末尾自动追加免责声明
- `SafetyCheckerService`: 规则引擎 + 合规校验 + AI 二次审查三重保障
- `ComplianceRule`: 禁止绝对化医疗用语（"治愈"、"根治"、"特效药"）

### 6.4 XSS 防护

- `XssFilter` + `XssHttpServletRequestWrapper`: 请求参数 HTML 转义
- `JacksonXssConfig`: JSON 反序列化时 XSS 过滤

---

## 七、Controller 层 API 一览（28 个控制器）

### 用户端 API

| Controller | 路径前缀 | 功能说明 |
|------------|----------|----------|
| `AuthController` | `/api/auth` | 登录/注册/验证码/密码重置 |
| `UserController` | `/api/user` | 个人资料管理 |
| `HealthController` | `/api/health` | 健康数据 CRUD |
| `BodyMeasurementController` | `/api/body-measurement` | 身体测量记录 |
| `CheckinController` | `/api/checkin` | 每日打卡 |
| `FoodController` | `/api/food` | 饮食记录管理 |
| `ExerciseController` | `/api/exercise` | 运动记录管理 |
| `SleepController` | `/api/sleep` | 睡眠记录 |
| `WaterController` | `/api/water` | 饮水记录 |
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
| `DashboardController` | `/api/dashboard` | 仪表盘（今日/周/月汇总） |
| `GoalMilestoneController` | `/api/goal-milestone` | 目标里程碑 |
| `CommunityController` | `/api/community` | 社区互动 |
| `NotificationController` | `/api/notification` | 消息通知 |
| `NotificationPreferenceController` | `/api/notification-preference` | 通知偏好设置 |
| `AnnouncementController` | `/api/announcement` | 系统公告 |
| `DataExportController` | `/api/data-export` | 数据导出 |

### 管理端 API

| Controller | 路径前缀 | 功能说明 |
|------------|----------|----------|
| `AdminUserController` | `/api/admin/user` | 用户管理 |
| `AdminFoodController` | `/api/admin/food` | 食物字典管理 |
| `AdminExerciseController` | `/api/admin/exercise` | 运动字典管理 |
| `AdminAnnouncementController` | `/api/admin/announcement` | 公告管理 |
| `AdminNotificationController` | `/api/admin/notification` | 消息推送管理 |
| `AdminAuditLogController` | `/api/admin/audit-log` | 审计日志查询 |
| `AdminPlanFeedbackController` | `/api/admin/plan-feedback` | 计划反馈管理 |

---

## 八、AOP 切面体系

| 切面 | 注解 | 功能 |
|------|------|------|
| `AdminPermissionAspect` | `@AdminOnly` | 管理员权限校验 |
| `NoRepeatSubmitAspect` | `@NoRepeatSubmit` | 基于 Redis 的防重复提交 |
| `RateLimitAspect` | `@RateLimit` | 接口调用频率限制 |
| `SubscriptionAspect` | `@RequiresSubscription` | 订阅等级检查 |

---

## 九、定时任务

| 定时任务 | 执行频率 | 功能 |
|----------|----------|------|
| `CheckinReminderScheduler` | 每日固定时间 | 未打卡用户推送提醒 |
| `HealthPushScheduler` | 动态（受 `PushFrequencyController` 控制） | 四大核心推送：安全告警(P0)、运动提醒(P2)、进度周报(P3)、饮食建议(P2) |
| `AlertManager.checkAlerts()` | 每 1 分钟 | AI API 告警检查 |
| `SafetySamplingTask` | 定时 | 线上安全随机采样评测 |

---

## 十、计费引擎

| 等级 | 价格 | 免费额度 | 超量价格 | 每日调用限制 |
|------|------|----------|----------|-------------|
| **免费版** | 免费 | 0 | - | 3 次/日 |
| **Pro 版** | ¥19/月 | 500 万 token/月 | input ¥1/M, output ¥2/M | 无限制 |
| **企业版** | ¥99/月 | 2000 万 token/月 | input ¥0.8/M, output ¥1.6/M | 无限制 |

---

## 十一、环境变量配置

启动前需在 `.env` 文件中配置以下变量（模板见 `.env.example`）：

| 变量 | 说明 | 示例 |
|------|------|------|
| `DB_URL` | 数据库连接 | `jdbc:mysql://localhost:3306/ai_health_system?...` |
| `DB_USERNAME` | 数据库用户 | `root` |
| `DB_PASSWORD` | 数据库密码 | `your-db-password` |
| `REDIS_HOST` | Redis 地址 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码 | `your-redis-password` |
| `JWT_SECRET` | JWT 密钥（至少 256 位） | `openssl rand -base64 32` 生成 |
| `DEEPSEEK_API_KEY` | DeepSeek API Key | `sk-xxx` |
| `CORS_ORIGINS` | CORS 允许来源 | `http://localhost:5173` |
| `SMS_DEV_MODE` | 短信开发模式 | `true` |

---

## 十二、部署方式

### Docker Compose 一键部署

```bash
# 1. 复制并配置环境变量
cp .env.example .env
# 编辑 .env 填入实际值

# 2. 初始化数据库
docker-compose up -d mysql redis
# 等待健康检查通过后导入 SQL
docker exec -i ai-health-mysql mysql -uroot -p${DB_PASSWORD} ai_health_system < ai_health_system.sql

# 3. 构建并启动全部服务
docker-compose up -d

# 4. 验证
curl http://localhost:8080/actuator/health
```

### 本地开发

```bash
# 1. 启动 MySQL 和 Redis
docker-compose up -d mysql redis

# 2. 导入数据库
# 3. 配置 application.yml 或环境变量
# 4. 启动应用
cd backend
mvn spring-boot:run
```

---

## 十三、关键业务流程

### 13.1 AI 计划生成流程

```
用户请求 → AiPlanController
  → 订阅检查（SubscriptionAspect）
  → AiPlanService.generatePlan()
    → 获取用户画像（UserProfile）
    → 数据脱敏（DataMaskingService）
    → Prompt 注入防护（PromptSanitizer）
    → 模型路由（ModelRouter）
    → 调用 AI（DeepSeek API / 备选模型）
    → 安全审查（SafetyCheckerService + SafetyReviewAgent）
    → 追加免责声明（MedicalDisclaimerFilter）
    → 写入审计日志（AiCallAuditLog）
    → 结构化 SDUI 响应（AiAgentResponse）
    → 返回前端
```

### 13.2 食物识别流程

```
用户上传图片 → FoodRecognitionController
  → 图片压缩（ImageCompressor，压缩至 1024px + JPEG 70%）
  → Base64 编码
  → 调用 AI 视觉识别
  → JSON 解析（AiResponseParser，容错处理）
  → 发布 FoodRecognizedEvent 事件
  → 返回 FoodRecognizeVO
```

### 13.3 健康推送流程

```
HealthPushScheduler 定时触发
  → 分布式锁（Redis SETNX）防止重复执行
  → 遍历活跃用户
  → 按优先级执行：
    P0 安全告警 → 检测禁忌运动/饮食
    P1 重要调整 → 睡眠不足/热量超标
    P2 常规提醒 → 运动/饮水提醒
    P3 鼓励/周报 → 完成率表扬/健康周报
  → PushFrequencyController 控制推送频率（防骚扰）
  → 写入 sys_notification 表
```

---

## 十四、技术亮点

1. **Multi-Agent 架构**: 四个专业 Agent 协作，通过 `AgentOrchestrator` 统一编排
2. **SDUI 协议**: 10 种 UI 组件类型，后端驱动前端渲染，支持协议版本演进
3. **多模型智能路由**: 主模型故障时自动切换备选，全部不可用时降级到规则引擎
4. **安全熔断器**: 基于滑动窗口的安全评分熔断机制，自动探测恢复
5. **LLM-as-a-Judge**: 自动评测引擎，支持 CI/CD 集成和线上实时采样
6. **Prompt 模板外置**: 数据库存储 + Redis 缓存，支持热更新和 A/B 测试
7. **全链路审计**: 所有 AI 调用记录入 `ai_call_audit_log`，支持问题回溯
8. **数据脱敏**: L3/L4 级敏感数据自动脱敏后再上传第三方 AI
9. **事件驱动**: `CheckinCompletedEvent`/`FoodRecognizedEvent`/`SleepLoggedEvent` 解耦业务
10. **成本管控**: Token 消耗实时监控、Prompt 缓存去重、按模型分层计费

---

## 十五、开发完善计划（DEVELOPMENT_PLAN.md）

项目已规划四个阶段的迭代计划：

| 阶段 | 主题 | 核心目标 |
|------|------|----------|
| **Phase 0** | 安全修复与合规加固 | Prompt 注入防护、JSON 解析加固、数据脱敏、医疗免责、审计日志 |
| **Phase 1** | AI-Native 核心重构 | LangChain4j 集成、Function Calling、Safety Checker、SDUI 协议、成本管控 |
| **Phase 2** | 感知闭环与情感智能 | 多模态、事件驱动、情绪感知、知识库闭环 |
| **Phase 3** | 商业级产品化与 LLMOps | Multi-Agent、评测体系、计费引擎、容灾保障 |

---

## 十六、快速导航索引

| 关注点 | 入口文件 |
|--------|----------|
| **启动入口** | `AiHealthSystemApplication.java` |
| **数据库表结构** | `ai_health_system.sql` |
| **应用配置** | `application.yml` |
| **环境变量** | `.env.example` |
| **部署配置** | `docker-compose.yml` |
| **API 定义** | `controller/` 目录下 28 个 Controller |
| **AI 核心** | `DeepSeekService.java` + `AgentOrchestrator.java` |
| **安全合规** | `DataMaskingService.java` + `PromptSanitizer.java` + `SafetyCheckerService.java` |
| **容灾降级** | `ModelRouter.java` + `OnlineSafetyCircuitBreaker.java` + `FallbackService.java` |
| **计费系统** | `BillingService.java` + `SubscriptionService.java` |
| **SDUI 协议** | `sdui/Widget.java` + 10 个子类 + `AiAgentResponse.java` |
| **定时任务** | `scheduler/` 目录下 4 个定时任务 |
| **LLMOps** | `evaluation/LLMEvaluator.java` + `llmops/AlertManager.java` |
| **开发计划** | `DEVELOPMENT_PLAN.md` |