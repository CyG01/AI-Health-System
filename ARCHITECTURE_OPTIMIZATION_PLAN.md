# AI-Health-System 架构优化计划（v2.0）

> 从"原型级可用"到"商用级优秀"的系统性升级路线图。
>
> 版本：v2.0（基于 v1.0 评审反馈，补充量化指标、高可用设计、成本模型、合规闭环、用户体验细节、数据治理）
>
> 当前状态：Spring Boot 单体 + MySQL + Redis，具备多智能体、LLMOps、SDUI、熔断弹性设计。
> 目标状态：高并发、高安全、高可用、高可观测、成本可控的 AI-Native 健康管理系统。

---

## 零、核心非功能量化指标（优化目标基线）

> 没有量化指标的优化计划是盲目的。以下指标作为每个维度优化完成的验收标准。

### 全局目标基线

| 维度       | 优化前（估算）                        | 优化目标                           | 验证方法                           |
|------------|---------------------------------------|------------------------------------|------------------------------------|
| **性能**   | 血糖年度趋势查询 P99 ≈ 5s（MySQL）    | P99 ≤ 200ms                        | JMeter 压测 + SkyWalking 链路分析  |
| **性能**   | 知识库检索首Token延迟 P99 ≈ 2s（LIKE）| P99 ≤ 500ms（含Rerank）            | Prometheus + Grafana 看板          |
| **并发**   | 估计支撑 500~1000 TPS（单体）          | 支撑 10,000 TPS（集群）            | 集群压测 + 限流阈值验证            |
| **成本**   | 人均 LLM 调用约 0.3~0.5 元/天          | ≤ 0.1 元/天（智能路由+本地模型）   | 成本监控面板 + 月度核算            |
| **安全**   | 敏感字段明文存储                      | 敏感字段加密覆盖率 100%            | 代码扫描 + 渗透测试                |
| **可用性** | 单机部署，MTTR ≈ 1h                    | 集群部署，MTTR ≤ 10min，可用率 99.9% | 故障注入 + 容灾演练每月一次     |
| **合规**   | 无合规对标                            | 通过等保三级技术差距分析           | 合规检查清单 + 第三方渗透测试      |
| **质量**   | 6 个测试文件，无 CI/CD                 | 单元测试覆盖率 ≥ 70%，CI/CD 流水线 | SonarQube + JaCoCo                 |
| **体验**   | 无量化                              | AI回复满意度 ≥ 85%，核心流程完成率 ≥ 90% | NPS 问卷 + 埋点分析            |

### 各阶段分目标

| 阶段     | 性能指标                        | 安全指标                    | 成本指标                        |
|----------|--------------------------------|-----------------------------|--------------------------------|
| Phase 1  | AI 异步化后 P99 ≤ 3s（端到端） | 敏感字段加密覆盖率 100%      | -                              |
| Phase 2  | 时序查询 P99 ≤ 200ms            | -                           | 知识检索一次模型调用成本降 50% |
| Phase 3  | 单元测试覆盖率 ≥ 70%            | -                           | AI 月均成本可追踪到用户粒度    |
| Phase 4  | 并发支撑 10,000 TPS             | 等保三级技术差距分析通过     | 人均 LLM 调用成本 ≤ 0.1 元/天  |
| Phase 5  | P99 端到端延迟 ≤ 1s（AI 对话）  | 合规审计日志 100% Hash 链化 | 本地模型替代 ≥ 30% 调用量      |

---

## 维度总览

| 维度                | 当前状态                                  | 目标状态                                         | 优先级 |
|---------------------|-------------------------------------------|--------------------------------------------------|--------|
| **架构演进**        | 单体 + 内存队列 + MySQL 存时序数据        | 消息队列解耦 + 时序库集群 + 读写分离             | P0     |
| **LLMOps 深化**     | MySQL 知识库 + 单模型路由                 | 向量库集群检索 + RRF 混合检索 + 智能多模型路由   | P0     |
| **安全合规**        | 关键词正则 + 明文日志                     | 字段级加密 + Hash链防篡改审计 + 医疗红线三层拦截 + 合规对标 | P0 |
| **UX & 可观测性**   | Web SDUI + 基础 SSE                       | 多端 SDUI + 离线缓存 + 断点续传 + 全链路追踪 + 体验度量 | P1 |
| **DevOps & 质量**   | 6 个测试 + 简单 docker-compose             | 分层测试体系 + SonarQube + CI/CD 蓝绿部署        | P1 |
| **成本精细化**      | 无成本追踪                                | 按用户/意图/模型维度的成本看板 + 降本策略验证     | P1 |
| **数据治理**        | 无生命周期管理                            | 冷热分离 + 过期清理 + 数据质量校验                | P1 |

---

## 维度一：架构演进（从单体到高并发高可用架构）

> **现状诊断**：系统基于 Spring Boot 单体部署，所有健康体征数据（血糖、睡眠、体测、饮水）均存储在 MySQL 中，AI 调用通过 `AiCallQueueService` 应用层内存队列处理。当用户量达到万级时，高频打卡和实时 AI 对话将成为瓶颈。无中间件集群部署，存在严重单点故障风险。

### 1.1 引入时序数据库（Time-Series DB）

**涉及文件**：`BloodSugar.java`、`SleepRecord.java`、`BodyMeasurement.java`、`WaterRecord.java`、`ExerciseRecord.java` 及其 Service/Controller

**量化目标**：年度血糖趋势聚合查询 P99 从 **5s → ≤ 200ms**，存储空间为 MySQL 的 **1/10**。

**问题**：
- 健康体征是典型的高频时间序列数据（每天多次血糖、持续心率/步数）
- MySQL 做时间范围聚合查询（如"过去一年的血糖波动趋势"）性能极差
- 体征数据表随用户增长无限膨胀，MySQL 分表维护成本高
- 单节点部署，无高可用保障

**方案**：

