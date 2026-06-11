import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'

const MainLayout = () => import('@/layout/MainLayout.vue')
const Login = () => import('@/views/auth/Login.vue')
const Register = () => import('@/views/auth/Register.vue')
const ForgotPassword = () => import('@/views/auth/ForgotPassword.vue')
const Dashboard = () => import('@/views/Dashboard.vue')
const Profile = () => import('@/views/auth/Profile.vue')
const HealthForm = () => import('@/views/health/Form.vue')
const HealthCreate = () => import('@/views/health/Create.vue')
const HealthView = () => import('@/views/health/View.vue')
const PlanGenerate = () => import('@/views/plan/Generate.vue')
const PlanList = () => import('@/views/plan/List.vue')
const PlanDetail = () => import('@/views/plan/Detail.vue')
const CheckinCalendar = () => import('@/views/checkin/Calendar.vue')
const FoodRecord = () => import('@/views/food/Record.vue')
const ExerciseRecord = () => import('@/views/exercise/Record.vue')
const StatisticsDashboard = () => import('@/views/statistics/Dashboard.vue')
const UserManage = () => import('@/views/admin/UserManage.vue')
const AnnouncementManage = () => import('@/views/admin/AnnouncementManage.vue')
const FoodManage = () => import('@/views/admin/FoodManage.vue')
const ExerciseManage = () => import('@/views/admin/ExerciseManage.vue')
const NotificationSend = () => import('@/views/admin/NotificationSend.vue')
const PlanFeedback = () => import('@/views/admin/PlanFeedback.vue')
const AuditLog = () => import('@/views/admin/AuditLog.vue')
const NotificationList = () => import('@/views/notification/NotificationList.vue')
const SleepRecord = () => import('@/views/sleep/Record.vue')
const HealthReport = () => import('@/views/health/Report.vue')
const WaterRecord = () => import('@/views/water/Record.vue')
const BodyMeasurement = () => import('@/views/body/Measurement.vue')
const BloodSugar = () => import('@/views/health/BloodSugar.vue')
const GoalMilestones = () => import('@/views/goal/Milestones.vue')
const CommunityFeed = () => import('@/views/community/Feed.vue')
const ChatBot = () => import('@/views/chat/ChatBot.vue')
const Billing = () => import('@/views/billing/Billing.vue')
const RefundInvoice = () => import('@/views/billing/RefundInvoice.vue')
const EnterpriseActivate = () => import('@/views/enterprise/Activate.vue')
const DataExport = () => import('@/views/export/Export.vue')
const AdminApproval = () => import('@/views/admin/ApprovalManage.vue')
const AdminRuleSuggestion = () => import('@/views/admin/RuleSuggestion.vue')
const AdminAiFeedback = () => import('@/views/admin/AiFeedback.vue')
const NotificationPreference = () => import('@/views/settings/NotificationPreference.vue')

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录', public: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: { title: '注册', public: true }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: ForgotPassword,
    meta: { title: '忘记密码', public: true }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard,
        meta: { title: '工作台', icon: 'Odometer', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: Profile,
        meta: { title: '个人中心', icon: 'User', requiresAuth: true }
      },
      {
        path: 'health/create',
        name: 'HealthCreate',
        component: HealthCreate,
        meta: { title: '创建健康档案', icon: 'Edit', requiresAuth: true }
      },
      {
        path: 'health/view',
        name: 'HealthView',
        component: HealthView,
        meta: { title: '健康档案', icon: 'Monitor', requiresAuth: true }
      },
      {
        path: 'health/form',
        name: 'HealthForm',
        component: HealthForm,
        meta: { title: '编辑健康档案', icon: 'Edit', requiresAuth: true }
      },
      {
        path: 'plan/generate',
        name: 'PlanGenerate',
        component: PlanGenerate,
        meta: { title: '生成AI计划', icon: 'MagicStick', requiresAuth: true }
      },
      {
        path: 'plan/list',
        name: 'PlanList',
        component: PlanList,
        meta: { title: 'AI计划列表', icon: 'List', requiresAuth: true }
      },
      {
        path: 'plan/:id',
        name: 'PlanDetail',
        component: PlanDetail,
        meta: { title: '计划详情', icon: 'Document', requiresAuth: true }
      },
      {
        path: 'checkin/calendar',
        name: 'CheckinCalendar',
        component: CheckinCalendar,
        meta: { title: '每日打卡', icon: 'Calendar', requiresAuth: true }
      },
      {
        path: 'food',
        name: 'FoodRecord',
        component: FoodRecord,
        meta: { title: '饮食记录', icon: 'Dish', requiresAuth: true }
      },
      {
        path: 'exercise',
        name: 'ExerciseRecord',
        component: ExerciseRecord,
        meta: { title: '运动记录', icon: 'Bicycle', requiresAuth: true }
      },
      {
        path: 'statistics',
        name: 'StatisticsDashboard',
        component: StatisticsDashboard,
        meta: { title: '数据看板', icon: 'PieChart', requiresAuth: true }
      },
      {
        path: 'admin/user',
        name: 'UserManage',
        component: UserManage,
        meta: { title: '用户管理', icon: 'UserFilled', requiresAuth: true, roles: ['admin'] }
      },
      {
        path: 'admin/announcement',
        name: 'AnnouncementManage',
        component: AnnouncementManage,
        meta: { title: '公告管理', icon: 'Notification', requiresAuth: true, roles: ['admin'] }
      },
      {
        path: 'admin/food',
        name: 'FoodManage',
        component: FoodManage,
        meta: { title: '食物字典', icon: 'Dish', requiresAuth: true, roles: ['admin'] }
      },
      {
        path: 'admin/exercise',
        name: 'ExerciseManage',
        component: ExerciseManage,
        meta: { title: '运动字典', icon: 'Bicycle', requiresAuth: true, roles: ['admin'] }
      },
      {
        path: 'admin/notification',
        name: 'NotificationSend',
        component: NotificationSend,
        meta: { title: '发送通知', icon: 'Message', requiresAuth: true, roles: ['admin'] }
      },
      {
        path: 'admin/feedback',
        name: 'PlanFeedback',
        component: PlanFeedback,
        meta: { title: '计划反馈', icon: 'ChatDotSquare', requiresAuth: true, roles: ['admin'] }
      },
      {
        path: 'admin/audit',
        name: 'AuditLog',
        component: AuditLog,
        meta: { title: '审计日志', icon: 'Document', requiresAuth: true, roles: ['admin'] }
      },
      {
        path: 'notification',
        name: 'NotificationList',
        component: NotificationList,
        meta: { title: '通知中心', icon: 'Bell', requiresAuth: true }
      },
      {
        path: 'sleep',
        name: 'SleepRecord',
        component: SleepRecord,
        meta: { title: '睡眠管理', icon: 'Moon', requiresAuth: true }
      },
      {
        path: 'health/report',
        name: 'HealthReport',
        component: HealthReport,
        meta: { title: 'AI健康报告', icon: 'Document', requiresAuth: true }
      },
      {
        path: 'water',
        name: 'WaterRecord',
        component: WaterRecord,
        meta: { title: '饮水记录', icon: 'Dish', requiresAuth: true }
      },
      {
        path: 'body-measurement',
        name: 'BodyMeasurement',
        component: BodyMeasurement,
        meta: { title: '身体围度', icon: 'DataLine', requiresAuth: true }
      },
      {
        path: 'blood-sugar',
        name: 'BloodSugar',
        component: BloodSugar,
        meta: { title: '血糖监测', icon: 'Odometer', requiresAuth: true }
      },
      {
        path: 'goal',
        name: 'GoalMilestones',
        component: GoalMilestones,
        meta: { title: '目标里程碑', icon: 'Trophy', requiresAuth: true }
      },
      {
        path: 'community',
        name: 'CommunityFeed',
        component: CommunityFeed,
        meta: { title: '健康社区', icon: 'ChatDotRound', requiresAuth: true }
      },
      {
        path: 'chat',
        name: 'ChatBot',
        component: ChatBot,
        meta: { title: 'AI健康助手', icon: 'Service', requiresAuth: true }
      },
      {
        path: 'billing',
        name: 'Billing',
        component: Billing,
        meta: { title: '计费与消费', icon: 'Money', requiresAuth: true }
      },
      {
        path: 'billing/refund-invoice',
        name: 'RefundInvoice',
        component: RefundInvoice,
        meta: { title: '退款与发票', icon: 'Receipt', requiresAuth: true }
      },
      {
        path: 'enterprise',
        name: 'EnterpriseActivate',
        component: EnterpriseActivate,
        meta: { title: '企业版订阅', icon: 'OfficeBuilding', requiresAuth: true }
      },
      {
        path: 'export',
        name: 'DataExport',
        component: DataExport,
        meta: { title: '数据导出', icon: 'Download', requiresAuth: true }
      },
      {
        path: 'settings/notification',
        name: 'NotificationPreference',
        component: NotificationPreference,
        meta: { title: '通知偏好', icon: 'Setting', requiresAuth: true }
      },
      {
        path: 'admin/approval',
        name: 'AdminApproval',
        component: AdminApproval,
        meta: { title: '审批管理', icon: 'Checked', requiresAuth: true, roles: ['admin'] }
      },
      {
        path: 'admin/rule-suggestion',
        name: 'AdminRuleSuggestion',
        component: AdminRuleSuggestion,
        meta: { title: '规则建议审核', icon: 'Warning', requiresAuth: true, roles: ['admin'] }
      },
      {
        path: 'admin/ai-feedback',
        name: 'AdminAiFeedback',
        component: AdminAiFeedback,
        meta: { title: 'AI反馈审核', icon: 'ChatLineSquare', requiresAuth: true, roles: ['admin'] }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  // 滚动行为优化
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    if (to.hash) {
      return { el: to.hash, behavior: 'smooth' }
    }
    return { top: 0, behavior: 'smooth' }
  }
})

