# AI-Health-System GUI + LUI 双轨融合优化方案

> **"智能不是强迫用户跟机器聊天，而是机器在后台默默算好，用传统的、最直观的界面呈现给用户。"**
>
> 版本：v1.0 | 日期：2026-06-12
>
> 目标：将当前"页面割裂"的体验升级为 **Copilot（副驾驶）模式 + 预测性看板（Predictive Dashboard）** 的融合架构。

---

## 零、现状诊断

### 当前体验痛点

| 问题 | 现状 | 影响 |
|------|------|------|
| **AI 是独立页面** | `ChatBot.vue` 是 MainLayout 底部浮动按钮，打开后是独立对话窗；`plan/Generate.vue` 是独立表单页 | 用户在传统页面（如看计划详情）需要 AI 帮助时，必须跳走或开小窗，AI 无法直接操作当前页面数据 |
| **首页是"死看板"** | `Dashboard.vue` 仅展示体重、BMI、图表等静态数据，毫无智能化 | 用户打开 App 后看不到任何动态的、个性化的 AI 建议 |
| **饮食记录太繁琐** | `food/Record.vue` 需要用户手动搜索、选单位、填克数 | 用户体验差，记录一顿饭要操作 5+ 步 |
| **AI 生成结果无法固化** | `plan/Generate.vue` 生成后只是一条计划记录，没有"审批→固化到日历"流程 | AI 的创意和用户的修改没有形成闭环 |
| **无全局状态通道** | `stores/app.js` 只有 `sidebarCollapsed` 和 `pageLoading` 两个字段，AI 修改数据后无法实时通知传统页面 | 无法实现"AI 说换动作，页面瞬间热更新" |

### 现有架构中的有利基础

| 已有能力 | 可以复用的部分 |
|----------|---------------|
| `ChatBot.vue` 已经在 MainLayout 底部作为全局组件加载 | 改造成 Bottom Sheet 抽屉的基础 |
| `FoodRecognitionController` + 图片识别已可用 | 扩展为"一句话记账"文字识别 |
| `AiPlanController` + SSE 流式生成 | 扩展为实时修改计划的 SSE 通道 |
| Pinia stores (`user.js`, `app.js`) | 扩展 `app.js` 为全局数据通道 |
| `PlanDetail.vue` 的 phase 结构 | 适配 AI 热更新 |

---

## 一、总体架构：创造与消费分离

```
┌─────────────────────────────────────────────────────────────┐
│                      用户终端 (Vue 3 App)                      │
│                                                               │
│  ┌─ GUI (传统页面 - 消费层) ──┐  ┌─ LUI (AI抽屉 - 创造层) ──┐ │
│  │                            │  │                            │ │
│  │  Dashboard (预测性看板)     │  │  GlobalCopilot (AI抽屉)    │ │
│  │  PlanDetail (课表跟练)      │  │    ↕ Voice/Text 交互       │ │
│  │  Calendar (打卡日历)        │  │    ↕ AgentOrchestrator     │ │
│  │  FoodRecord (饮食记录GUI)   │  │    ↕ 生成/修改/分析       │ │
│  │  只查 MySQL/Redis           │  │                                │ │
│  │                            │  │                            │ │
│  └────────────────────────────┘  └────────────────────────────┘ │
│              ↕                         ↕                        │
│         ┌─────────────────────────────────────┐                 │
│         │        Pinia Store (数据通道)         │                 │
│         │   app.js / plan.js / food.js / ...   │                 │
│         └─────────────────────────────────────┘                 │
│              ↕                         ↕                        │
│         ┌─────────────────────────────────────┐                 │
│         │          Spring Boot 后端              │                 │
│         │                                       │                 │
│         │  DashboardController (预计算)          │                 │
│         │  AiPlanController (计划CRUD)           │                 │
│         │  ChatController / PlanAdjustController │                 │
│         │  FoodRecognitionController             │                 │
│         │                                       │                 │
│         │  ← MySQL / Redis / Elasticsearch →    │                 │
│         └─────────────────────────────────────┘                 │
└──────────────────────────────────────────────────────────────────┘
```

