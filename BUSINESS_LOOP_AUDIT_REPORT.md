## AI Health System 全模块业务闭环检查报告

检查时间：2026-06-12 | 检查范围：40 个前端页面、42 个 Controller、44 个 Entity、11 个子模块

---

### 一、总览

| 模块 | 状态 | 严重缺口 | 中等缺口 | 轻微缺口 |
|------|------|----------|----------|----------|
| 用户认证 | ⚠️ 部分闭环 | 0 | 3 | 3 |
| 健康看板 Dashboard | ⚠️ 部分闭环 | 0 | 5 | 3 |
| AI 计划生成与管理 | ⚠️ 部分闭环 | 2 | 3 | 3 |
| 饮食记录 | ❌ 核心断裂 | 4 | 1 | 1 |
| 运动记录 | ❌ 核心断裂 | 4 | 1 | 1 |
| 打卡/睡眠/饮水/体重 | ⚠️ 部分闭环 | 1 | 1 | 4 |
| AI 聊天咨询 Copilot | ✅ 基本闭环 | 0 | 1 | 0 |
| 健康数据与体检 | ⚠️ 部分闭环 | 1 | 1 | 2 |
| 社区 | ✅ 完全闭环 | 0 | 0 | 1 |
| 通知 | ⚠️ 部分闭环 | 0 | 2 | 1 |
| 管理后台 | ❌ 多处断裂 | 4 | 1 | 0 |
| 计费/目标/导出/企业 | ⚠️ 部分闭环 | 3 | 0 | 1 |
| 安全合规与基础设施 | ✅ 完全闭环 | 0 | 0 | 0 |

---

### 二、严重缺口（会导致运行时失败或数据丢失）

#### S1. 饮食/运动记录提交字段全面不匹配（影响：饮食记录、运动记录、NLP 记录、图像识别入库）

前端表单提交的字段名与后端 DTO 完全不同，导致所有记录提交都会 400 报错：

| 前端发送 | 后端期望 | 问题 |
|---------|---------|------|
| `foodItemId` | `itemId` | 字段名不同 |
| `amount` | `weightGrams` | 字段名不同 |
| `unit` | （无对应字段） | 多余字段 |
| （缺失） | `checkinId` | @NotNull，必填 |
| （缺失） | `caloriesConsumed` | @NotNull，必填 |

运动记录同理：`exerciseItemId` vs `itemId`、`duration` vs `durationMinutes`、缺失 `checkinId` 和 `caloriesBurned`。

**修复方案：** 统一前后端字段名；前端增加卡路里自动计算（`caloriePer100g * weightG / 100`）；前端在提交前获取当日 checkinId。

#### S2. 后端多处 `@RequestParam` 接收前端 JSON Body（影响：退款、发票、目标状态、审批、规则建议、AI 反馈审核）

以下 6 个 Controller 方法使用 `@RequestParam` 接收参数，但前端统一发送 `Content-Type: application/json` Body：

- `RefundAndInvoiceController.applyRefund()` — reason
- `RefundAndInvoiceController.applyInvoice()` — orderNo, invoiceType, invoiceTitle, taxNumber
- `GoalMilestoneController.updateStatus()` — status
- `AdminApprovalController.approve()` / `reject()` — approverName, reason
- `AdminRuleSuggestionController.approveSuggestion()` / `rejectSuggestion()` — reviewerName
- `AiFeedbackController.review()` — result

**修复方案：** 将 `@RequestParam` 改为 `@RequestBody` + 对应 DTO，或前端改用 query parameter。

#### S3. 三个管理后台 Controller 缺少 `@AdminOnly` 注解（安全漏洞）

- `AdminApprovalController` — 任何登录用户可审批/拒绝敏感操作
- `AdminRuleSuggestionController` — 任何登录用户可审批/拒绝规则建议
- `AiFeedbackController` — 任何登录用户可访问待审核列表和执行审核

**修复方案：** 在这 3 个 Controller 类上添加 `@AdminOnly` 注解。

#### S4. 管理后台敏感操作缺少审批 ID（影响：用户封禁、食品/运动项目删除、通知发送）

后端要求 `X-Approval-Id` Header 才能执行敏感操作，但前端管理页面全部未传递此 Header：

- `UserManage.vue` — 封禁/解封用户
- `FoodManage.vue` — 删除食品项目
- `ExerciseManage.vue` — 删除运动项目
- `NotificationSend.vue` — 发送全站通知