```
MySQL（业务数据）                TDengine 集群（时序数据）
├── sys_user                    ┌─→ TDengine Master（写 + 读）
├── chat_message                │
├── subscription                ├─→ TDengine Slave-1（只读副本）
├── ...                         └─→ TDengine Slave-2（只读副本）
                                     ↓ 每 1 小时增量备份 + 每日全量快照

双写过渡期（2 周）
  MySQL ←→ TDengine (写)
  Dashboard/Report 查询：优先 TDengine，降级 MySQL
```

**实施步骤**：

1. **部署 TDengine 3.x 集群**（1 Master + 2 Slave），配置定时快照备份
2. **新建 `tsdb` 模块**，封装 TDengine 的连接池（含读写分离 + 故障切换）
3. **双写过渡期**（≥ 2 周）：
   - 在现有 Service 中增加 AOP 切面，体征数据同时写 MySQL 和 TDengine
   - 聚合查询走 TDengine，失败时 **自动降级到 MySQL**（兜底逻辑）
   - 对比双写一致性，确认无数据偏差后再下线 MySQL 时序表
4. **监控告警**：TDengine 写入延迟超 50ms、集群切换时自动通知

**高可用设计**：
- Master 宕机 → Slave 自动选举提升（TDengine 3.x 原生支持）
- 降级兜底：TDengine 整体不可用时，`StatisticsService` 自动回退到 MySQL（需保留 MySQL 时序表至少 3 个月作为冷备）
- 备份策略：每日全量快照 + 每小时增量备份，保留 30 天

**关键代码变更**：

```java
// 新增：config/TdengineConfig.java
@Configuration
public class TdengineConfig {
    @Bean
    public TSDBConnectionPool tdenginePool(
            @Value("${tdengine.master.url}") String masterUrl,
            @Value("${tdengine.slaves.urls}") List<String> slaveUrls) {
        // 带故障切换的连接池
        return new TSDBConnectionPool(masterUrl, slaveUrls);
    }
}

// 改造：StatisticsServiceImpl.java — 带降级兜底的查询
public List<BloodSugarTrend> getYearlyTrend(Long userId) {
    try {
        return tdenginePool.query(
            "SELECT AVG(value) FROM blood_sugar " +
            "WHERE userId=? AND ts >= NOW - 365d INTERVAL(1d)", userId);
    } catch (Exception e) {
        log.warn("TDengine 查询失败，降级到 MySQL", e);
        return bloodSugarMapper.selectYearlyTrend(userId); // 兜底
    }
}
```

---

### 1.2 消息队列解耦 AI 任务

**涉及文件**：`resilience/AiCallQueueService.java`、`HealthReportService.java`、`MemoryService.java`

**量化目标**：AI 任务异步化后，Controller 层 RT（响应时间）从 30s → **≤ 200ms（立即返回 202）**。

**问题**：
- `AiCallQueueService` 是 JVM 内存队列，服务重启丢失所有排队任务
- 健康报告生成、RAG 知识库构建耗时 10-30 秒，阻塞了数据库连接池
- 多实例部署时队列无法共享，Consumer 无法水平扩展

**方案**：

```
HTTP 请求 → Controller → RocketMQ Producer → 立即返回 202 + taskId
                                ↓
                       Consumer Group（可扩展至 N 个实例）
                                ↓
                       异步执行 LLM 调用 / 报告生成
                                ↓
                       结果写入 DB + Redis → WebSocket 通知前端

失败处理：
  - 首次失败 → 指数退避重试（1s, 2s, 4s, 8s, 16s）
  - 重试 5 次仍失败 → 进入死信队列
  - 死信 → 人工排查 + 手动重放
```

**实施步骤**：

1. **部署 RocketMQ 集群**（3 节点：1 NameServer + 2 Broker），开启持久化 + 同步刷盘
2. **定义 Topic 与消费者组**：

   | Topic                    | 消费者组              | 并发度 | 说明               |
   |--------------------------|----------------------|--------|--------------------|
   | `health-report-generate` | `report-consumers`   | 4      | 健康报告生成       |
   | `ai-plan-generate`       | `plan-consumers`     | 4      | AI 计划生成        |
   | `knowledge-index-build`  | `knowledge-consumers`| 2      | 知识库索引构建     |
   | `llm-chat-response`      | `chat-consumers`     | 8      | LLM 对话响应       |

3. **改造 `AiCallQueueService`**：
   - 原 `BlockingQueue` 替换为 RocketMQ Template
   - 增加死信队列处理逻辑
4. **异步结果通知**：
   - Consumer 完成后写 Redis（`task:{taskId}:result`）+ WebSocket 推送
   - 前端 30 秒内轮询 Redis，超时展示"处理中"状态

**高可用设计**：
- RocketMQ 3 节点集群 → 任意 1 节点宕机不影响生产消费
- 消息持久化 → Broker 重启后消息不丢
- 死信队列 → 消费失败 5 次自动转入，避免无限重试
- 容灾演练：每月模拟 1 次 Broker 宕机，验证 Consumer 自动切换

---

### 1.3 读写分离与数据分片

**涉及文件**：所有 Mapper 层

**量化目标**：读吞吐提升 **3x**（MySQL 从库分担），`chat_message` 单表写入 **≤ 500 万行**。

**方案**：

```
                        ┌─→ MySQL Master（写）
ShardingSphere-Proxy ───│         ↓
                        │    user_usage 单表
                        │
                        ├─→ MySQL Slave-1（读）
                        └─→ MySQL Slave-2（读）
                              ↓ + 分片  ↓
                        chat_message_{0..7}  按 user_id % 8 分 8 张表
```

**实施步骤**：

1. 引入 `shardingsphere-jdbc-core`，配置 `sharding.yml`
2. 读写分离：`@Transactional(readOnly=true)` 的方法自动走从库
3. 分片：`chat_message` 按 `user_id` 一致性 Hash 分 8 表
4. 灰度上线：先在测试环境验证分片路由正确性，生产单表行数 > 500 万时自动触发分片迁移

