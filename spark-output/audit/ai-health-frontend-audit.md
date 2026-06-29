# Audit — AI 健康管理系统前端

- **生成时间**：2026-06-12T16:30:00+08:00
- **走查对象**：代码 `E:\pc-java\AI-Health-System\frontend`（Vue 3.4 + Element Plus + Vite）
- **走查页面数**：14 个主要页面 + 布局/导航/请求层/状态管理
- **走查模式**：自动走查（代码扫描）

## 总览

| 严重度 | 数量 |
| --- | --- |
| Blocker | 3 |
| Major | 16 |
| Minor | 12 |

**按维度分布**：

| 维度 | findings 数 |
| --- | --- |
| visibility（系统状态可见性） | 4 |
| real-world-match（系统与现实匹配） | 1 |
| user-control（用户控制与自由） | 1 |
| consistency（一致性与标准） | 5 |
| error-prevention（错误预防） | 3 |
| recognition（识别优于回忆） | 1 |
| flexibility（灵活性与效率） | 2 |
| aesthetic（美学与极简） | 3 |
| error-recovery（错误恢复） | 4 |
| help-docs（帮助与文档） | 2 |
| responsive（响应式适配） | 3 |
| performance（性能感知） | 2 |

---

## 改版机会点（按优先级排序）

### 高优先级

**机会点 1：建立统一设计系统（Design Tokens + 组件库）**
- 关联 findings：audit-7, audit-8, audit-9, audit-10, audit-17
- 优先级理由：三套色彩体系共存（design tokens / Ant Design 色值 / Element Plus 默认色），无间距比例、无字体阶梯、无共享基础组件（EmptyState / LoadingState / Modal / ConfirmDialog）。这是所有视觉不一致的根因，影响全站每一个页面。
- 影响范围：全站所有页面

**机会点 2：修复移动端可用性（从"完全不可用"到"基本可用"）**
- 关联 findings：audit-24, audit-25, audit-26
- 优先级理由：`body { min-width: 1366px }` 直接让所有移动端适配代码成为死代码。健康管理类产品的移动端使用占比通常在 60%+，这是一个 Blocker 级问题。
- 影响范围：全站移动端

**机会点 3：重做 Dashboard 首页体验**
- 关联 findings：audit-1, audit-2, audit-19, audit-22
- 优先级理由：Dashboard 是用户登录后第一个看到的页面，同时发起 8 个 API 请求却没有任何加载反馈，错误全部静默吞掉。新用户体验为"空白页 → 突然弹出数据"。作为产品门面，需要骨架屏 + 渐进加载 + 空状态引导。
- 影响范围：Dashboard 首页

### 中优先级

**机会点 4：统一数据录入表单体验**
- 关联 findings：audit-11, audit-12, audit-13, audit-14, audit-15
- 优先级理由：饮食/运动/血糖/睡眠/饮水 5 个核心录入页面的表单验证、空状态、错误提示各不相同，且全部缺少 inline validation。这是用户每天高频操作的核心路径。
- 影响范围：5 个数据录入页面

**机会点 5：建立错误处理与恢复机制**
- 关联 findings：audit-19, audit-20, audit-21, audit-22
- 优先级理由：ErrorBoundary 组件已写好但未部署，大量 catch 块为空（依赖拦截器兜底），Statistics 页面用 Promise.all 导致一个接口失败全部崩溃。用户遇到错误时无法重试、不知道发生了什么。
- 影响范围：全站错误场景

**机会点 6：补齐无障碍基础（ARIA + 键盘导航）**
- 关联 findings：audit-27, audit-28, audit-29
- 优先级理由：全站零 aria-label、零 alt text（仅 1 处验证码）、零焦点管理。作为健康类产品，老年用户和视障用户是重要人群。WCAG AA 合规在医疗场景是法规风险。
- 影响范围：全站

### 低优先级

**机会点 7：完善新手引导与帮助体系**
- 关联 findings：audit-30, audit-31
- 优先级理由：无 onboarding 流程、无 inline 帮助。健康类产品用户认知门槛高，需要引导。
- 影响范围：新用户首次使用体验

---

## 完整 Findings 清单

### 维度 1：visibility（系统状态可见性）