**核心原则：**
- **GUI 只消费数据**：读取 MySQL/Redis，零大模型 Token。80% 的用户时间花在这里。
- **LUI 只创造/修改数据**：调用大模型。用户每天只用几次，每次几秒到几分钟。
- **API 成本节约**：相比纯对话模式（每次打开 App 都调 LLM），**至少节省 70-90% 的 Token 消耗**。

---

## 二、前端改造方案（5 个关键改造点）

### 2.1 全局 AI Copilot 抽屉

**改造文件**：`frontend/src/views/chat/ChatBot.vue` → 重构为 `frontend/src/components/GlobalCopilotBottomSheet.vue`

**现状**：ChatBot.vue 在 MainLayout 中以 `defineAsyncComponent` 方式加载为浮动按钮 + 聊天窗，与页面完全解耦，不知道用户在哪个页面、在看什么内容。

**改造后**：

```
┌──────────────────────────────────┐
│  Header                          │
│                                  │
│  ┌─── 当前页面内容 ───┐           │
│  │                       │           │
│  │  PlanDetail /        │           │
│  │  FoodRecord /        │      +  ┌──────────────┐
│  │  Calendar / ...      │         │ 悬浮 AI 按钮  │
│  │                       │         └──────────────┘
│  │                       │           点击 / 上滑
│  │                       │              ↓
│  │                       │      ┌─────────────────────┐
│  │                       │      │ AI Copilot 底部抽屉   │
│  └───────────────────────┘      │ • 上下文感知消息      │
│                                  │ • 语音输入            │
│                                  │ • 修改当前页面数据     │
│                                  │ • 一键固化到计划       │
│                                  └─────────────────────┘
└──────────────────────────────────┘
```

**实施步骤**：

#### Step 1：扩展 Pinia app store 为全局数据总线

**文件**：`frontend/src/stores/app.js`

```js
import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'

export const useAppStore = defineStore('app', () => {
  // === 原有状态 ===
  const sidebarCollapsed = ref(false)
  const pageLoading = ref(false)

  // === 新增：全局 Copilot 状态 ===
  const copilotOpen = ref(false)
  const copilotContext = ref(null)  // 当前页面上下文：{ page, entityId, entityData }

  // === 新增：全局事件总线（用于 AI → GUI 热更新） ===
  const eventListeners = reactive(new Map())

  function on(event, callback) {
    if (!eventListeners.has(event)) {
      eventListeners.set(event, new Set())
    }
    eventListeners.get(event).add(callback)
  }

  function off(event, callback) {
    eventListeners.get(event)?.delete(callback)
  }

  function emit(event, payload) {
    eventListeners.get(event)?.forEach(cb => cb(payload))
  }

  // === Copilot 操控 ===
  function openCopilot(context = null) {
    copilotContext.value = context
    copilotOpen.value = true
  }

  function closeCopilot() {
    copilotOpen.value = false
    copilotContext.value = null
  }

  // === 原有方法 ===
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setPageLoading(loading) {
    pageLoading.value = loading
  }

  return {
    // 原有
    sidebarCollapsed, pageLoading, toggleSidebar, setPageLoading,
    // 新增
    copilotOpen, copilotContext, openCopilot, closeCopilot,
    on, off, emit
  }
})
```

#### Step 2：新建全局 Copilot 底部抽屉组件

**新文件**：`frontend/src/components/GlobalCopilotDrawer.vue`

核心能力：
- 底部抽屉（Bottom Sheet），参考微信/抖音的交互
- 感知当前页面上下文（路由 + Pinia 中传入的 context）
- 支持文字输入 + 语音输入
- 调用 ChatController 的 SSE 接口
- 大模型返回结构化的 tool_calls，前端解析后调用对应的 Pinia action 修改页面数据

**组件结构**：