**微服务拆分判断标准**（防止过早拆分）：

| 条件                                       | 阈值                   | 判定                   |
|--------------------------------------------|------------------------|------------------------|
| 团队规模                                   | > 10 人                 | 开始考虑按业务域拆分   |
| 单个接口 QPS 达到单实例瓶颈                 | > 5,000 QPS             | 该接口独立服务         |
| 某业务域变更频率远超其他域                 | > 3x                    | 独立部署该域           |
| 某表数据量导致单库无法支撑                 | > 500 GB                | 该域数据独立           |

**接口契约管理**（为微服务拆分做准备）：
- 所有 Controller 接口版本号化：`/api/v1/health` → `/api/v2/health`
- 使用 SpringDoc 生成 OpenAPI 3.0 Schema，作为服务间契约
- 未来引入 Pact 做契约测试，保证 Consumer 和 Provider 兼容

---

## 维度二：大模型工程化（LLMOps）深化

> **现状诊断**：知识库 `KnowledgeDoc` 存储在 MySQL，通过 `KnowledgeService` 做 SQL `LIKE` 模糊搜索；模型路由已支持四厂商降级但缺少意图分级和成本意识。

### 2.1 向量数据库 + 混合检索

**涉及文件**：`service/KnowledgeService.java`、`service/impl/KnowledgeServiceImpl.java`、`entity/KnowledgeDoc.java`

**量化目标**：知识检索首 Token 延迟 P99 从 **2s → ≤ 500ms**（含 Rerank），召回 Top-5 准确率从 SQL LIKE 的约 60% → **≥ 85%**。

**方案**：

```
用户问题 → Embedding（BGE-M3 / DeepSeek Embedding）
                ├─→ 向量检索（Qdrant，Top-50）
                └─→ BM25 关键词检索（Qdrant 稀疏向量，Top-50）
                              ↓
                         RRF 融合（Reciprocal Rank Fusion）
                              ↓
                         Rerank 模型（BGE-Reranker-v2 / Cohere Rerank）
                              ↓
                         Top-5 最相关文档 → 拼入 LLM Context
```

**实施步骤**：

1. **部署 Qdrant 集群**（2 节点 + 分片 + 副本）：轻量、Rust 编写、带稀疏向量支持（可替代 ES）
2. **新建 `vector` 模块**：
   - `VectorStoreService`：封装 Qdrant CRUD + 混合检索
   - `EmbeddingService`：调用 BGE-M3 本地模型或 DeepSeek Embedding API
3. **实现混合检索**：RRF 公式 `score(doc) = Σ 1/(k + rank_i)`，k=60
4. **实现 Rerank**：先用 Cohere Rerank API 快速上线，再评估本地 BGE-Reranker
5. **改造 `KnowledgeServiceImpl.search()`**：全量切换
6. **知识文档入库**：`KnowledgeDoc` 新增/修改时，同步更新 Qdrant 向量 + 稀疏向量

**高可用设计**：
- Qdrant 集群 + 副本 → 单节点宕机无感知切换
- 定时快照备份 → 每日全量 + 每小时增量备份到 S3/OSS
- 降级兜底：Qdrant 不可用时，降级到 MySQL LIKE + Redis 热词缓存（用户高频问题的缓存结果）

**数据质量校验**：
- `KnowledgeDoc` 入库前：字段完整性校验（标题、正文、分类非空）
- 向量化后一致性校验：随机抽检 1% 文档，验证 `cos_similarity(原文 → Embedding → 检索结果)` ≥ 0.85

---

### 2.2 基于意图和成本的智能模型路由

**涉及文件**：`resilience/ModelRouter.java`、`agent/orchestrator/IntentRouter.java`、`monitor/DeepSeekCostMonitor.java`

**量化目标**：人均 LLM 调用成本从 **0.3~0.5 元/天 → ≤ 0.1 元/天**（智能路由 + 本地模型替代 30% 请求）。

**方案**：

```
用户请求 → IntentRouter 意图分类
    ├── ModelTier.LOW（闲聊/简单查询）     → Ollama-本地 Llama3-8B（成本≈0）
    ├── ModelTier.MEDIUM（饮食识别/运动记录）→ 千问-Turbo（成本低）
    ├── ModelTier.HIGH（医学指标分析/计划生成）→ DeepSeek-R1 / GPT-4o
    └── ModelTier.CRITICAL（心理危机干预）    → 最高安全模型 + SafetyReviewAgent 强制审核

每个 Tier 独立的熔断/重试策略：
  - LOW 不可用 → 升级到 MEDIUM
  - MEDIUM 不可用 → 升级到 HIGH
  - HIGH 不可用 → 输出固定话术："系统繁忙，请稍后重试"
  - ALL 不可用 → 所有请求返回预设安全应答（最长兜底链）
```

**实施步骤**：

1. **在 `IntentRouter` 定义 `ModelTier` 枚举**：LOW / MEDIUM / HIGH / CRITICAL
2. **改造 `ModelRouter.chat(ModelTier tier, ...)`**：每个 Tier 独立模型池 + 独立熔断状态
3. **成本追踪器扩展**：`MultiModelCostMonitor` 按 `模型 × 意图 × 用户` 维度统计 Token 用量和费用
4. **本地模型部署**：Ollama + Llama3-8B（或 Qwen2-7B），处理 LOW tier 请求
5. **成本报警**：单用户日调用成本 > 1 元时，自动暂停并发送通知

**极端场景降级链**：

```
所有云端模型全部不可用（极端场景）
    ↓
LOW tier     → 本地 Ollama 模型（永远在线）
MEDIUM tier  → 降级到本地 Ollama 模型
HIGH tier    → 返回预设安全应答："系统繁忙，请稍后重试。如有紧急健康问题，请拨打 120。"
CRITICAL tier → 返回预设安全应答 + 实时通知管理员

预设应答模板（必须预置在代码中，不依赖任何外部服务）：
  - "系统繁忙" + 急救电话
  - "您的健康档案已保存，AI建议将在恢复后生成" + 客服入口
```