1. **[major]** Dashboard 首页无加载状态
   - 出现位置：`views/Dashboard.vue` — 8 个并发 API 请求，无任何 loading/skeleton 反馈
   - 修复建议：使用已有的 `SkeletonScreen` 组件做骨架屏，按卡片粒度渐进加载
   - 修复成本：medium

2. **[major]** 记录类页面数据表无空状态引导
   - 出现位置：`views/food/Record.vue`、`views/exercise/Record.vue`、`views/health/BloodSugar.vue`、`views/water/Record.vue` — 新用户看到空表格，无引导文案
   - 修复建议：封装 `EmptyState` 共享组件，带操作引导（"记录第一顿饮食"、"开始你的第一次运动"）
   - 修复成本：medium

3. **[minor]** 全局进度条为二元状态（on/off）
   - 出现位置：`App.vue` + `stores/app.js` — `pageLoading` 只有 true/false，无法区分加载阶段
   - 修复建议：对 Dashboard 等重量级页面使用 NProgress 百分比或骨架屏替代
   - 修复成本：quick-win

4. **[major]** 饮水记录图表数据不刷新
   - 出现位置：`views/water/Record.vue` — `initChart()` 仅在 onMounted 调用一次，快捷添加饮水后图表不更新
   - 修复建议：将 `initChart()` 加入 watch 或在数据变更后调用 `chart.setOption()`
   - 修复成本：quick-win

### 维度 2：real-world-match（系统与现实匹配）

5. **[minor]** 导出 PDF 实际导出 HTML
   - 出现位置：`views/plan/Detail.vue` — 按钮文案"导出 PDF"但实际生成 HTML blob
   - 修复建议：接入 jsPDF 或 html2pdf.js 生成真实 PDF，或修改按钮文案为"打印预览"
   - 修复成本：medium

### 维度 3：user-control（用户控制与自由）

6. **[minor]** 表单离开无未保存提醒
   - 出现位置：所有数据录入页面 — 用户误操作关闭页面/切换路由，表单数据静默丢失
   - 修复建议：实现 dirty-state 检测 + `beforeRouteLeave` 守卫弹窗提醒
   - 修复成本：medium

### 维度 4：consistency（一致性与标准）

7. **[blocker]** 三套色彩体系共存
   - 出现位置：`styles/global.scss` 定义 `--color-primary: #58a6ff`；`views/Dashboard.vue` 使用 `#1890ff`（Ant Design 色值）；错误页面使用 `#409eff`（Element Plus 默认色）
   - 修复建议：统一为 design tokens，全局替换所有硬编码色值；配置 ESLint 规则禁止内联 hex
   - 修复成本：major-rework

8. **[major]** 无间距比例系统
   - 出现位置：`styles/global.scss` — 16px / 20px / 24px 混用，各组件自定义间距无统一规范
   - 修复建议：定义 spacing tokens（`--space-xs: 4px` ... `--space-3xl: 48px`），全站替换
   - 修复成本：medium

9. **[major]** 无字体阶梯系统
   - 出现位置：`styles/global.scss` — 仅定义 `--text-primary` 和 `--text-secondary`，无 h1-h6/body/caption 字号
   - 修复建议：定义 type scale（`--text-xs: 12px` ... `--text-3xl: 30px`），统一所有标题和正文字号
   - 修复成本：medium

10. **[major]** 表单验证模式不一致
    - 出现位置：Auth 页面使用 `el-form :rules`（inline blur 验证）；饮食/运动/血糖/睡眠/饮水页面使用手动 `if` 检查（仅提交时验证）
    - 修复建议：所有表单统一使用 `el-form :rules` + `trigger: 'blur'` 模式
    - 修复成本：medium

11. **[minor]** 管理后台表单验证模式也不一致
    - 出现位置：`admin/FoodManage.vue`、`admin/ExerciseManage.vue` 使用 `ElMessage.warning` 手动检查，而 `admin/AnnouncementManage.vue` 使用 `el-form :rules`
    - 修复建议：统一到 `el-form :rules` 模式
    - 修复成本：quick-win

### 维度 5：error-prevention（错误预防）