```vue
<template>
  <el-drawer
    v-model="visible"
    direction="btt"
    size="65vh"
    :with-header="false"
    :before-close="handleClose"
    custom-class="copilot-drawer"
  >
    <div class="copilot-container">
      <!-- 头部：上下文信息 -->
      <div class="copilot-header">
        <div class="handle-bar" />
        <div class="context-badge" v-if="contextInfo">
          <el-tag size="small" type="info" effect="plain">
            <el-icon><component :is="contextInfo.icon" /></el-icon>
            {{ contextInfo.label }}
          </el-tag>
          <el-tag v-if="contextInfo.entityName" size="small" type="warning" effect="plain">
            {{ contextInfo.entityName }}
          </el-tag>
        </div>
        <el-button text :icon="CloseBold" @click="handleClose" />
      </div>

      <!-- 消息区 -->
      <div class="copilot-messages" ref="messagesRef">
        <!-- ... 复用原 ChatBot.vue 的消息渲染逻辑 ... -->
      </div>

      <!-- 输入区：文字 + 语音 -->
      <div class="copilot-input">
        <el-input
          v-model="inputText"
          placeholder="描述你想做什么，如：把深蹲换成臀桥..."
          @keyup.enter="handleSend"
          :disabled="streaming"
        />
        <el-button :icon="Microphone" circle @mousedown="startVoice" @mouseup="stopVoice" />
        <el-button type="primary" :icon="Promotion" @click="handleSend" :loading="streaming" />
      </div>

      <!-- 快速操作建议（上下文相关） -->
      <div class="quick-actions" v-if="quickActions.length > 0">
        <el-button
          v-for="action in quickActions" :key="action.label"
          size="small" plain
          @click="handleQuickAction(action)"
        >
          {{ action.label }}
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>
```

#### Step 3：修改 MainLayout 加载方式

**文件**：`frontend/src/layout/MainLayout.vue`

```vue
<!-- 替换原来的 <ChatBot /> 为 -->
<GlobalCopilotDrawer />
```

**改动**：在 `MainLayout.vue` 中从 `defineAsyncComponent(() => import('@/views/chat/ChatBot.vue'))` 改为直接导入 `GlobalCopilotDrawer`。

---

### 2.2 预测性动态看板

**改造文件**：`frontend/src/views/Dashboard.vue`

**现状**：只显示体重/BMI/卡路里卡片 + ECharts 图表，纯静态数据。

**改造后**：保留所有 ECharts 图表，但顶部增加 AI 预计算的动态问候卡片。

#### Step 1：新增 DynamicGreetingCard 组件

**新文件**：`frontend/src/components/DynamicGreetingCard.vue`

```vue
<template>
  <div class="greeting-card" :class="cardStyleClass">
    <!-- 主卡片 -->
    <div class="greeting-content">
      <div class="greeting-header">
        <span class="greeting-emoji">{{ card.icon }}</span>
        <span class="greeting-time">{{ card.greeting }}</span>
      </div>
      <h3 class="greeting-message">{{ card.message }}</h3>
      <p class="greeting-detail">{{ card.detail }}</p>

      <!-- CTA 按钮 -->
      <div class="greeting-actions" v-if="card.actions?.length">
        <el-button
          v-for="action in card.actions"
          :key="action.label"
          :type="action.primary ? 'primary' : 'default'"
          size="small"
          @click="handleAction(action)"
        >
          {{ action.label }}
        </el-button>
      </div>
    </div>

    <!-- 进度条 (如果有今日计划进度) -->
    <div class="greeting-progress" v-if="card.progress != null">
      <el-progress
        :percentage="card.progress"
        :color="progressColor"
        :stroke-width="6"
        :show-text="false"
      />
      <span class="progress-label">{{ card.progress }}% 完成</span>
    </div>
  </div>
</template>
```

**4 种卡片状态**：

| 时段/状态 | icon | 效果 | 消息示例 |
|-----------|------|------|----------|
| **早晨** (有训练计划) | ☀️ | 蓝色渐变 | "早安！今日身体状态良好，今晚 20:00 安排了胸肌训练" |
| **中午** (饮食评估) | 🍽️ | 绿色渐变 | "午饭时间到！今日还可摄入 850 kcal，推荐低脂高蛋白午餐" |
| **晚上未打卡** | ⏰ | 红色警告 | "今晚的训练还未打卡！点击这里让 AI 为你降级为 5 分钟拉伸" |
| **晚上已打卡** | 🏆 | 金色庆祝 | "今日任务全部完成！连续打卡 12 天，太厉害了！" |

#### Step 2：后端 API

**改造文件**：`DashboardController.java`（新增端点）