---

### 2.3 Agent 记忆体系升级

**涉及文件**：`entity/UserMemory.java`、`service/impl/MemoryServiceImpl.java`、`service/impl/ChatServiceImpl.java`

**量化目标**：AI 对话中对用户历史健康状况的引用准确率从约 60%（当前单表）→ **≥ 90%**。

**方案**：

```
短期工作记忆（Redis，TTL 30min）
    ├── 当前对话上下文（最近 20 轮，带 token 计数）
    └── 本次会话的临时偏好

长期事实记忆（MySQL JSON 字段 / Neo4j）
    ├── 用户基础画像（年龄、性别、身高体重 → UserProfile）
    ├── 疾病史时间线（2019高血压 → 2021加重 → 2023稳定）
    ├── 运动偏好演化（2024跑步 → 2025游泳，因膝盖损伤）
    └── 饮食过敏/禁忌

行为模式记忆（用户行为画像表）
    ├── 打卡规律（工作日 7:30，周末 10:00）
    ├── AI 咨询偏好时段
    └── 健康目标完成率趋势（月/周）
```

**实施步骤**：

1. **短期记忆**：`ChatServiceImpl` 每次对话后写入 Redis List（左 PUSH，LRIM 保留 40 条），TTL 30min
2. **长期事实记忆**：`UserMemory` 表增加 `memory_type`（FACT/PREFERENCE/TIMELINE）+ `metadata` JSON 字段
3. **记忆召回 Pipeline**：每次 AI 调用前执行：
   ```
   MemoryService.buildContext(userId) →
     1. 从 Redis 取最近 20 轮对话（短期）
     2. 从 MySQL 取用户健康画像 + 关键 Timeline（长期事实）
     3. 从 MySQL 取最近 7 天行为摘要（行为模式）
     4. 拼接为结构化 Context JSON → 注入 System Prompt
   ```
4. **记忆更新**：AI 回复后，从回复中提取关键事实（LLM 提取 → 结构化 → 写入长期记忆）

---

## 维度三：医疗级安全与合规

> **现状诊断**：`SafetyCheckerService` 基于关键词正则 + MySQL 规则表，`AiCallAuditLog` 和 `SafetyReviewLog` 明文存储。缺少字段级加密、防篡改审计、合规对标。

### 3.1 敏感数据字段级加密

**涉及文件**：`HealthRecord.java`、`EmotionRecord.java`、`UserProfile.java`、`ChatMessage.java`

**量化目标**：敏感字段加密覆盖率 **100%**，通过代码扫描 + 渗透测试验证。

**方案**：

**实施步骤**：

1. **创建 `config/EncryptionConfig.java`**：密钥从环境变量 `AES_ENCRYPTION_KEY` 或云 KMS 获取，禁止硬编码
2. **创建 `util/AesEncryptor.java`**：AES-256-GCM（带认证标签，防篡改 + 防泄漏）
3. **MyBatis TypeHandler**：`EncryptedStringTypeHandler`，持久层透明加解密
4. **标记敏感字段**（`@TableField(typeHandler = EncryptedStringTypeHandler.class)`）：

   | Entity             | 字段                        | 加密原因               |
   |--------------------|-----------------------------|------------------------|
   | `HealthRecord`     | `diseaseHistory`            | 用户疾病史             |
   | `HealthRecord`     | `allergyHistory`            | 过敏史                 |
   | `EmotionRecord`    | `content`                   | 情绪记录内容           |
   | `ChatMessage`      | `content`                   | AI 对话内容            |
   | `UserProfile`      | `realName`                  | 用户真实姓名           |

5. **密钥轮换策略**：每 90 天生成新密钥，历史数据用归档密钥解密后重新加密
6. **加密失败报警**：`AesEncryptor` 加密/解密异常时，记录 Alert 并抛出特定异常

---

### 3.2 不可篡改审计日志

**涉及文件**：`entity/AdminAuditLog.java`、`entity/SafetyReviewLog.java`、`service/AuditLogService.java`

**量化目标**：100% 高危操作日志（管理员操作、AI 安全审查、敏感数据访问）纳入 Hash 链，可验证不可篡改性。

**方案**：

```
高危操作 → 写 MySQL 审计日志表
    ↓
计算 SHA-256(prev_hash + JSON.stringify(log_entry)) → hash_chain
    ↓
每 100 条 OR 每 10 分钟 → 计算 Merkle Root
    ↓
Merkle Root 写入阿里云 OSS（WORM 锁定 / 合规保留策略）
    ↓
定期审计：重新逐条计算 Hash → 比对 OSS 中的 Root → 不一致 = 被篡改
```

**实施步骤**：

1. **`AuditLogService` 增加 Hash 链写入**：每条日志记录 `prev_hash` → `hash_chain`
2. **`MerkleRootAnchoringJob`**：`@Scheduled(cron = "0 */10 * * * ?")`，每 10 分钟将 Merkle Root 写入 OSS
3. **轻量替代方案**（不引入区块链 / OSS WORM）：
   - MySQL 审计表使用独立数据库账号（只写，不可改不可删）
   - `binlog` 开启并输出到独立日志服务器
   - 审计日志表增加 `signature` 字段（HMAC-SHA256 签名），私钥由安全管理员离线保管

**审计日志保留策略**（合规要求：等保三级 ≥ 6 个月）：
- MySQL 审计表：保留 6 个月热数据
- OSS / 日志服务器：保留 3 年（归档存储，成本低）

---

### 3.3 医疗红线拦截强化

**涉及文件**：`service/SafetyCheckerService.java`、`agent/SafetyReviewAgent.java`、`util/MedicalSafetyDict.java`

**量化目标**：AI 可能的危险回复拦截率 **≥ 99.9%**（通过取真集测试验证）。

**方案**：