// 避免重复初始化
let authInitialized = false
let lastAuthInit = 0
const AUTH_INIT_INTERVAL = 30000 // 30秒内不重复初始化

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const appStore = useAppStore()

  // 页面加载过渡
  appStore.setPageLoading(true)

  // 防抖：短期内不重复调用 initAuth
  const now = Date.now()
  if (!authInitialized || (!from.name && now - lastAuthInit > AUTH_INIT_INTERVAL)) {
    await userStore.initAuth()
    authInitialized = true
    lastAuthInit = now
  }

  if (to.meta.public) {
    if (userStore.isLoggedIn && (to.path === '/login' || to.path === '/register')) {
      next('/dashboard')
      return
    }
    next()
    return
  }

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.roles && Array.isArray(to.meta.roles)) {
    const userRole = userStore.userInfo?.role
    if (!to.meta.roles.includes(userRole)) {
      next('/dashboard')
      return
    }
  }

  // 首次进入受保护页面时，启动 Token 主动刷新定时器
  if (userStore.isLoggedIn) {
    userStore.scheduleTokenRefresh()
  }

  next()
})

// 路由切换完成，关闭加载状态
router.afterEach(() => {
  const appStore = useAppStore()
  requestAnimationFrame(() => {
    appStore.setPageLoading(false)
  })
})

export default router