```java
@Operation(summary = "AI 预测性问候卡片")
@GetMapping("/greeting")
public Result<DashboardGreetingVO> greeting(@RequestAttribute("userId") Long userId) {
    // 规则引擎（非大模型）生成卡片，降低延迟和成本
    return Result.success(dashboardService.generateGreeting(userId));
}
```

**规则引擎逻辑**（`DashboardService.java` 中新增）：

```java
public DashboardGreetingVO generateGreeting(Long userId) {
    // 1. 查询最近一次睡眠（判断身体状态）
    SleepRecord lastSleep = sleepMapper.selectLatestByUserId(userId);

    // 2. 查询今日打卡状态
    DailyCheckin todayCheckin = checkinMapper.selectByUserIdAndDate(userId, LocalDate.now());

    // 3. 查询今日 AI 计划
    AiPlanDetail todayPlan = aiPlanDetailMapper.selectCurrentByUserId(userId);

    // 4. 根据时间 + 状态组合，用规则引擎匹配卡片模板
    return GreetingRuleEngine.evaluate(lastSleep, todayCheckin, todayPlan);
}
```

**关键**：此处不需要调用大模型！纯规则匹配 + 模板填充，毫秒级响应。

#### Step 3：修改 Dashboard.vue

在 `Dashboard.vue` 的 `<template>` 顶部插入：

```vue
<!-- AI 动态问候卡片 -->
<DynamicGreetingCard
  v-if="greetingCard"
  :card="greetingCard"
  @action="handleGreetingAction"
/>
```

---

### 2.3 AI 嵌入传统表单："一句话记账"

**改造文件**：`frontend/src/views/food/Record.vue`

**现状**：已有图片识别（拍照→AI→填入），但文字输入仍是传统搜索栏。

**改造后**：在搜索栏上方新增"一句话录入"输入框。

#### 前端改动

在 `food/Record.vue` 中，在搜索栏上方添加：

```vue
<!-- 一句话记账 -->
<el-card class="nlp-input-card">
  <div class="nlp-input-row">
    <el-input
      v-model="nlpText"
      placeholder="一句话记录，如：中午吃了一碗兰州拉面加煎蛋"
      @keyup.enter="handleNlpRecord"
    />
    <el-button type="primary" :loading="nlpLoading" @click="handleNlpRecord">
      智能识别
    </el-button>
  </div>
  <!-- AI 解析结果预览 -->
  <div v-if="nlpResult" class="nlp-result">
    <div v-for="(item, i) in nlpResult.items" :key="i" class="nlp-result-item">
      <el-icon><Check /></el-icon>
      <span>{{ item.foodName }}</span>
      <el-input-number v-model="item.weightG" :min="1" size="small" controls-position="right" />
      <span>克</span>
      <span class="nlp-calorie">≈ {{ item.calories }} kcal</span>
      <el-button size="small" type="danger" text @click="removeNlpItem(i)">删除</el-button>
    </div>
    <div class="nlp-result-footer">
      <el-button size="small" @click="nlpResult = null">取消</el-button>
      <el-button size="small" type="primary" @click="confirmNlpRecord">一键录入</el-button>
    </div>
  </div>
</el-card>
```

#### 后端 API

**改造文件**：`FoodRecognitionController.java`（新增文字识别端点）

```java
@Operation(summary = "自然语言食物识别（一句话记账）")
@PostMapping("/recognize-text")
public Result<FoodRecognizeVO> recognizeText(
    @RequestAttribute("userId") Long userId,
    @RequestBody @Valid FoodRecognizeDTO dto
) {
    return Result.success(foodRecognitionService.recognizeByText(userId, dto.getText()));
}
```

`FoodRecognitionService.recognizeByText()` 实现逻辑：

```java
public FoodRecognizeVO recognizeByText(Long userId, String text) {
    // 1. 调用大模型解析
    String prompt = """
        解析食物描述为 JSON：
        { 
          "items": [
            {"foodName": "兰州拉面", "weightG": 500, "calories": 560},
            {"foodName": "煎蛋", "weightG": 60, "calories": 90}
          ]
        }
        描述：%s
    """.formatted(text);

    String response = deepSeekService.chatSync(prompt);  // 非流式

    // 2. 与食物数据库交叉比对，校准热量
    List<FoodItem> calibrated = foodMapper.matchAndCalibrate(response);

    return new FoodRecognizeVO(calibrated);
}
```