12. **[major]** 数据录入表单无 inline 校验
    - 出现位置：`views/food/Record.vue`、`views/exercise/Record.vue`、`views/health/BloodSugar.vue`、`views/sleep/Record.vue`、`views/water/Record.vue` — 用户点击提交后才知道必填项缺失
    - 修复建议：为每个表单添加 `:rules` 定义，trigger 设为 blur
    - 修复成本：medium

13. **[minor]** 补卡日期无可视提示
    - 出现位置：`views/checkin/Calendar.vue` — 过去 7 天可补卡但日历上无视觉标记区分"可补卡"与"不可补卡"
    - 修复建议：在可补卡日期格子上添加虚线边框或"补"字角标
    - 修复成本：quick-win

14. **[minor]** 血糖趋势图 markPoints 死代码
    - 出现位置：`views/health/BloodSugar.vue` — 计算了异常数据点 markPoints 但未传入 chart.setOption
    - 修复建议：将 markPoints 传入 ECharts series.markPoint 配置
    - 修复成本：quick-win

### 维度 6：recognition（识别优于回忆）

15. **[minor]** 无面包屑导航
    - 出现位置：全站 — 二级页面（如 `/plan/:id`、`/health/view`）无路径回溯导航
    - 修复建议：在 MainLayout 的 header 区域添加 Breadcrumb 组件
    - 修复成本：quick-win

### 维度 7：flexibility（灵活性与效率）

16. **[minor]** 无键盘快捷键
    - 出现位置：全站 — 无全局快捷键（如 Cmd+K 搜索、Cmd+N 新建记录）
    - 修复建议：实现全局 Command Palette（Cmd+K），支持页面跳转和常用操作
    - 修复成本：medium

17. **[minor]** 亮色主题未实现
    - 出现位置：`utils/theme.js` — 有主题切换逻辑但亮色 theme tokens 未定义
    - 修复建议：补齐 `html:not(.dark)` 下的所有 CSS 变量
    - 修复成本：medium

### 维度 8：aesthetic（美学与极简）

18. **[major]** Dashboard 信息密度过高
    - 出现位置：`views/Dashboard.vue` — 707 行单文件，首屏同时展示 greeting + 4 统计卡 + tabs + 2 图表 + 进度 + 评估 + AI 推荐（运动/食物/健康 tips），视觉焦点超过 5 个
    - 修复建议：将 Dashboard 拆为 3-4 个独立组件；推荐区改为懒加载卡片；首屏只展示核心指标
    - 修复成本：major-rework

19. **[minor]** GlobalCopilotDrawer 单文件 1253 行
    - 出现位置：`components/GlobalCopilotDrawer.vue` — 单文件过大，混合了 SSE 通信、UI 渲染、会话管理
    - 修复建议：拆分为 CopilotChat / CopilotSession / CopilotStreaming 等子组件
    - 修复成本：medium

20. **[minor]** Dashboard 图表未复用 BaseChart 组件
    - 出现位置：`views/Dashboard.vue` — 直接使用 `echarts.init()` 而非已封装的 `BaseChart.vue`，重复了 resize/dispose 逻辑
    - 修复建议：替换为 `BaseChart` 组件
    - 修复成本：quick-win

### 维度 9：error-recovery（错误恢复）

21. **[blocker]** ErrorBoundary 组件未部署
    - 出现位置：`components/ErrorBoundary.vue` — 已实现完整的错误捕获 + 重试 UI，但未在任何 `<router-view>` 上使用
    - 修复建议：在 `App.vue` 中包裹 `<router-view>`，或在 MainLayout 的 `<main>` 中包裹
    - 修复成本：quick-win

22. **[major]** Statistics 页面用 Promise.all 而非 Promise.allSettled
    - 出现位置：`views/statistics/Dashboard.vue` — 9 个 API 并发，任一失败则全部图表崩溃
    - 修复建议：改用 `Promise.allSettled`，每个图表独立处理成功/失败
    - 修复成本：quick-win

23. **[major]** 大量 catch 块为空（静默吞错误）
    - 出现位置：`views/Dashboard.vue`（`catch { /* 静默处理 */ }`）、多个页面的 `loadData` 函数 — 拦截器覆盖不到的场景用户完全无反馈
    - 修复建议：统一 catch 策略 — 数据加载失败时显示 inline error + retry 按钮
    - 修复成本：medium