所有操作将返回 403。

**修复方案：** 在管理页面增加"发起审批"流程，调用审批接口获取 approvalId 后再执行操作；或评估是否需要审批流。

#### S5. 健康报告月度生成功能失效

`HealthReportController.generate()` 使用 `@RequestParam` 读取 `reportType`，但前端以 JSON Body 发送。Spring 忽略 Body，永远使用默认值 `"weekly"`。点击"生成月报"按钮实际生成的是周报。

**修复方案：** 改为 `@RequestBody Map<String,String>` 或前端改为 query parameter。

#### S6. AI 计划 Tool Call 热更新不持久化

Copilot 通过 Tool Call（`replace_item`/`replace_day_items`/`set_plan`）修改计划时，只更新了 Pinia 内存状态，没有任何 API 调用将变更写回数据库。用户刷新页面后所有 AI 驱动的计划修改全部丢失。

**修复方案：** 在 `executeToolCall()` 中增加 API 调用，将修改持久化到 `ai_plan` 表的 `ai_content` 字段或 `ai_plan_detail` 表。

#### S7. AI 计划调整前后端响应契约不匹配

前端 `Detail.vue` 期望 `adjustResult.summary` 和 `adjustResult.changes[]`，但后端返回 `AiAgentResponse` 结构（`text` + `widgets` + `disclaimer`）。调整结果区域将渲染为空。

**修复方案：** 前端改为解析 `AiAgentResponse` 的 `text` 和 `widgets`，或后端增加 `summary`/`changes` 字段。

#### S8. 计划固化流程语义失效

所有计划创建时直接设为 `status=1`（ACTIVE），但固化流程假设计划初始状态为 DRAFT。`solidifyPlan()` 将状态设为 1 是 no-op。此外 DTO 文档提到"自动填充日历"但没有任何日历相关实现。

**修复方案：** 计划生成时设为 `status=0`（DRAFT）；或移除固化入口；明确日历集成的范围。

---

### 三、中等缺口（功能不完整或数据浪费）

#### M1. 用户名前后端校验长度不一致

前端 `validate.js` 用户名最小 3 位，后端 `UserRegisterDTO` 最小 4 位。3 位用户名在前端通过但在后端报 400。

#### M2. 忘记密码密码长度校验不一致

前端 `ForgotPassword.vue` 密码最小 6 位，后端 `ResetPasswordDTO` 最小 8 位。

#### M3. 修改密码后不强制重新登录

`UserServiceImpl.updatePassword()` 只更新密码，不清除现有 Token。其他设备上的会话仍然有效。

#### M4. Dashboard "今日" 标签页大量数据未展示

后端 `DashboardTodayVO` 计算了 10 个字段（planId, planName, tasks[] 完整任务列表含 5 个子字段, completedTasks, totalTasks, exerciseRecordsCount, dietRecordsCount），但前端今日标签页只展示了打卡状态和卡路里数字。

#### M5. 健康评估丰富数据未展示

后端 `HealthAssessmentVO` 计算了 healthScore, aiSuggestion, weightTrend, bmiTrend, estimatedBodyFatRate, bodyFatLevel, bmrAssessment, cardiovascularRisk, exerciseAbility 共 9 个字段，前端 `View.vue` 只展示了 bmiLevel 和 risks。

#### M6. 健康报告永远不会标记为已读

`Report.vue` 打开报告详情时用的是本地缓存对象，从不调用 `getReportDetail()` 接口，导致后端 `markAsRead()` 永远不触发。

#### M7. 通知偏好 Service 只保存 2/7 个字段

`UserServiceImpl.updateNotificationPreference()` 只保存 `notificationEnabled` 和 `reminderTime`，忽略了 `notifyExercise`, `notifyDiet`, `notifyCheckin`, `quietStart`, `quietEnd`。

#### M8. 用户反馈提交无前端入口

`submitPlanFeedback` API 已定义、后端已实现（含 AI 自动调整触发），但没有任何用户页面提供反馈表单。

#### M9. 运动指导前后端结构不匹配

前端 `ExerciseRecord.vue` 期望结构化对象（`basicInfo.type`, `steps[]` 数组, `tips`），后端返回扁平字符串（`technique`, `commonMistakes`, `safetyTips`）。指导卡片将显示为空或乱码。

