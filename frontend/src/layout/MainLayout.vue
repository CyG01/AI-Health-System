<template>
  <div class="main-layout">
    <!-- 移动端遮罩层 -->
    <div
      v-if="isMobile && !appStore.sidebarCollapsed"
      class="sidebar-overlay"
      @click="appStore.toggleSidebar"
    />

    <aside class="sidebar glass-card" :class="{ collapsed: appStore.sidebarCollapsed, 'mobile-open': isMobile && !appStore.sidebarCollapsed }">
      <div class="logo">
        <el-icon :size="24" color="var(--color-primary)"><FirstAidKit /></el-icon>
        <span v-show="!appStore.sidebarCollapsed" class="logo-text">AI健康管理</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="appStore.sidebarCollapsed"
        background-color="transparent"
        text-color="var(--text-secondary)"
        active-text-color="var(--color-primary)"
        router
      >
        <el-menu-item
          v-for="item in mainMenuItems"
          :key="item.path"
          :index="item.path"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>

        <div class="menu-group-title" v-show="!appStore.sidebarCollapsed && secondaryMenuItems.length > 0">更多功能</div>
        <el-menu-item
          v-for="item in secondaryMenuItems"
          :key="item.path"
          :index="item.path"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>

        <template v-if="isAdmin">
          <div class="menu-group-title" v-show="!appStore.sidebarCollapsed">系统管理</div>
          <el-menu-item
            v-for="item in adminMenuItems"
            :key="item.path"
            :index="item.path"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </aside>

    <div class="main-container">
      <header class="header glass-card">
        <div class="header-left">
          <el-button
            :icon="appStore.sidebarCollapsed ? Expand : Fold"
            text
            @click="appStore.toggleSidebar"
          />
          <!-- 面包屑导航 -->
          <el-breadcrumb separator="/" class="header-breadcrumb">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path" :to="item.path ? { path: item.path } : undefined">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
          <span class="page-title" v-if="breadcrumbs.length === 0">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <el-badge :value="userStore.unreadCount" :hidden="userStore.unreadCount === 0" :max="99" class="header-badge">
            <el-button :icon="Bell" text @click="router.push('/notification')" aria-label="通知" />
          </el-badge>
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-info" role="button" aria-label="用户菜单">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar">
                {{ avatarText }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content" v-loading="appStore.pageLoading">
        <router-view />
      </main>
      <!-- AI智能助手全局抽屉 -->
      <GlobalCopilotDrawer />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Fold, Expand, ArrowDown, Bell } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'

import GlobalCopilotDrawer from '@/components/GlobalCopilotDrawer.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

// 移动端检测
const isMobile = ref(window.innerWidth <= 768)
function handleResize() {
  const wasDesktop = !isMobile.value
  isMobile.value = window.innerWidth <= 768
  // 从桌面切换到移动端时，收起侧边栏
  if (wasDesktop && isMobile.value && !appStore.sidebarCollapsed) {
    appStore.toggleSidebar()
  }
}
onMounted(() => {
  window.addEventListener('resize', handleResize, { passive: true })
  // 移动端默认收起侧边栏
  if (isMobile.value && !appStore.sidebarCollapsed) {
    appStore.toggleSidebar()
  }
})

// 路由切换时自动收起移动端侧边栏
watch(() => route.path, () => {
  if (isMobile.value && !appStore.sidebarCollapsed) {
    appStore.toggleSidebar()
  }
})

// 面包屑导航
const breadcrumbs = computed(() => {
  const matched = route.matched.filter(r => r.meta?.title)
  return matched.map(r => ({
    title: r.meta.title,
    path: r.path
  }))
})

// 从路由配置动态生成菜单项，避免与 router/index.js 双重维护
const mainLayoutRoutes = router.options.routes.find(r => r.path === '/')?.children || []

// 一级菜单：核心页面
const primaryRouteNames = ['Dashboard', 'PlanList', 'FoodRecord']