24. **[minor]** 无请求重试机制
    - 出现位置：`utils/request.js` — SSE 客户端有重试（3 次），但常规 API 请求无重试
    - 修复建议：对 GET 请求添加 1-2 次自动重试（指数退避）
    - 修复成本：quick-win

### 维度 10：help-docs（帮助与文档）

25. **[major]** 无新手引导（Onboarding）
    - 出现位置：全站 — 新用户注册后直接进入空白 Dashboard，无功能介绍、无引导步骤
    - 修复建议：实现 3-5 步 onboarding（健康档案 → 生成计划 → 打卡 → 记录饮食 → AI 助手）
    - 修复成本：major-rework

26. **[minor]** 复杂功能缺少 inline 帮助
    - 出现位置：血糖管理的测量类型说明、运动指导、AI 计划调整 — 无"?"图标或说明文字
    - 修复建议：在复杂操作旁添加 tooltip 或 popover 说明
    - 修复成本：quick-win

### 维度 11：responsive（响应式适配）

27. **[blocker]** `body { min-width: 1366px }` 阻断移动端
    - 出现位置：`styles/global.scss:55` — 强制最小视口宽度 1366px，所有 @media 查询成为死代码
    - 修复建议：删除 min-width 约束，改为 `overflow-x: hidden`；逐页适配移动端
    - 修复成本：major-rework

28. **[major]** 页面级组件无响应式布局
    - 出现位置：所有 views/ — 使用固定 `el-col span` 值，无 `:xs/:sm` 断点适配；Checkin Calendar 侧边栏硬编码 340px
    - 修复建议：为所有 `el-row/el-col` 添加响应式断点；使用 flex-wrap 替代固定宽度
    - 修复成本：major-rework

29. **[major]** 移动端侧边栏无遮罩层
    - 出现位置：`layout/MainLayout.vue` — 移动端侧边栏滑出时无 backdrop overlay，无点击外部关闭
    - 修复建议：添加遮罩层 + click-outside 关闭行为
    - 修复成本：quick-win

### 维度 12：performance（性能感知）

30. **[minor]** SkeletonScreen 组件已构建但未使用
    - 出现位置：`components/SkeletonScreen.vue` — 有 shimmer 动画的骨架屏组件，全站零引用
    - 修复建议：在 Dashboard、Statistics、列表页等数据密集页面启用骨架屏
    - 修复成本：quick-win

31. **[minor]** Checkin Calendar heatmap 未正确销毁
    - 出现位置：`views/checkin/Calendar.vue` — 每次 loadData 重新 `echarts.init()` 但未 dispose 旧实例
    - 修复建议：使用 BaseChart 组件或手动在 onBeforeUnmount 中 dispose
    - 修复成本：quick-win

---

## 下一步建议

- **改版方向**：基于机会点 1-3（设计系统 / 移动端 / Dashboard），建议下一步走 Brief 把改版方向沉淀为一页纸设计简报
- **修复优先**：3 个 Blocker 项（色彩统一 / ErrorBoundary 部署 / min-width 移除）建议在任何改版工程启动前先修，成本极低但收益巨大
- **深挖建议**：如需深入了解具体页面的交互流程，可走 `/Web页面设计` 或 `/mobile页面设计` 重新设计核心 flow

---

## 技术资产盘点（已有的但未充分利用）

| 组件 | 状态 | 建议 |
| --- | --- | --- |
| `SkeletonScreen.vue` | 已构建，零引用 | 部署到 Dashboard/Statistics/列表页 |
| `ErrorBoundary.vue` | 已构建，零引用 | 包裹 `<router-view>` |
| `BaseChart.vue` | 已构建，仅 Statistics 使用 | Dashboard/Water/BloodSugar 图表应统一使用 |
| `DynamicGreetingCard.vue` | 仅 Dashboard 使用 | 可扩展到其他欢迎场景 |
| CSS design tokens | 已定义 12 个变量 | 需扩展 spacing + type scale，替换所有硬编码值 |
| `validate.js` 规则库 | 已构建 | 数据录入页面应统一引用 |
