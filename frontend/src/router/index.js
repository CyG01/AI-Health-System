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
const StatisticsDashboard = () => import('@/views/statistics/Dashboard.vue')

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
        path: 'statistics',
        name: 'StatisticsDashboard',
        component: StatisticsDashboard,
        meta: { title: '数据看板', icon: 'PieChart', requiresAuth: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
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

  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next('/dashboard')
    return
  }

  next()
})

router.afterEach(() => {
  const appStore = useAppStore()
  appStore.setPageLoading(false)
})

export default router
