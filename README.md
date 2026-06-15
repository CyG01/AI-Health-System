# AI-Health-System

> **AI-Native 健康管理与运动指导系统** — 基于大语言模型的个性化健康计划生成、饮食分析、心理激励与健康咨询服务。

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5-4FC08D)](https://vuejs.org/)
[![NaiveUI](https://img.shields.io/badge/NaiveUI-2.44-36AD58)](https://www.naiveui.com/)
[![Vite](https://img.shields.io/badge/Vite-8-646CFF)](https://vite.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6-3178C6)](https://www.typescriptlang.org/)
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
  - [前端架构](#前端架构)
- [功能模块](#功能模块)
  - [AI 智能服务](#ai-智能服务)
  - [健康管理](#健康管理)
  - [社区互动](#社区互动)
  - [商业化计费](#商业化计费)
  - [管理后台](#管理后台)
  - [LLMOps 运维](#llmops-运维)
- [前端视图与服务](#前端视图与服务)
  - [视图页面清单](#视图页面清单)
  - [API 服务模块](#api-服务模块)
  - [路由架构](#路由架构)
  - [状态管理](#状态管理)
  - [离线缓存](#离线缓存)
- [安全与合规](#安全与合规)
- [API 概览](#api-概览)
- [数据库设计](#数据库设计)
- [部署指南](#部署指南)
- [开发指南](#开发指南)
- [相关文档](#相关文档)

---

## 项目概述

AI-Health-System 是一个 **AI-Native 健康管理与运动指导系统**，通过接入 DeepSeek 等大语言模型，为用户提供：

- **个性化健康计划生成**：基于用户身体数据、健康目标和偏好，AI 自动生成分天的运动/饮食/冥想计划
- **智能饮食分析**：食物图像识别、营养计算、饮食建议
- **动态计划调整**：根据用户打卡反馈自动调整计划强度与内容
- **24/7 健康咨询**：AI 健康顾问实时对话（SSE 流式输出），解答运动、营养、心理相关问题
- **情绪管理与激励**：情绪记录与分析，AI 心理激励
- **AI Copilot 全局助手**：侧边栏抽屉式 AI 助手，支持多会话管理和计划固化

系统采用 **Multi-Agent 多智能体协作架构**，由 HealthCoachAgent（健康教练）、NutritionAgent（营养师）、PsychologyAgent（心理咨询师）和 SafetyReviewAgent（安全审查）四大 Agent 协同工作，并通过 SDUI 协议实现前后端分离的高效协作。

前端基于 **soybean-admin v2.2.0** 框架，采用 Vue 3.5 + NaiveUI + TypeScript 技术栈，支持 elegant-router 文件路由、UnoCSS 原子化 CSS、IndexedDB 离线缓存等现代特性。

---

## 核心特性

### AI 能力

- **Multi-Agent 协作**：4 个专业 Agent 分工协作，覆盖健康计划、营养、心理、安全审查
- **Function Calling**：模型自主决策调用工具，自动落库、查询、调整计划
- **SDUI 协议**：11 种 UI Widget，服务端驱动前端渲染，支持运动卡片、营养图表、进度环等
- **意图路由**：根据用户意图自动路由到合适的 Agent 和模型
- **知识库 RAG**：基于 Prompt 模板和知识文档的检索增强生成
- **SSE 流式输出**：AI 对话和计划生成支持实时流式响应，带游标断点续传
- **AI Copilot**：全局侧边栏 AI 助手，多会话管理，支持计划固化到数据库

### 安全合规

- **数据分级脱敏**：L1-L4 四级数据分级，敏感信息泛化后上传 AI
- **Prompt 注入防护**：6 类注入模式检测与过滤
- **医疗免责声明**：所有 AI 回复自动追加医疗免责
- **安全审查 Agent**：输出内容合规校验，禁止绝对化医疗用语
- **全链路审计日志**：AI 调用全链路记录，支持问题追溯
- **XSS 防护**：请求参数 HTML 转义 + JSON 反序列化过滤 + Servlet Filter 三层防护

### 高可用

- **多模型路由**：DeepSeek / Qwen-Max / GLM-4 / Moonshot-v1 / Ollama 五模型智能切换
- **熔断降级**：Resilience4j 熔断器 + 安全熔断器 + 规则引擎兜底
- **模型健康检查**：定时探测模型可用性，自动隔离故障模型
- **异步队列**：AI 任务异步执行，避免长时间阻塞
- **分级成本管控**：按订阅等级设置每日 Token 预算，超预算自动熔断

### 商业化

- **订阅计费**：分级订阅（Basic / Pro / Enterprise）
- **用量追踪**：按用户维度的 AI 调用用量统计
- **成本监控**：按模型/场景维度的 Token 消耗和费用核算，支持全局/用户/模型多维视图
- **开票退款**：完整的订单-发票-退款流程

### LLMOps

- **Prompt 版本管理**：Prompt 模板外置数据库，支持热更新和 A/B 测试
- **自动化评测**：LLM-as-a-Judge + RAGAS 评测体系
- **监控告警**：Prometheus + Grafana 指标监控，Webhook 告警
- **安全采样**：定时采样 AI 输出进行安全审查
- **成本仪表盘**：LLM 成本实时监控面板，支持模型状态查询和熔断器管理

### 前端体验

- **NaiveUI 组件库**：统一的设计语言，开箱即用的企业级 UI
- **响应式布局**：适配桌面端的自适应布局方案
- **离线缓存**：IndexedDB 四层存储（聊天/会话/SDUI/API），支持断网续用
- **全局 AI Copilot**：抽屉式侧边栏，随时随地与 AI 对话
- **SSE 流式渲染**：AI 回复逐字显示，支持中断/取消操作
- **elegant-router 路由**：基于文件的路由自动生成，类型安全

---

## 技术栈

### 后端

| 层次 | 技术 | 说明 |
|------|------|------|
| **框架** | Spring Boot 3.3.0 | 主框架 |
| **语言** | Java 17 | LTS 版本 |
| **ORM** | MyBatis-Plus 3.5.7 | 增强型 MyBatis |
| **数据库** | MySQL 8.0 | 主数据库，utf8mb4 编码 |
| **缓存** | Redis 7 + Lettuce | 缓存与会话管理 |
| **AI 框架** | LangChain4j 1.0.0-beta2 | Agent/Tool/Chain 编排 |
| **AI 模型** | DeepSeek（主力）/ Qwen-Max / GLM-4 / Moonshot-v1 / Ollama | 多模型路由降级 |
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

### 前端

| 层次 | 技术 | 说明 |
|------|------|------|
| **框架** | Vue 3.5 | 渐进式 JavaScript 框架 |
| **UI 组件库** | NaiveUI 2.44 | 企业级 Vue 3 组件库 |
| **构建工具** | Vite 8 | 下一代前端构建工具 |
| **语言** | TypeScript 6 | 类型安全的 JavaScript 超集 |
| **基础框架** | soybean-admin v2.2.0 | 企业级中后台解决方案 |
| **路由** | elegant-router | 基于文件的自动路由生成 |
| **CSS** | UnoCSS | 即时按需的原子化 CSS 引擎 |
| **状态管理** | Pinia | Vue 官方状态管理库 |
| **HTTP 客户端** | Axios（createFlatRequest） | 请求封装，返回 `{data, error}` 元组 |
| **图标** | Iconify | 统一图标方案 |
| **包管理** | pnpm | 快速、节省磁盘空间的包管理器 |
| **离线缓存** | IndexedDB | 浏览器端结构化存储 |
| **容器化** | Docker + Nginx | 前端静态资源部署 |

### 基础设施

| 组件 | 技术 | 说明 |
|------|------|------|
| **容器编排** | Docker Compose | MySQL + Redis + Backend + Frontend 四服务编排 |
| **反向代理** | Nginx | 前端静态资源 + API 反向代理 |
| **CI/CD** | Dockerfile（多阶段构建） | 构建与运行环境分离 |
| **环境变量** | .env 文件 | 统一的环境变量管理 |

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
│       ├── controller/                     # REST 控制器 (41 个)
│       ├── convert/                        # MapStruct 转换器 (8 个)
│       ├── dto/                            # 数据传输对象 (30+ 个)
│       ├── entity/                         # 数据库实体 (45 个)
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
│
├── frontend/                               # 前端 Vue 3 工程
│   ├── package.json                        # 依赖配置（pnpm）
│   ├── vite.config.ts                      # Vite 构建配置
│   ├── tsconfig.json                       # TypeScript 配置
│   ├── uno.config.ts                       # UnoCSS 配置
│   ├── .env                                # 环境变量
│   └── src/
│       ├── App.vue                         # 根组件
│       ├── main.ts                         # 应用入口
│       ├── components/                     # 全局组件
│       │   ├── GlobalCopilotDrawer.vue     # 全局 AI Copilot 抽屉
│       │   └── ...                         # 其他通用组件
│       ├── views/                          # 页面视图 (50+ 页面)
│       │   ├── statistics/Dashboard.vue    # 仪表盘（数据总览）
│       │   ├── health/                     # 健康管理（创建/表单/历史/报告）
│       │   ├── plan/                       # AI 计划（生成/列表/详情）
│       │   ├── exercise/                   # 运动记录
│       │   ├── food/                       # 饮食记录
│       │   ├── sleep/                      # 睡眠记录
│       │   ├── water/                      # 饮水记录
│       │   ├── blood-sugar/                # 血糖管理
│       │   ├── checkin/                    # 每日打卡
│       │   ├── chat/                       # AI 健康咨询
│       │   ├── community/                  # 社区互动
│       │   ├── billing/                    # 计费订阅
│       │   ├── admin/                      # 管理后台 (7 个页面)
│       │   ├── settings/                   # 设置页面
│       │   └── export/                     # 数据导出
│       ├── service/                        # API 服务层
│       │   ├── api/                        # 33 个 API 模块
│       │   │   ├── index.ts                # 统一导出
│       │   │   ├── auth.ts                 # 认证服务
│       │   │   ├── health.ts               # 健康数据
│       │   │   ├── plan.ts                 # AI 计划
│       │   │   ├── chat.ts                 # AI 对话
│       │   │   ├── admin.ts                # 管理后台
│       │   │   ├── llmCost.ts              # LLM 成本监控
│       │   │   ├── privacy.ts              # 隐私同意
│       │   │   └── ...                     # 其他 26 个模块
│       │   └── request/                    # 请求基础设施
│       │       ├── index.ts                # createFlatRequest 封装
│       │       └── ...
│       ├── store/                          # Pinia 状态管理
│       │   └── modules/
│       │       ├── auth/                   # 认证状态
│       │       ├── route/                  # 路由状态
│       │       ├── plan/                   # 计划状态
│       │       ├── tab/                    # 标签页状态
│       │       └── ...
│       ├── router/                         # 路由配置
│       │   └── elegant/
│       │       ├── imports.ts              # 视图懒加载导入
│       │       ├── routes.ts               # 路由定义
│       │       └── transform.ts            # 路径映射
│       ├── typings/                        # TypeScript 类型定义
│       │   ├── api/                        # API 类型声明
│       │   │   ├── admin.d.ts              # 管理端类型
│       │   │   ├── health.d.ts             # 健康数据类型
│       │   │   ├── bloodSugar.d.ts         # 血糖类型
│       │   │   └── ...
│       │   └── elegant-router.d.ts         # 路由类型声明
│       └── utils/                          # 工具函数
│           ├── sseClient.ts                # SSE 流式客户端
│           └── ...
│
├── sql/                                    # 数据库脚本
│   ├── init.sql                            # 基础建表
│   └── migrations/                         # 增量迁移 (12 个文件)
├── docker-compose.yml                      # 容器编排
├── Dockerfile                              # 后端多阶段构建
├── start.bat                               # Windows 一键启动脚本
├── .env.example                            # 环境变量模板
├── .gitignore
└── README.md                               # 本文档
```

---

## 快速开始

### 环境要求

**后端：**

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7+

**前端：**

- Node.js >= 20.19.0
- pnpm >= 9.0

**可选：**

- Docker & Docker Compose（容器化部署）

### 方式一：一键启动（Windows）

```bash
# 项目提供 start.bat 脚本，自动完成：
# - 环境检查（JDK/Maven/MySQL/Redis/Node.js）
# - Docker 回退（如本地无 MySQL/Redis 则自动启动容器）
# - 端口冲突检测
# - 数据库初始化
# - 后端/前端启动
# - 健康检查
start.bat
```

### 方式二：Docker Compose 部署

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
docker exec -i ai-health-mysql mysql -uroot -p${DB_PASSWORD} ai_health_system < sql/init.sql
```

### 方式三：本地开发

**启动后端：**

```bash
# 1. 启动 MySQL 和 Redis
docker-compose up -d mysql redis

# 2. 初始化数据库
mysql -uroot -p < sql/init.sql

# 3. 配置环境变量
set DB_PASSWORD=your-db-password
set REDIS_PASSWORD=your-redis-password
set JWT_SECRET=your-jwt-secret
set DEEPSEEK_API_KEY=sk-your-api-key

# 4. 启动后端
cd backend
mvn spring-boot:run
```

**启动前端：**

```bash
# 1. 安装依赖
cd frontend
pnpm install

# 2. 启动开发服务器
pnpm dev

# 3. 访问
# 前端：http://localhost:5173
# API 代理：/proxy-default → http://localhost:8080
```

**访问地址：**

| 服务 | 地址 |
|------|------|
| 前端应用 | http://localhost:5173 |
| 后端 API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Actuator | http://localhost:8081/actuator/health |

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DB_URL` | 数据库连接地址 | `jdbc:mysql://localhost:3306/ai_health_system` |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | - |
| `REDIS_HOST` | Redis 主机 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码 | - |
| `JWT_SECRET` | JWT 签名密钥 | - |
| `DEEPSEEK_API_KEY` | DeepSeek API Key | - |
| `CORS_ORIGINS` | CORS 允许的来源（逗号分隔） | `http://localhost:5173` |

---

## 架构设计

### 分层架构

```
┌──────────────────────────────────────────────────┐
│                   Controller 层                    │
│     接收 HTTP 请求，参数校验，调用 Service          │
│     (41 个 REST Controller)                        │
├──────────────────────────────────────────────────┤
│                    Service 层                      │
│     核心业务逻辑，Agent 编排，AI 调用              │
│     (60+ 个 Service 类)                            │
├──────────────────────────────────────────────────┤
│                    Mapper 层                       │
│     数据库访问，MyBatis-Plus BaseMapper            │
│     (36+ 个 Mapper 接口)                           │
├──────────────────────────────────────────────────┤
│                    Entity 层                       │
│     数据库实体映射                                  │
│     (45 个 Entity 类)                              │
└──────────────────────────────────────────────────┘
```

横切关注点：

- **Filter / Interceptor**：安全头注入、XSS 过滤、JWT 认证（3 个 Filter + JWT 拦截器）
- **AOP 切面**：权限校验（`@AdminOnly`）、防重复提交（`@NoRepeatSubmit`）、限流（`@RateLimit`）、订阅校验（`@RequiresSubscription`）
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

**Agent 工具集（5 个 Tool）**：

| 工具 | 功能 |
|------|------|
| `PlanTool` | 创建/查询/更新 AI 健康计划 |
| `HealthDataTool` | 查询用户健康数据（打卡、饮食、运动等） |
| `RecommendationTool` | 生成个性化推荐 |
| `AdjustmentTool` | 根据反馈调整计划强度和内容 |
| `SafetyTool` | 安全规则查询和合规校验 |

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
降级状态: ModelRouter → Qwen-Max / GLM-4 / Moonshot-v1 / Ollama
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
| **TierCircuitBreaker** | 按订阅等级的成本熔断器，超预算自动限流 |

### 前端架构

```
┌────────────────────────────────────────────────────────────────┐
│                       Vue 3.5 应用                              │
├────────────────────────────────────────────────────────────────┤
│  Views (50+ 页面)                                              │
│  ├── statistics/Dashboard    ├── health/ (4 pages)             │
│  ├── plan/ (3 pages)         ├── exercise/food/sleep/water/    │
│  ├── admin/ (7 pages)        ├── chat/ community/ billing/     │
│  └── settings/ export/ checkin/ blood-sugar/                   │
├────────────────────────────────────────────────────────────────┤
│  Components                                                     │
│  ├── GlobalCopilotDrawer (AI 侧边栏)                           │
│  └── 通用 UI 组件                                               │
├────────────────────────────────────────────────────────────────┤
│  Store (Pinia)                                                  │
│  ├── auth/ (认证 + 用户信息)   ├── route/ (路由状态)           │
│  ├── plan/ (计划 + 流式状态)   ├── tab/ (标签页管理)           │
│  └── app/ (应用全局配置)                                       │
├────────────────────────────────────────────────────────────────┤
│  Service Layer (33 个 API 模块)                                 │
│  ├── createFlatRequest → { data, error } 元组                  │
│  ├── SSE Client → 流式响应 + 游标断点续传                      │
│  └── Token 管理 → 115 分钟主动刷新 + 401 刷新队列              │
├────────────────────────────────────────────────────────────────┤
│  Router (elegant-router)                                        │
│  ├── 4 文件同步：d.ts / imports / routes / transform           │
│  ├── 56+ 路由键定义                                            │
│  └── Admin 路由角色守卫 (roles: ['admin'])                     │
├────────────────────────────────────────────────────────────────┤
│  Utils                                                          │
│  ├── sseClient.ts (SSE 流式 + 断点续传)                        │
│  ├── IndexedDB (离线缓存 4 层)                                  │
│  └── Token 工具 (AH_ 前缀 + localStg)                          │
└────────────────────────────────────────────────────────────────┘
```

**API 请求模式**：

前端采用 `createFlatRequest` 封装 Axios，所有 API 调用返回 `{ data, error }` 元组，避免 try/catch 嵌套：

```typescript
// 请求封装
const { data, error } = await fetchGetPlanList({ page: 1, size: 10 });
if (error) {
  // 错误处理
  return;
}
// data 已通过 transform 自动解包 response.data.data
```

**后端响应格式**：

```json
{
  "code": 200,
  "data": { ... },
  "msg": "success"
}
```

`code` 为数字类型，200 表示成功，`transform` 函数自动提取 `response.data.data`。

---

## 功能模块

### AI 智能服务

| 功能 | 说明 | 核心类 |
|------|------|--------|
| **AI 计划生成** | 基于用户画像生成分天运动/饮食/冥想计划，SSE 流式输出 | `AiPlanServiceImpl`, `PlanGenerateV2Service` |
| **AI 计划调整** | 根据打卡反馈动态调整计划，支持单日项替换/批量调整 | `PlanAdjustServiceImpl`, `AutoPlanAdjustService` |
| **AI 健康咨询** | 多轮对话式健康顾问，SSE 流式响应，支持会话管理 | `ChatServiceImpl`, `MemoryService` |
| **食物识别** | 上传图片识别食物并估算营养 | `FoodRecognitionServiceImpl` |
| **运动指导** | 个性化运动方案推荐 | `ExerciseGuidanceServiceImpl` |
| **个性化推荐** | 基于用户行为推荐健康内容 | `RecommendationServiceImpl` |
| **AI 反馈** | 收集并处理用户对 AI 服务的反馈 | `AiFeedbackServiceImpl` |
| **引导上手** | 新用户引导流程 | `OnboardingServiceImpl` |
| **计划固化** | 将 AI 对话中生成的计划持久化到数据库 | `SolidifyService` |

### 健康管理

| 功能 | 说明 |
|------|------|
| **身体测量** | 体重、BMI、围度等基础指标记录与趋势，实时 BMI/BMR 计算预览 |
| **血糖管理** | 血糖记录、异常标记（abnormalFlag）、测量类型区分、趋势分析 |
| **饮食记录** | 每日饮食摄入记录，宏量营养素（蛋白质/碳水/脂肪）分析 |
| **运动记录** | 运动执行记录、卡路里消耗统计、按日期查询 |
| **睡眠记录** | 睡眠时长、质量记录与分析 |
| **饮水记录** | 每日饮水追踪，日总量统计，多时间范围趋势（7/14/30 天） |
| **情绪记录** | 情绪状态记录与情绪趋势分析 |
| **每日打卡** | 健康任务打卡、补签、连续打卡统计、分页历史查询 |
| **健康报告** | 周期性健康综合报告生成，类型筛选、日期范围、分页、评分统计 |
| **目标里程碑** | 健康目标设定与里程碑追踪 |
| **仪表盘** | 今日/本周/本月健康数据汇总，14 个信息区块，8 个统计图表 |
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
| **账单汇总** | 按周期生成的费用明细（支持按天数查询） |
| **发票服务** | 电子发票申请与管理（invoiceType/invoiceTitle/orderNo） |
| **退款处理** | 退款申请与审核 |
| **企业方案** | 企业批量采购与定制方案 |

### 管理后台

| 功能 | 说明 |
|------|------|
| **用户管理** | 用户列表、详情弹窗、CSV 导出、状态管理 |
| **食物管理** | 食物字典维护 |
| **运动管理** | 运动字典维护 |
| **公告管理** | 系统公告发布与管理 |
| **通知推送** | 系统通知推送 |
| **审核管理** | 管理员审批流程（X-Admin-Id header） |
| **审计日志** | 管理员操作审计 |
| **规则建议** | 安全/合规规则建议管理 |
| **计划反馈** | 查看与处理用户计划反馈（关键词/评分客户端过滤） |
| **AI 反馈管理** | AI 服务反馈审核与处理 |
| **LLM 成本监控** | 全局/用户/模型多维成本仪表盘，熔断器状态查询，超预算用户管理 |

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

## 前端视图与服务

### 视图页面清单

前端包含 50+ 个业务页面，按功能域组织：

| 功能域 | 页面 | 说明 |
|--------|------|------|
| **仪表盘** | `statistics/Dashboard` | 数据总览，14 信息区块 + 8 图表 |
| **健康管理** | `health/Create`, `Form`, `History`, `Report` | 健康档案创建、编辑、历史、报告 |
| **AI 计划** | `plan/Generate`, `List`, `Detail` | AI 计划生成（SSE）、列表、详情 |
| **运动** | `exercise/Record` | 运动记录（增删改查 + 日统计） |
| **饮食** | `food/Record` | 饮食记录（宏量营养素 + 日总量） |
| **睡眠** | `sleep/Record` | 睡眠记录 |
| **饮水** | `water/Record` | 饮水记录（多时间范围趋势） |
| **血糖** | `blood-sugar/Index` | 血糖管理（异常标记 + 趋势） |
| **打卡** | `checkin/Calendar` | 打卡日历 + 分页历史 + 补签 |
| **AI 对话** | `chat/Index` | AI 健康咨询（SSE 流式） |
| **社区** | `community/Feed` | 社区帖子 + 评论 + 点赞 |
| **计费** | `billing/Index` | 订阅管理 + 账单 + 发票 |
| **通知** | `notification/Index` | 消息通知 |
| **导出** | `export/Index` | 数据导出 |
| **管理后台** | `admin/UserManage`, `FoodManage`, `ExerciseManage`, `Announcement`, `ApprovalManage`, `AiFeedback`, `RuleSuggestion`, `AuditLog`, `PlanFeedback`, `LlmCostMonitor` | 10 个管理页面 |
| **设置** | `settings/Profile`, `Privacy` | 个人设置 + 隐私同意 |

### API 服务模块

前端包含 **33 个 API 服务模块**，统一从 `src/service/api/index.ts` 导出：

| 模块 | 主要函数 | 对接 Controller |
|------|----------|-----------------|
| `auth.ts` | login, register, refreshToken | AuthController |
| `user.ts` | getProfile, updateProfile, uploadAvatar | UserController |
| `health.ts` | create/update/getHistory/getLatest | HealthController |
| `bodyMeasurement.ts` | CRUD 身体测量 | BodyMeasurementController |
| `plan.ts` | generate/getList/getDetail/updateDayItem/replaceDayItems/updateContent | AiPlanController, PlanDayItemController |
| `planAdjust.ts` | adjustPlan | PlanAdjustController |
| `planFeedback.ts` | submit/getByPlanId | PlanFeedbackController |
| `chat.ts` | createSession/getMessages/sendMessage(SSE) | ChatController |
| `exercise.ts` | getItems/getRecordsByDate/create/delete | ExerciseController |
| `food.ts` | getItems/getRecordsByDate/parseText/create | FoodController |
| `foodRecognition.ts` | recognizeImage | FoodRecognitionController |
| `sleep.ts` | CRUD 睡眠记录 | SleepController |
| `water.ts` | getRecords/getTotal/create | WaterController |
| `bloodSugar.ts` | getRecords/getTrend/create | BloodSugarController |
| `checkin.ts` | checkin/getPage/getStreak | CheckinController |
| `emotion.ts` | CRUD 情绪记录 | EmotionController |
| `community.ts` | posts/comments/likes | CommunityController |
| `billing.ts` | getSubscription/getHistory/applyInvoice | BillingController |
| `dashboard.ts` | getTodayStats | DashboardController |
| `statistics.ts` | getTrend/getSummary | StatisticsController |
| `healthReport.ts` | getReports/getById/generate | HealthReportController |
| `goalMilestone.ts` | CRUD 目标里程碑 | GoalMilestoneController |
| `recommendation.ts` | getRecommendations | RecommendationController |
| `notification.ts` | getNotifications/markRead | NotificationController |
| `notificationPreference.ts` | get/update偏好 | NotificationPreferenceController |
| `exerciseGuidance.ts` | getGuidance | ExerciseGuidanceController |
| `aiFeedback.ts` | submit/getList | AiFeedbackController |
| `admin.ts` | getUserList/getUserDetail/exportUserList/approve/reject + 食物/运动/公告/通知/审计 | 9 个 Admin Controller |
| `llmCost.ts` | getGlobalDailyCost/getUserDailyCost/getOverBudgetUsers/pause/resume/getModelStatus/getCircuitBreaker | LlmCostController |
| `privacy.ts` | getPrivacyConsent/updatePrivacyConsent | PrivacyController |
| `export.ts` | exportData | DataExportController |
| `enterprise.ts` | getPlans/subscribe | EnterprisePlanController |
| `onboarding.ts` | getSteps/complete | OnboardingController |

### 路由架构

前端使用 **elegant-router** 文件路由系统，要求 **4 个文件严格同步**，否则路由静默失效：

| 文件 | 作用 |
|------|------|
| `src/typings/elegant-router.d.ts` | RouteMap 类型定义，所有路由键到路径的映射 |
| `src/router/elegant/imports.ts` | 视图组件的 lazy import（`() => import('...')`） |
| `src/router/elegant/routes.ts` | 路由定义（组件引用、meta、重定向、角色守卫） |
| `src/router/elegant/transform.ts` | 路径映射（routeMap 对象） |

当前已配置 **56+ 个路由键**，覆盖所有业务页面。路由结构分为三类：

- **多级路由**（含 layout + 子路由）：health、plan、admin、settings、community
- **单级路由**（含重定向）：food、exercise、sleep、water、blood-sugar、checkin、emotion
- **独立页面**：billing、chat、export、notification

Admin 路由设置 `roles: ['admin']`，非管理员无法访问。

### 状态管理

使用 Pinia 管理应用状态，核心 store 模块：

| Store | 功能 |
|-------|------|
| `auth` | 用户认证、Token 管理（AH_ 前缀 + localStg）、用户信息、115 分钟主动刷新 |
| `route` | 路由状态、静态路由模式、角色过滤 |
| `plan` | AI 计划状态、流式生成进度、SSE 连接管理 |
| `tab` | 标签页管理、首页 Tab 初始化 |
| `app` | 应用全局配置（主题、布局、语言） |

### 离线缓存

前端使用 IndexedDB 实现四层离线缓存策略：

| 存储名 | TTL | 用途 |
|--------|-----|------|
| `chat_cache` | 7 天 | AI 对话历史缓存 |
| `session_cache` | 30 天 | 会话列表缓存 |
| `sdui_cache` | 1 天 | SDUI Widget 渲染数据缓存 |
| `api_cache` | 5 分钟 | 通用 API 响应缓存 |

### SSE 流式客户端

`sseClient.ts` 提供 SSE 流式通信能力：

```typescript
createSSEStream(url, data, {
  onMessage: (text: string) => void,  // 每条消息回调
  onDone: () => void,                  // [DONE] 信号回调
  onError: (error: Error) => void      // [ERROR] 信号回调
});
```

特性：

- **3 参数调用**：URL + 请求数据 + 回调对象
- **[DONE]/[ERROR] 处理**：先传递给 onMessage，再触发 onDone/onError
- **游标断点续传**：支持从上次中断位置恢复流式传输
- **中断/取消**：支持 AbortController 中止流

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

- **XSS 防护**：`XssFilter` + `JacksonXssConfig` + Servlet Filter 三重过滤
- **JWT 认证**：无状态 token，支持过期刷新，前端 115 分钟主动续期
- **BCrypt 密码加密**：用户密码使用 Spring Security Crypto 加密存储
- **接口限流**：`@RateLimit` 注解支持自定义限流策略
- **防重复提交**：`@NoRepeatSubmit` 注解防止表单重复提交
- **CORS 配置**：可控的跨域来源白名单
- **Token 前缀**：前端 Token 使用 `AH_` 前缀存储于 localStg

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
| `PlanDayItemController` | `/api/ai/plan-day-item` | 计划日项管理（更新/替换） |
| `AiFeedbackController` | `/api/ai/feedback` | AI 反馈 |
| `ChatController` | `/api/chat` | AI 健康咨询对话（SSE） |
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
| `AdminUserController` | `/api/admin/user` | 用户管理（列表/详情/导出） |
| `AdminFoodController` | `/api/admin/food` | 食物字典管理 |
| `AdminExerciseController` | `/api/admin/exercise` | 运动字典管理 |
| `AdminAnnouncementController` | `/api/admin/announcement` | 公告管理 |
| `AdminNotificationController` | `/api/admin/notification` | 通知推送 |
| `AdminApprovalController` | `/api/admin/approval` | 审批管理（X-Admin-Id） |
| `AdminAuditLogController` | `/api/admin/audit-log` | 审计日志 |
| `AdminPlanFeedbackController` | `/api/admin/plan-feedback` | 计划反馈管理 |
| `AdminRuleSuggestionController` | `/api/admin/rule-suggestion` | 规则建议管理 |

### 商业与运维 API（6 个 Controller）

| Controller | 路径前缀 | 功能 |
|------------|----------|------|
| `BillingController` | `/api/billing` | 订阅/计费 |
| `EnterprisePlanController` | `/api/enterprise` | 企业方案 |
| `RefundAndInvoiceController` | `/api/billing` | 退款/发票 |
| `DataExportController` | `/api/export` | 数据导出 |
| `LlmOpsController` | `/api/llmops` | LLMOps 运维管理 |
| `LlmCostController` | `/api/llm-cost` | LLM 成本监控（全局/用户/模型/熔断器） |

---

## 数据库设计

系统包含 **40+ 张数据表**，按业务域划分。数据库初始化脚本位于 `sql/` 目录，包含基础建表 `init.sql` 和 12 个增量迁移文件。

### 用户体系

| 表名 | 说明 |
|------|------|
| `sys_user` | 用户主表 |
| `user_profile` | 用户健康画像（病史、过敏史、性别、目标体重、家族病史、用药情况） |
| `user_memory` | 用户记忆（长期偏好、习惯） |
| `user_usage` | 用户 AI 调用用量 |
| `subscription` | 订阅信息 |
| `privacy_consent` | 隐私同意记录 |

### AI 计划体系

| 表名 | 说明 |
|------|------|
| `ai_plan` | AI 主计划 |
| `ai_plan_detail` | 计划每日明细（运动/饮食任务项） |
| `ai_plan_day_item` | 计划日项（可单独更新/替换） |
| `ai_plan_feedback` | 用户反馈（难度/强度/满意度） |
| `ai_feedback` | AI 服务反馈 |
| `prompt_template` | Prompt 模板（支持 A/B 测试和热更新） |

### 健康记录体系

| 表名 | 说明 |
|------|------|
| `daily_checkin` | 每日打卡（支持补签） |
| `diet_record` | 饮食执行记录（含宏量营养素） |
| `exercise_record` | 运动执行记录 |
| `sleep_record` | 睡眠记录 |
| `water_record` | 饮水记录 |
| `blood_sugar` | 血糖记录（含异常标记、测量类型） |
| `body_measurement` | 身体测量（体重、围度、BMI/BMR） |
| `health_record` | 健康主记录 |
| `health_report` | 健康报告（含评分） |
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
| `exercise_rules` | 运动规则（降级引擎使用） |
| `announcement` | 系统公告 |
| `notification` | 系统通知 |
| `notification_preference` | 通知偏好 |
| `admin_approval` | 管理员审批 |
| `rule_suggestion` | 规则建议 |

### 计费与运维

| 表名 | 说明 |
|------|------|
| `billing_record` | 计费记录 |
| `invoice` | 发票 |
| `refund` | 退款 |
| `enterprise_plan` | 企业方案 |
| `llm_cost_daily` | LLM 每日成本统计 |
| `model_health_status` | 模型健康状态 |

---

## 部署指南

### Docker Compose 部署

`docker-compose.yml` 定义了 4 个服务：

| 服务 | 镜像 | 端口 |
|------|------|------|
| `mysql` | mysql:8.0 | 3306 |
| `redis` | redis:7 | 6379 |
| `backend` | 自定义构建 | 8080, 8081 |
| `frontend` | nginx:alpine | 80 |

```bash
# 构建并启动
docker-compose up -d --build

# 查看日志
docker-compose logs -f backend

# 数据库初始化
docker exec -i ai-health-mysql mysql -uroot -p${DB_PASSWORD} ai_health_system < sql/init.sql
```

### 后端独立部署

```bash
cd backend
mvn clean package -DskipTests
java -jar target/ai-health-system-1.0.0.jar \
  --spring.profiles.active=prod \
  --DB_PASSWORD=${DB_PASSWORD} \
  --DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY}
```

### 前端独立部署

```bash
cd frontend
pnpm install
pnpm build

# 产物在 dist/ 目录，部署到 Nginx 或其他静态服务器
# Nginx 需配置 SPA 路由和 API 反向代理
```

### Nginx 配置参考

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_buffering off;            # SSE 需要
        proxy_cache off;                # SSE 需要
        proxy_read_timeout 300s;        # SSE 长连接
    }
}
```

---

## 开发指南

### 前端开发

```bash
cd frontend
pnpm install    # 安装依赖
pnpm dev        # 开发模式（HMR）
pnpm build      # 生产构建
pnpm lint       # 代码检查
pnpm typecheck  # 类型检查
```

**Vite 开发代理**：前端通过 `/proxy-default` 前缀将 API 请求代理到 `http://localhost:8080`，路径自动重写去除前缀。

### 添加新页面的步骤

1. 在 `src/views/` 下创建 Vue 组件文件
2. 在 `src/service/api/` 下创建对应的 API 模块
3. 在 `src/typings/api/` 下定义 TypeScript 类型
4. **同步更新 4 个路由文件**（elegant-router 要求）：
   - `src/typings/elegant-router.d.ts` → RouteMap 添加路由键
   - `src/router/elegant/imports.ts` → 添加 lazy import
   - `src/router/elegant/routes.ts` → 添加路由定义
   - `src/router/elegant/transform.ts` → 添加路径映射

### 后端开发

```bash
cd backend
mvn spring-boot:run              # 开发模式
mvn clean package                # 打包
mvn checkstyle:check             # 代码风格检查
```

### 数据库迁移

增量迁移脚本放在 `sql/migrations/` 目录，按版本序号命名（如 `V001__add_xxx.sql`）。

---

## 相关文档

| 文档 | 说明 |
|------|------|
| [PROJECT_README.md](PROJECT_README.md) | 项目详细技术文档 |
| [ARCHITECTURE_OPTIMIZATION_PLAN.md](ARCHITECTURE_OPTIMIZATION_PLAN.md) | 架构优化计划 |
| [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md) | 开发完善路线图 |

---

## 许可证

本项目基于 MIT 许可证开源。
