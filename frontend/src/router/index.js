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
const GoalMilestones = () => import('@/views/goal/Milestones.vue')
const CommunityFeed = () => import('@/views/community/Feed.vue')

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
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const appStore = useAppStore()

  appStore.setPageLoading(true)

  if (to.meta.public) {
    if (userStore.isLoggedIn && (to.path === '/login' || to.path === '/register')) {
      next('/dashboard')
    } else {
      next()
    }
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

  next()
})

router.afterEach(() => {
  const appStore = useAppStore()
  appStore.setPageLoading(false)
})

export default router