function buildMenuItem(r) {
  return {
    path: '/' + r.path.replace(/^\//, ''),
    title: r.meta?.title || '',
    icon: r.meta?.icon || 'Menu',
    name: r.name
  }
}

const mainMenuItems = computed(() =>
  mainLayoutRoutes
    .filter(r => r.meta?.requiresAuth && !r.meta?.roles
      && primaryRouteNames.includes(r.name))
    .map(buildMenuItem)
)

const secondaryMenuItems = computed(() =>
  mainLayoutRoutes
    .filter(r => r.meta?.requiresAuth && !r.meta?.roles
      && !primaryRouteNames.includes(r.name))
    .map(buildMenuItem)
)

const adminMenuItems = computed(() =>
  mainLayoutRoutes
    .filter(r => r.meta?.roles?.includes('admin'))
    .map(buildMenuItem)
)

const isAdmin = computed(() => userStore.userInfo?.role === 'admin')
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || 'AI健康管理系统')
const avatarText = computed(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username || 'U'
  return name.charAt(0).toUpperCase()
})

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    try {
      await userStore.logout()
      ElMessage.success('已退出登录')
    } catch {
      userStore.clearAuth()
    }
    router.push('/login')
  }
}

onMounted(() => {
  if (userStore.isLoggedIn) {
    userStore.fetchUnreadCount()
  }
})
</script>

<style scoped lang="scss">
.main-layout {
  display: flex;
  width: 100%;
  height: 100%;
  background: var(--bg-primary);
}

/* 移动端遮罩层 */
.sidebar-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  backdrop-filter: blur(2px);
  animation: fade-overlay 0.2s ease;
}

@keyframes fade-overlay {
  from { opacity: 0; }
  to { opacity: 1; }
}

.sidebar {
  width: 220px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: var(--space-base) 0;
  transition: width 0.25s ease;
  border-right: 1px solid var(--border-color-muted);

  &.collapsed {
    width: 64px;
  }
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 var(--space-lg) var(--space-lg);
}

.logo-text {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: var(--space-base);
  gap: var(--space-base);
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 var(--space-lg);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  min-width: 0;
  flex: 1;
}

.header-breadcrumb {
  :deep(.el-breadcrumb__inner) {
    color: var(--text-secondary);
  }
  :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
    color: var(--text-primary);
    font-weight: 500;
  }
  :deep(.el-breadcrumb__separator) {
    color: var(--text-tertiary);
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}

.header-badge {
  :deep(.el-badge__content) {
    border: 2px solid var(--bg-primary);
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  cursor: pointer;
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-base);
  transition: background 0.2s ease;

  &:hover {
    background: var(--color-primary-bg);
  }
}

.username {
  font-size: var(--text-base);
  color: var(--text-primary);
}

.content {
  flex: 1;
  overflow: auto;
  border-radius: var(--radius-base);
}

.menu-group-title {
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: var(--space-base) var(--space-lg) var(--space-sm);
  border-top: 1px solid var(--border-color-muted);
  margin-top: var(--space-xs);
}

/* ====== 移动端响应式 ====== */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 1000;
    width: 220px;
    transform: translateX(-100%);
    transition: transform 0.3s ease;

    &.mobile-open {
      transform: translateX(0);
    }

    &.collapsed {
      width: 220px;
      transform: translateX(-100%);
    }
  }

  .main-container {
    padding: var(--space-sm);
    gap: var(--space-sm);
  }

  .header {
    height: 48px;
    padding: 0 var(--space-md);
  }

  .username {
    display: none;
  }

  .page-title {
    font-size: var(--text-base);
  }

  .content {
    border-radius: 6px;
  }

  .header-breadcrumb {
    :deep(.el-breadcrumb__inner) {
      font-size: var(--text-sm);
    }
  }
}

@media (max-width: 480px) {
  .header {
    height: 44px;
    padding: 0 var(--space-sm);
  }

  .user-info {
    gap: var(--space-xs);
    padding: 2px var(--space-xs);
  }

  :deep(.el-menu-item) {
    font-size: 13px;
    padding-left: 12px !important;
  }
}
</style>