```
三层拦截架构：

Layer 1: 正则硬匹配（< 1ms）
  ├── MedicalSafetyDict 禁止词汇表（处方药名、诊断术语、剂量建议）
  ├── 匹配 → 直接拦截，返回预设安全话术
  └── 不匹配 → 放行

Layer 2: 判别式小模型（~50ms）
  ├── 微调过的 BERT / RoBERTa 二分类模型（安全 ≈ 0 / 不安全 ≈ 1）
  ├── 评分 ≥ 0.7 → 拦截
  └── 评分 < 0.7 → 放行

Layer 3: LLM 最终审查（~500ms）
  ├── SafetyReviewAgent 做语义级审查
  ├── 审查 Prompt："以下回复是否存在医疗风险？是否包含诊断/处方/剂量建议？"
  └── 存在风险 → 拦截，输出修改后安全版本
```

**实施步骤**：

1. **创建 `util/MedicalSafetyDict.java`**：维护禁止词汇表（药物名、诊断术语、剂量相关词汇），支持热更新
2. **部署安全判别模型**：下载 `medical-safety-bert` 或自行微调，ONNX Runtime 推理
3. **`SafetyReviewAgent` 必须在所有 AI 回复前执行**，不能有绕过路径
4. **前端 `MedicalDisclaimerBanner`**：确认在以下页面均展示：
   - AI 对话页（`ChatBot.vue`）
   - AI 计划生成页（`Generate.vue`）
   - 健康报告页（`Report.vue`）
5. **拦截事件追溯**：
   - 每次拦截记录 `SafetyReviewLog`（含用户输入、AI 原始输出、拦截原因）
   - 每周统计拦截 Top-10 模式，反向更新 `MedicalSafetyDict`

---

### 3.4 合规对标与用户授权

**目标**：将优化计划对齐到具体法规要求，并建立用户数据授权流程。

**合规对标表**：

| 法规/标准                          | 核心要求                                 | 优化计划对应项                     | 验收方式                   |
|------------------------------------|------------------------------------------|------------------------------------|----------------------------|
| 《个人信息保护法》                 | 最小必要原则、数据加密、用户知情同意      | 3.1 字段级加密 + 3.5 数据授权     | 隐私合规审查              |
| 《健康医疗数据安全指南》           | 医疗数据分类分级、数据脱敏、审计追溯      | 3.1 加密 + 3.2 Hash链审计         | 合规差距分析              |
| 等保三级（GB/T 22239-2019）        | 身份鉴别、访问控制、安全审计、数据保密性  | 3.1-3.3 + 日志保留 6 个月         | 等保测评机构评估          |
| HIPAA（如需国际化）                | PHI 加密、BA 协议、访问控制               | 3.1-3.3 + 数据传输 TLS 1.3       | HIPAA 自评估              |

**用户数据授权流程**：

```
[用户首次登录]
    ↓
隐私政策展示 + 授权勾选
    ├── □ 同意收集健康数据用于 AI 分析（必选）
    ├── □ 同意使用脱敏数据用于模型优化（可选）
    └── □ 同意接收 AI 健康改善建议（可选）
    ↓
用户可随时在"设置 → 隐私"中撤销授权
    ↓
撤销后：该用户的数据从模型训练集中移除（或标记为不可用）
```

**实施步骤**：

1. **`UserProfile` 增加授权字段**：`dataConsentForModel`、`dataConsentForRecommend`
2. **`PrivacyController`**：提供授权查询/修改接口
3. **数据最小化**：查询用户健康档案时遵循"最小必要"原则，非必须字段不返回

---

## 维度四：极致用户体验与可观测性

> **现状诊断**：SDUI 有 13 个 Widget 但仅服务 Vue Web 端；SSE 不支持断点续传；缺少全链路追踪；无用户行为埋点和体验度量。

### 4.1 SDUI 多端泛化

**涉及文件**：`sdui/` 包下所有 Widget 类、`frontend/src/` 下对应渲染组件

**量化目标**：新增一种终端类型的渲染适配工作量从全量重写 → **≤ 2 周**（复用 80% 渲染逻辑）。

**方案**：

```
后端 SDUI Widget (Java)
    ↓ JSON 序列化（严格 Schema）
    ↓
┌───────────────────────────────┐
│     SDUI Protocol v1.0        │  ← 标准化 JSON Schema + $schema 声明
│  { "$schema": "...",         │
│    "type": "progress_ring",  │
│    "props": { ... }          │
│  }                            │
└───────────────────────────────┘
    ↓                       ↓
 Vue Web        未来：iOS SwiftUI / Android Compose
    ├── SduiRenderer  ├── SduiRenderer
    ├── Widget缓存     ├── Widget缓存
    └── 离线降级       └── 性能限制适配
```

**实施步骤**：

1. **标准化 Schema**：为 13 种 Widget 各定义 JSON Schema（`widgets/{type}.schema.json`）
2. **前端渲染层分离**：`frontend/src/components/sdui/SduiRenderer.vue`，按 Widget type 动态加载组件
3. **前端缓存策略**：
   - 常用 Widget（如 ProgressRing、StatCard、Tip）本地 IndexedDB 缓存
   - 接口返回 `ETag`，304 Not Modified 时不重新下载
4. **移动端适配**：
   - 未来移动端：渲染层适配，组件内对 `canvas` / `animation` 做降级
   - 离线模式：缓存最近 7 天的健康数据 + Widget 定义，断网时展示本地数据 + "离线"标识
5. **多端性能约束**：
   - 移动端单次接口数据量 ≤ 1MB
   - 移动端 Widget 动画帧率 ≥ 30fps

---

### 4.2 SSE 断点续传 + AI 体验打磨

**涉及文件**：`frontend/src/utils/sseClient.js`、`controller/ChatController.java`

**量化目标**：网络中断后恢复时间从重新开始 → **≤ 2s 内恢复**，用户无感知；AI 回复感知等待感降低 40%。

**方案**：

**后端改造**：
```
每个 SSE chunk 携带元数据：
event: message
data: {"cursor": 42, "text": "您的血糖", "progress": {"stage": "分析中", "total": 3, "current": 1}}
```