#### M10. 通知偏好两个页面数据类型不一致

`NotificationList.vue` 用整数 1/0，`NotificationPreference.vue` 用布尔 true/false，提交到同一个后端接口。

#### M11. 通知偏好 Controller 绕过 Service 层

`NotificationPreferenceController` 直接注入 `SysUserMapper`，没有事务管理和业务校验。

---

### 四、轻微缺口（体验优化或代码清理）

| 编号 | 模块 | 问题 |
|------|------|------|
| L1 | 认证 | 头像 URL 字段接受任意字符串，无 URL 格式校验 |
| L2 | 认证 | 账号注销 API 和后端已实现，但 Profile 页面无入口按钮 |
| L3 | 认证 | 旧密码 DTO 校验 min=6 与实际密码 min=8 不一致 |
| L4 | 看板 | 月份标签页未展示 exerciseRecordsCount 和 dietRecordsCount |
| L5 | 看板 | 打卡图表用 totalDays（0/1 记录数）代替 completeRate（完成率） |
| L6 | 看板 | AI 推荐 healthTips 字段后端生成但前端不展示 |
| L7 | 看板 | 统计看板未调用饮食趋势对比 API |
| L8 | 饮食 | 管理后台表单字段名不匹配后端 DTO（caloriesPerUnit vs caloriePer100g 等） |
| L9 | 饮食 | 食品识别事件 `FoodRecognizedEvent` 发布后无消费者 |
| L10 | 运动 | ExerciseRule 实体已定义但未接入任何业务流程 |
| L11 | 打卡 | CheckinTrendVO 存在但无 Controller 暴露 |
| L12 | 睡眠 | 删除 API 已定义但页面无删除按钮 |
| L13 | 饮水 | 删除 API 和每日总量 API 已定义但页面无对应按钮 |
| L14 | 体测 | 删除 API 已定义但页面无删除按钮 |
| L15 | 血糖 | getBloodSugarByDate API 已定义但未使用 |
| L16 | 健康 | getHealthHistory 和 getHealthProgress API 已定义但未被任何页面调用 |
| L17 | 健康 | Create.vue 和 Form.vue 功能重复，导航路径混乱 |
| L18 | 通知 | 广播通知逐条插入（性能问题） |
| L19 | 导出 | 导出历史区域被 `v-if="false"` 隐藏（预期未完成） |
| L20 | AI计划 | 计划列表标签仅显示 sport/diet 两种类型 |

---

### 五、完全闭环的模块

以下模块前后端全链路畅通，无严重或中等问题：

**社区模块** — 发帖、评论、点赞、排行、删除，9 个 API 全部匹配，限流和防重复提交到位。

**AI 聊天 Copilot** — SSE 流式对话（含断点续传）、上下文感知、Tool Call 执行、SDUI 渲染、会话管理，核心链路完整。SSE 超时保护和错误边界已就绪。

**安全合规与基础设施** — 四层安全过滤器（CSRF/XSS/安全头/JWT）、四个切面（限流/防重复/订阅/管理员）、CORS、WebSocket、RocketMQ（4 生产者 4 消费者无孤儿）、6 个定时任务全部正确装配。

---

### 六、修复优先级建议

**P0（必须立即修复，阻断核心功能）：**
1. S1 — 饮食/运动记录字段全面对齐
2. S2 — 6 个 Controller 的 @RequestParam 改 @RequestBody
3. S3 — 3 个管理 Controller 补 @AdminOnly
4. S5 — 健康报告月度生成修复

**P1（尽快修复，影响用户体验或数据安全）：**
5. S4 — 管理后台审批流集成或简化
6. S6 — Tool Call 变更持久化
7. S7 — 计划调整响应契约对齐
8. S8 — 计划固化语义修正（DRAFT 状态）
9. M1/M2 — 前后端校验统一
10. M6 — 报告已读标记修复
11. M9 — 运动指导结构对齐

**P2（计划修复，提升完整性）：**
12. M4/M5 — Dashboard 展示已计算的丰富数据
13. M7 — 通知偏好全字段持久化
14. M8 — 计划反馈提交入口
15. M3 — 修改密码后 Token 失效
16. M10/M11 — 通知偏好类型统一 + Service 层重构
17. L12/L13/L14 — 各记录页面补充删除按钮