---

### 2.4 计划页面的 AI 实时热更新

**改造文件**：
- `frontend/src/views/plan/Detail.vue`（接收实时更新）
- `frontend/src/components/GlobalCopilotDrawer.vue`（发起更新指令）
- `frontend/src/stores/plan.js`（新建计划状态 store）

#### Step 1：新建 plan store

**新文件**：`frontend/src/stores/plan.js`

```js
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const usePlanStore = defineStore('plan', () => {
  const currentPlan = ref(null)
  const currentPlanDays = ref([])
  const planVersion = ref(0)  // 版本号，用于触发响应式更新

  function setPlan(plan, days) {
    currentPlan.value = plan
    currentPlanDays.value = days
    planVersion.value++
  }

  function updateDayItem(dayIndex, itemIndex, newItem) {
    const day = currentPlanDays.value[dayIndex]
    if (day) {
      day.items[itemIndex] = newItem
      planVersion.value++
    }
  }

  function replaceDayItems(dayIndex, newItems) {
    const day = currentPlanDays.value[dayIndex]
    if (day) {
      day.items = newItems
      planVersion.value++
    }
  }

  return { currentPlan, currentPlanDays, planVersion, setPlan, updateDayItem, replaceDayItems }
})
```

#### Step 2：修改 PlanDetail.vue 使用 Pinia

**改动点**：把 `plan` 和 `planDays` 从 local ref 改为从 `usePlanStore()` 取，让 AI 修改后自动反映：

```vue
<script setup>
import { usePlanStore } from '@/stores/plan'
const planStore = usePlanStore()
const plan = computed(() => planStore.currentPlan)
const planDays = computed(() => planStore.currentPlanDays)
// planVersion 变化时 watch 自动刷新
</script>
```

#### Step 3：AI Copilot 修改计划的流程

```
用户在 PlanDetail 页面，打开 AI Copilot
    ↓
用户说："把第二天的深蹲换成臀桥"
    ↓
前端构造上下文 → appStore.openCopilot({
  page: 'planDetail',
  planId: 42,
  entityName: '30天运动计划'
})
    ↓
GlobalCopilotDrawer 发送消息到 ChatController
  → 附带上 context（planId, 当前计划结构）
    ↓
后端 IntentRouter 识别意图 → "plan.adjust"
  → AgentOrchestrator 调用 PlanTools.adjustPlan()
    ↓
大模型返回结构化 tool_call：
  {
    "action": "replace_item",
    "dayIndex": 1,
    "itemIndex": 2,
    "newItem": { "name": "臀桥", "durationMin": 15, "phases": [...] }
  }
    ↓
前端解析 tool_call → 执行 planStore.replaceDayItems(1, [...])
    ↓
PlanDetail.vue 因为 planDays computed 变化 → 视图实时热更新
```

#### Step 4：后端 PlanAdjustController（已存在，需改造）

**改造文件**：`PlanAdjustController.java`

```java
// 新增：支持 SSE 流式返回调整结果
@PostMapping("/adjust-stream")
public SseEmitter adjustPlanStream(
    @RequestAttribute("userId") Long userId,
    @RequestBody PlanAdjustDTO dto
) {
    // 返回 SSE，实时推送调整步骤
    return planAdjustService.adjustWithStream(userId, dto);
}
```

---

### 2.5 SDUI 一键固化

**改造文件**：`frontend/src/views/plan/Generate.vue` + `frontend/src/views/chat/ChatBot.vue`

**场景**：用户在聊天中与 AI 反复调整，最终得到一个完美的 30 天计划。

**改造**：在聊天中 AI 返回的计划卡片右上角，加上 `[固化到我的计划]` 按钮。

#### 前端：AI 消息气泡中嵌入操作按钮

在 `ChatBot.vue`（或 `GlobalCopilotDrawer.vue`）的消息渲染中，当消息携带 `sdui` 字段为 plan 类型时：