**前端改造**：

```javascript
// sseClient.js v2.0 — 带断点续传
export function createSSEStream(url, data, onMessage, onError, onProgress) {
  let lastCursor = 0;
  let receivedText = '';
  let reconnectAttempt = 0;

  function connect(cursor) {
    const payload = cursor ? { ...data, cursor } : data;
    fetch(`${BASE_URL}${url}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
      body: JSON.stringify(payload)
    }).then(response => {
      // ...
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      function process() {
        reader.read().then(({ done, value }) => {
          if (done) { onComplete(); return; }
          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n');
          buffer = lines.pop() || '';
          for (const line of lines) {
            if (line.startsWith('data:')) {
              const msg = JSON.parse(line.substring(5).trim());
              lastCursor = msg.cursor;       // 记录当前游标
              receivedText += msg.text;
              onMessage(msg.text);           // 打字机逐字输出
              if (msg.progress) onProgress(msg.progress); // 进度提示
            }
          }
          process();
        }).catch(err => {
          // 网络错误 → 指数退避重连
          const delay = Math.min(1000 * Math.pow(2, reconnectAttempt), 30000);
          reconnectAttempt++;
          onProgress({ stage: '重连中...', delay });
          setTimeout(() => connect(lastCursor), delay);
        });
      }
      process();
    });
  }

  connect();
}
```

**AI 体验细节**：

1. **打字机动画**：每个 chunk 追加到已有文本末尾，而非整块替换
2. **进度提示**：AI 处理中展示阶段信息：
   - "正在检索您的健康档案..."  →
   - "正在分析您的血糖数据..."  →
   - "正在生成健康建议..."
   - 预计剩余时间（如"预计还需 8 秒"）
3. **低质回复一键重生成**：回复末尾加"重新生成"按钮，带上 `regenerate: true` 标识
4. **回复反馈**：点赞/点踩 + 可选原因（"不准确" / "太笼统" / "有安全风险"），写入 `AiFeedback`
5. **首Token时间展示**：前端记录从发送到收到第一个 token 的时间，超过 3s 展示 "模型加速中..."

---

### 4.3 全链路追踪

**量化目标**：AI 响应变慢时，**≤ 1 分钟内**定位到瓶颈环节（鉴权 / DB / 检索 / LLM API）。

**实施步骤**：

1. **SkyWalking Agent**（无侵入）：
   - Dockerfile 增加 `-javaagent:skywalking-agent.jar -Dskywalking.agent.service_name=ai-health-backend`
   - 自动追踪 Controller → Service → Mapper 链路
2. **关键方法手动 Span**：
   ```java
   // ModelRouter.java
   @Trace(operationName = "llm/chat")
   public String chat(ModelTier tier, String prompt) { ... }

   // KnowledgeServiceImpl.java
   @Trace(operationName = "knowledge/search")
   public List<KnowledgeDoc> search(String query) { ... }

   // AesEncryptor.java
   @Trace(operationName = "security/encrypt")
   public static String encrypt(String plain) { ... }
   ```
3. **LLM 专用 Metrics**（Prometheus + Grafana）：

   | 指标                   | 类型      | 说明                       |
   |------------------------|-----------|----------------------------|
   | `llm_request_total`    | Counter   | 按模型 + 意图维度统计      |
   | `llm_token_total`      | Counter   | Token 消耗（Input + Output） |
   | `llm_first_token_ms`   | Histogram | 首 Token 延迟分布          |
   | `llm_e2e_ms`           | Histogram | 端到端延迟分布             |
   | `llm_error_total`      | Counter   | 调用失败次数               |

4. **业务监控面板**（Grafana Dashboard）：
   - 每日活跃用户、新增用户
   - AI 调用成功率、医疗红线拦截次数
   - P99 延迟趋势（按接口）
   - 模型调用成本趋势

5. **告警策略**：
   | 条件                             | 级别   | 通知方式         |
   |----------------------------------|--------|------------------|
   | P99 延迟 > 5s                    | P2     | Webhook 通知     |
| LLM 错误率 > 5%                  | P1     | Webhook + 短信   |
| 加密失败                         | P0     | Webhook + 电话   |
| 单用户日成本 > 1 元               | P1     | Webhook 通知     |
| 医疗红线拦截次数突增 > 200%       | P1     | Webhook + 安全审计 |

---

## 维度五：自动化测试与 DevOps

> **现状诊断**：6 个测试文件，无分层测试体系；有 Dockerfile 和 nginx 但无 CI/CD 流水线。

### 5.1 分层测试体系

**量化目标**：核心业务逻辑单元测试覆盖率 **≥ 70%**，安全相关代码覆盖率 **≥ 90%**。

**当前测试覆盖**：

| 测试类                                       | 类型 | 覆盖范围       |
|----------------------------------------------|------|----------------|
| `BaseTest.java`                              | 基类 | Spring 上下文  |
| `AuthServiceTest.java`                       | 单元 | 认证逻辑       |
| `HealthServiceTest.java`                     | 单元 | 健康记录 CRUD  |
| `JwtUtilTest.java`                           | 单元 | JWT 生成/解析  |
| `FunctionCallingTest.java`                   | 集成 | Agent Tool     |
| `DeepSeekFunctionCallingIntegrationTest.java` | 集成 | DeepSeek 对接  |

**目标测试金字塔**：

```
          /\
         /E2E\        5%   - Playwright（核心流程）
        /------\
       /集成测试\      20%  - Testcontainers（真实 MySQL/Redis/TDengine）
      /----------\
     /  单元测试 \    75%  - Mockito + JUnit 5
    /--------------\
```

**实施步骤**：

1. **单元测试（75%，需新增约 40 个测试类）**：

   | 优先级 | 测试类                                | 覆盖要点                                  |
   |--------|---------------------------------------|------------------------------------------|
   | P0     | `SafetyCheckerServiceTest`            | 所有 SafetyRule 匹配 + ComplianceRule    |
   | P0     | `ModelRouterTest`                     | 多 Tier 路由 + 熔断 + 降级链             |
   | P0     | `OnlineSafetyCircuitBreakerTest`      | 熔断触发/恢复/半开状态转换                |
   | P0     | `IntentRouterTest`                    | 意图分类正确率                            |
   | P1     | `AesEncryptorTest`                    | 加解密往返一致性 + 篡改检测               |
   | P1     | `AuditLogServiceTest`                 | Hash 链一致性                             |
   | P1     | `MemoryServiceImplTest`               | 三层记忆召回正确性                        |
   | P1     | 所有 Convert 类                        | 字段映射完整性                            |

2. **集成测试（20%）**：
   - 引入 Testcontainers：`@Testcontainers` 启动真实 MySQL + Redis + TDengine
   - `SafetyCheckerServiceIntegrationTest`：真实 DB 插入规则，验证完整拦截链路
   - `ChatServiceIntegrationTest`：端到端对话流程，验证记忆召回 + 安全拦截
   - `KnowledgeServiceIntegrationTest`：验证混合检索 + Rerank 结果正确性

3. **E2E 测试（5%）**：
   - 引入 Playwright
   - 场景 1：注册 → 登录 → 填写健康档案 → 生成 AI 计划 → 查看计划
   - 场景 2：AI 对话 → 流式回复 → 反馈评分
   - 场景 3：血糖录入 → Dashboard 趋势图渲染

4. **Agent 专项测试**：
   - Mock LLM 响应，验证 AgentOrchestrator 路由正确性
   - 验证 SafetyReviewAgent 对 50+ 危险回复取真集的拦截率 ≥ 99%

---

### 5.2 CI/CD 流水线

**量化目标**：代码 Push → 自动化测试完成 → 部署到测试环境 **≤ 15 分钟**。

```yaml
# .github/workflows/ci-cd.yml（新建）
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  SONAR_HOST: ${{ secrets.SONAR_HOST_URL }}
  SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}

jobs:
  # ===== Stage 1: 代码质量 =====
  quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }

      - name: Checkstyle（已有 checkstyle.xml）
        run: mvn checkstyle:check

      - name: SonarQube 静态分析
        run: mvn sonar:sonar
          -Dsonar.host.url=$SONAR_HOST
          -Dsonar.token=$SONAR_TOKEN
          -Dsonar.qualitygate.wait=true

      - name: OWASP 依赖漏洞扫描
        run: mvn dependency-check:check

  # ===== Stage 2: 测试 =====
  test:
    needs: quality
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: test
          MYSQL_DATABASE: ai_health_test
        ports: ['3306:3306']
      redis:
        image: redis:7-alpine
        ports: ['6379:6379']
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }

      - name: 单元测试
        run: mvn test -Dspring.profiles.active=test

      - name: 集成测试
        run: mvn verify -Pintegration-test

      - name: 覆盖率报告
        run: mvn jacoco:report

  # ===== Stage 3: 构建与部署 =====
  build-and-deploy:
    needs: test
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - name: 构建 Docker Image
        run: |
          docker build -t registry.example.com/ai-health-backend:${{ github.sha }} .
          docker push registry.example.com/ai-health-backend:${{ github.sha }}

      - name: 蓝绿部署
        run: |
          # 1. 部署新版本（green）
          kubectl apply -f k8s/deployment-green.yaml
          kubectl rollout status deployment/ai-health-backend-green

          # 2. 健康检查
          curl -f http://green-service/actuator/health || exit 1

          # 3. 切流到 green
          kubectl patch service ai-health-backend -p '{"spec":{"selector":{"version":"green"}}}'

          # 4. 保留 blue 版本用于回滚（24小时后清理）