```vue
<div v-if="msg.sdui?.type === 'plan_card'" class="sdui-plan-card">
  <div class="sdui-plan-header">
    <h4>{{ msg.sdui.planName }}</h4>
    <el-button size="small" type="primary" @click="handleApplyPlan(msg.sdui)">
      固化到我的计划
    </el-button>
  </div>
  <div class="sdui-plan-preview">
    <span>{{ msg.sdui.durationDays }}天 · {{ msg.sdui.planType }}</span>
    <span>{{ msg.sdui.totalExercises }}个动作</span>
  </div>
</div>
```

#### 后端：固化 API

**改造文件**：`AiPlanController.java`

```java
@Operation(summary = "将聊天生成的临时计划固化为正式计划")
@PostMapping("/solidify")
public Result<AiPlanVO> solidify(
    @RequestAttribute("userId") Long userId,
    @RequestBody PlanSolidifyDTO dto
) {
    // dto 包含：临时计划 ID + 用户在聊天中调整后的最终版本号
    return Result.success(aiPlanService.solidifyPlan(userId, dto));
}
```

**逻辑**：
1. 用户点击"固化到我的计划"
2. 后端将临时计划 `AiPlan` 状态从 `DRAFT` → `ACTIVE`
3. 自动填充 `DailyCheckin` 记录（从 `startDate` 到 `endDate` 每一天）
4. 用户第二天打开 `checkin/Calendar.vue` 时，日历已经排满

---

## 三、后端改造方案

### 3.1 新增的 Controller 端点汇总

| 端点 | 方法 | 用途 | 文件 |
|------|------|------|------|
| `/api/dashboard/greeting` | GET | 返回 AI 预测性问候卡片数据 | `DashboardController.java` |
| `/api/food/recognize-text` | POST | 一句话自然语言食物解析 | `FoodRecognitionController.java` |
| `/api/chat/send-with-context` | POST (SSE) | 带页面上下文的 AI 对话 | `ChatController.java` |
| `/api/plan/adjust-stream` | POST (SSE) | 流式调整计划（返回 tool_calls） | `PlanAdjustController.java` |
| `/api/ai-plan/solidify` | POST | 固化临时计划到正式计划 | `AiPlanController.java` |

### 3.2 新增的 Service 方法

| 方法 | 所在 Service | 功能 |
|------|-------------|------|
| `generateGreeting(userId)` | `DashboardService` | 规则引擎生成动态问候卡片 |
| `recognizeByText(userId, text)` | `FoodRecognitionService` | 大模型解析文字食物描述 |
| `adjustWithStream(userId, dto)` | `PlanAdjustService` | 流式调整计划项 |
| `solidifyPlan(userId, dto)` | `AiPlanService` | 临时计划→正式计划+日历填充 |

### 3.3 规则引擎 GreetingRuleEngine

**新文件**：`backend/src/main/java/com/example/engine/GreetingRuleEngine.java`

```java
public class GreetingRuleEngine {

    public static DashboardGreetingVO evaluate(
        SleepRecord lastSleep,
        DailyCheckin todayCheckin,
        AiPlanDetail todayPlan
    ) {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();

        // 早晨 (5:00-10:00)
        if (hour >= 5 && hour < 10) {
            return buildMorningCard(lastSleep, todayPlan);
        }
        // 中午 (10:00-14:00)
        else if (hour >= 10 && hour < 14) {
            return buildNoonCard(todayCheckin);
        }
        // 下午/傍晚 (14:00-20:00)
        else if (hour >= 14 && hour < 20) {
            return buildAfternoonCard(todayPlan);
        }
        // 晚上 (20:00-5:00)
        else {
            if (todayCheckin != null && todayCheckin.getIsCheckedIn()) {
                return buildCelebrationCard(todayCheckin);
            } else {
                return buildReminderCard(todayPlan);
            }
        }
    }

    private static DashboardGreetingVO buildMorningCard(SleepRecord sleep, AiPlanDetail plan) {
        String bodyState = (sleep != null && sleep.getDurationMin() >= 420) ? "良好" : "一般";
        boolean hasPlan = plan != null && plan.getTotalExercises() > 0;

        return DashboardGreetingVO.builder()
            .type("morning")
            .icon("☀️")
            .greeting("早安！")
            .message(hasPlan
                ? "今日身体状态：" + bodyState + "。今晚安排了" + plan.getPlanName() + "训练"
                : "今日身体状态：" + bodyState + "。还没有训练计划，让AI帮你安排？")
            .actions(hasPlan
                ? List.of(new CardAction("查看计划", "/plan/" + plan.getPlanId()))
                : List.of(new CardAction("生成计划", "/plan/generate")))
            .build();
    }

    // ... 其他时段逻辑
}
```