```

---

### 5.3 代码质量闭环

**代码评审 Checklist**（PR 必须检查）：

- [ ] 敏感字段是否使用了 `EncryptedStringTypeHandler`？
- [ ] AI 回复是否经过了 `SafetyReviewAgent` 审核？
- [ ] 异步任务是否有失败重试策略？
- [ ] 新增 SQL 查询是否有对应索引？
- [ ] 新增 API 接口是否有 `@RateLimit` 注解？
- [ ] 健康数据查询是否遵循"最小必要"原则？
- [ ] 异常是否正确分类（`BusinessException` vs 通用）？

---

## 维度六：成本精细化管理

> **现状诊断**：无成本追踪，不知道每用户、每意图的 LLM 费用分布。

### 6.1 成本追踪与看板

**量化目标**：建立按 **用户 × 意图 × 模型** 三维度的成本看板，粒度到单次调用。

**实施步骤**：

1. **`MultiModelCostMonitor`** 扩展为表存储：
   ```sql
   CREATE TABLE llm_cost_log (
       id BIGINT PRIMARY KEY,
       user_id BIGINT,
       intent VARCHAR(32),       -- 意图分类：chat/report/plan/check
       model VARCHAR(64),        -- 模型名称
       model_tier VARCHAR(16),   -- LOW/MEDIUM/HIGH/CRITICAL
       prompt_tokens INT,
       completion_tokens INT,
       cost_cents INT,           -- 费用（分）
       created_at DATETIME
   );
   ```
2. **Grafana 成本面板**：
   - 当日 / 当周 / 当月总费用
   - Top-10 高消费用户
   - 按意图的费用分布（饼图）
   - 本地模型 vs 云端模型的请求比例
3. **成本报警**：单用户日消费 > 1 元 → Webhook 通知

### 6.2 降本策略验证

| 策略                  | 预期降本 | 验证方式                       |
|-----------------------|----------|-------------------------------|
| 本地 Ollama 模型       | 30-40%   | 对比本地/云端请求量占比         |
| 智能路由分层           | 20-30%   | 对比 LOW tier 走本地前后的费用  |
| 混合检索减少 Token     | 10-15%   | 对比检索质量提升后的 Prompt 压缩 |
| 缓存常见问题           | 5-10%    | 对比 Redis 命中率              |

---

## 维度七：数据治理

> **现状诊断**：数据无限增长，无生命周期管理和清理策略。

### 7.1 数据生命周期管理

| 数据类型               | 热数据保留 | 冷数据归档       | 清理策略                   |
|------------------------|-----------|-------------------|---------------------------|
| 体征数据（血糖等）     | 6 个月    | TDengine 长期保留 | 无需清理（TDengine 压缩率极高） |
| AI 对话 `ChatMessage`  | 3 个月    | OSS 归档（3 年）  | 3 个月后加密归档 → 删除 MySQL |
| 审计日志               | 6 个月    | OSS 归档（3 年）  | 等保要求 ≥ 6 个月在线      |
| 验证码/临时 Redis Key  | -         | -                 | TTL 自动过期               |
| 用户行为埋点           | 30 天     | OSS 归档（1 年）  | 聚合后删除原始记录          |

**实施步骤**：

1. **冷数据归档 Job**：`@Scheduled` 每月执行，将 3 个月前的对话/日志导出加密 JSON → OSS
2. **数据清理 Job**：归档确认后，删除 MySQL 中对应行
3. **过期用户数据清理**：用户注销后 30 天内可恢复，30 天后物理删除所有数据

---

## 实施优先级与路线图（含异常场景覆盖）

| 阶段              | 任务                            | 预估工时 | 前置依赖         | 异常场景覆盖                                       |
|-------------------|---------------------------------|----------|-------------------|----------------------------------------------------|
| **Phase 1**（第 1-2 周） | 敏感数据字段加密                | 3 人天   | 无                | 加密失败报警 + 密钥轮换容错                         |
|                   | 医疗红线三层拦截                | 5 人天   | 无                | BERT 模型不可用时降级为纯正则                       |
|                   | 消息队列解耦 AI 任务            | 5 人天   | RocketMQ 部署     | Broker 宕机切换 + 死信队列 + 全部不可周时降级方案   |
| **Phase 2**（第 3-4 周） | 时序数据库引入                  | 5 人天   | TDengine 集群部署 | 集群宕机降级 MySQL + 双写对比验证                    |
|                   | 向量数据库 + 混合检索           | 8 人天   | Qdrant 集群部署   | Qdrant 宕机降级 MySQL LIKE + Redis缓存              |
|                   | SSE 断点续传 + AI 体验打磨      | 5 人天   | 无                | 网络中断指数退避重连 + 超时后展示"请刷新"           |
| **Phase 3**（第 5-7 周） | 智能模型路由升级                | 5 人天   | Phase 2 向量库   | 全部云端模型不可用时预设安全应答模板                  |
|                   | 分层测试补充                    | 8 人天   | Phase 1-2 稳定   | 测试环境不可用时跳过集成测试（仅跑单元测试）         |
|                   | CI/CD 流水线搭建                | 5 人天   | 测试补完         | 部署失败自动回滚                                    |
| **Phase 4**（第 8-11 周）| Agent 记忆体系                  | 8 人天   | 消息队列         | Redis 不可用时降级为仅事实记忆 + 限流保护            |
|                   | SDUI 多端泛化 + 合规对标        | 12 人天  | 无                | 超 1MB 数据自动分页                            |
|                   | 读写分离/分库分表               | 5 人天   | 无                | 从库不可用时全部降级读主库                           |
| **Phase 5**（第 12-14 周）| 全链路追踪 + 业务监控面板        | 5 人天   | 无                | SkyWalking OAP 不可用时不影响业务                    |
|                   | 成本精细化 + 数据治理            | 8 人天   | Phase 4          | 成本日志写入失败时异步重试，不影响主链路              |
|                   | 本地模型部署 + 容灾演练          | 8 人天   | GPU 服务器       | Ollama 宕机所有请求升级到云端                        |

**容灾演练计划**（月度）：

1. 模拟 TDengine Master 宕机 → 验证自动切换 + 数据完整性
2. 模拟 RocketMQ Broker 宕机 → 验证消息不丢 + Consumer 切换
3. 模拟 Qdrant 节点宕机 → 验证降级 MySQL + 恢复后数据同步
4. 模拟全部云端 LLM 不可用 → 验证降级链路到本地模型 + 预设应答
5. 模拟 MySQL 主库宕机 → 验证读降级 + 写阻塞处理

---

## 风险与注意事项

1. **双写过渡期风险**：时序库和消息队列引入时，保留 MySQL 原有逻辑至少 2 周，确保可回滚
2. **加密密钥管理**：AES 密钥必须从环境变量或 KMS 获取，禁止硬编码；密钥轮换时要支持双密钥并存过渡期
3. **向量库选型**：Qdrant 适合千万级向量；超亿级时提前评估 Milvus 迁移方案
4. **不要过早微服务化**：满足"微服务拆分判断标准"中 ≥ 2 个条件时再考虑
5. **本地模型部署**：Ollama + GPU 推理，需要至少 1 张 T4/A10 GPU，冷启动时间约 5-10s
6. **合规认证前置**：等保三级认证周期通常 3-6 个月，建议 Phase 1 就启动差距分析
7. **成本监控**：避免"优化后成本反而更高"的情况，每次技术选型后跑 1 周的成本对比报告

---

> **核心理念**：
>
> 优秀的医疗 AI 软件，不是"技术功能的堆砌"，而是"技术闭环 × 业务闭环 × 合规闭环 × 成本闭环"的综合产物。
>
> - 用户用得放心 → 安全合规做到极致
> - 团队维护得省心 → 工程化体系到位
> - 业务跑得稳心 → 高可用 + 成本可控
>
> 这份 v2.0 方案在 v1.0 的"做什么"基础上，补齐了"做到什么程度（量化指标）"和"如何闭环（高可用/合规/体验度量/数据治理）"。
> 每轮优化后，用 **非功能指标 + 用户体验指标 + 成本指标** 三重验证效果，形成"优化 → 验证 → 复盘"的持续改进闭环。