---

## 四、路由与导航重构

### 4.1 Tab Bar 终极形态

```
┌─────────────────────────────────────────────────────────┐
│  MainLayout (左侧栏保留，移动端改为底部 Tab)               │
│                                                         │
│  📊 首页 (Dashboard)       → 预测性看板 + 大盘数据       │
│  🏃 计划 (Plan/Calendar)   → 日历打卡 + 课表详情 + 跟练   │
│  ✨  (中心悬浮 AI 按钮)    → 随时呼出 GlobalCopilot       │
│  🍽️ 饮食 (Food)           → 一句话记账 + 拍照识别        │
│  👤 我的 (Profile)         → 档案 + 报告 + 设置           │
│                                                         │
│  二级入口（我的 → 滑动进入）：                             │
│  ─ 睡眠管理 / 饮水记录 / 身体围度 / 血糖监测              │
│  ─ 数据看板 / 目标里程碑 / 健康社区                       │
│  ─ 计费 / 数据导出 / 通知偏好                             │
└─────────────────────────────────────────────────────────┘
```

### 4.2 路由调整

**改造文件**：`frontend/src/router/index.js`

```js
// 不需要改路由，只需要在 MainLayout 中调整侧边栏菜单的排序和默认显示策略

// 在 MainLayout.vue 的菜单过滤中，新增一个"二级入口"分组
const primaryMenuItems = computed(() =>
  mainLayoutRoutes
    .filter(r => ['Dashboard', 'PlanList', 'FoodRecord'].includes(r.name))
    .map(buildMenuItem)
)

const secondaryMenuItems = computed(() =>
  mainLayoutRoutes
    .filter(r => r.meta?.requiresAuth && !r.meta?.roles
      && !['Dashboard', 'PlanList', 'FoodRecord'].includes(r.name))
    .map(buildMenuItem)
)
```

---

## 五、分阶段实施路线图

### Phase 1：基础通道（第 1-2 周）

| 任务 | 产出 | 工作量 |
|------|------|--------|
| 扩展 `stores/app.js` 全局事件总线 | 支持 `on/off/emit` + Copilot 状态 | 0.5d |
| 改造 `ChatBot.vue` → `GlobalCopilotDrawer.vue` | 底部抽屉，支持上下文感知 | 2d |
| 改造 `MainLayout.vue` 加载方式 | 替换 ChatBot 为 GlobalCopilotDrawer | 0.5d |
| 后端：`/api/chat/send-with-context` (SSE) | 支持页面上下文的 AI 对话 | 1d |

**验收标准**：在任何页面打开 AI 抽屉，AI 知道用户在看什么内容。

### Phase 2：预测性看板（第 3-4 周）

| 任务 | 产出 | 工作量 |
|------|------|--------|
| 新建 `DynamicGreetingCard.vue` | 4 种卡片状态的动态组件 | 1d |
| 后端：`GreetingRuleEngine` + `/api/dashboard/greeting` | 规则引擎生成问候卡片 | 0.5d |
| 改造 `Dashboard.vue` 集成卡片 | 顶部增加 AI 问候卡片 | 0.5d |

**验收标准**：用户早晨打开 App，首页顶部显示个性化的 AI 问候卡片。

### Phase 3：AI 热更新 + 一句话记账（第 5-6 周）

| 任务 | 产出 | 工作量 |
|------|------|--------|
| 新建 `stores/plan.js` | 计划状态 Pinia store | 0.5d |
| 改造 `plan/Detail.vue` 使用 Pinia | 支持热更新 | 1d |
| 后端：`/api/plan/adjust-stream` (SSE) | 流式返回 tool_calls | 1.5d |
| 后端：`/api/food/recognize-text` | 一句话食物解析 | 1d |
| 改造 `food/Record.vue` 增加 NLP 录入 | "一句话记账"功能 | 1d |

**验收标准**：在计划页打开 AI 说"换动作"，页面热更新。在饮食页一句话录入，自动解析并填充。

### Phase 4：SDUI 固化 + 全量测试（第 7-8 周）

| 任务 | 产出 | 工作量 |
|------|------|--------|
| 后端：`/api/ai-plan/solidify` | 临时计划固化 | 1d |
| 聊天消息中嵌入"固化"按钮 | SDUI plan_card 组件 | 1d |
| 全流程联调测试 | 端到端验收 | 2d |

**验收标准**：用户在聊天中生成/调整计划 → 点击固化 → 第二天日历自动排满。

---

## 六、成本影响分析

### 当前成本模型（估算）

| 用户行为 | 调用 LLM 次数 | 每天 Token 估算 |
|----------|--------------|-----------------|
| 打开 Dashboard | 0（无 AI） | 0 |
| 打开 AI 对话 | 1次 | ~5000 tokens |
| 生成计划 | 1次 | ~8000 tokens |
| 总计 | - | ~13000 tokens/天/活跃用户 |

### 改造后成本模型

| 用户行为 | 调用 LLM 次数 | 每天 Token 估算 |
|----------|--------------|-----------------|
| 打开 Dashboard（规则引擎，无 LLM） | 0 | 0 |
| 打开 AI 调整计划（按需） | 0.3次（平均3天1次） | ~2000 tokens |
| 一句话记账 | 1次（文字只解析一次） | ~1000 tokens |
| 总计 | - | ~3000 tokens/天/活跃用户 |

**预期节省**：~75% Token 消耗。

---

## 七、文件变更清单

### 新建文件

| 文件 | 说明 |
|------|------|
| `frontend/src/components/GlobalCopilotDrawer.vue` | 全局 AI 底部抽屉组件 |
| `frontend/src/components/DynamicGreetingCard.vue` | 预测性动态问候卡片 |
| `frontend/src/stores/plan.js` | 计划状态 Pinia store（支持热更新） |
| `backend/src/main/java/com/example/engine/GreetingRuleEngine.java` | 问候卡片规则引擎 |
| `backend/src/main/java/com/example/vo/DashboardGreetingVO.java` | 问候卡片 VO |

### 修改文件

| 文件 | 改动内容 |
|------|----------|
| `frontend/src/stores/app.js` | 新增 copilotOpen, copilotContext, on/off/emit 事件总线 |
| `frontend/src/layout/MainLayout.vue` | 替换 ChatBot 为 GlobalCopilotDrawer，菜单分组优化 |
| `frontend/src/views/Dashboard.vue` | 顶部集成 DynamicGreetingCard |
| `frontend/src/views/plan/Detail.vue` | 改用 Pinia plan store，支持热更新 |
| `frontend/src/views/food/Record.vue` | 新增"一句话记账" NLP 录入 |
| `frontend/src/views/chat/ChatBot.vue` | 可保留向后兼容，新增 context 参数支持 |
| `backend/.../controller/DashboardController.java` | 新增 `/api/dashboard/greeting` |
| `backend/.../controller/FoodRecognitionController.java` | 新增 `/api/food/recognize-text` |
| `backend/.../controller/ChatController.java` | 新增 `/api/chat/send-with-context` |
| `backend/.../controller/PlanAdjustController.java` | 新增 `/api/plan/adjust-stream` |
| `backend/.../controller/AiPlanController.java` | 新增 `/api/ai-plan/solidify` |
| `backend/.../service/DashboardService.java` | 新增 `generateGreeting()` 方法 |
| `backend/.../service/FoodRecognitionService.java` | 新增 `recognizeByText()` 方法 |

---

## 八、不变更的已有资产

以下能力在改造中**完全保留**，不受影响：

- 所有 `admin/` 下管理后台页面
- `HealthForm.vue` / `HealthView.vue` 健康档案
- `SleepRecord.vue` / `WaterRecord.vue` / `BodyMeasurement.vue` / `BloodSugar.vue`
- `GoalMilestones.vue` 目标里程碑
- `CommunityFeed.vue` 健康社区
- `Billing.vue` / `EnterpriseActivate.vue` / `DataExport.vue`
- `NotificationList.vue` 通知中心
- 所有后端 Service / Mapper / Entity（新增方法，不删不改）
- AgentOrchestrator / IntentRouter（扩展，不重构）