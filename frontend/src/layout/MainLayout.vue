<template>
  <div class="main-layout">
    <aside class="sidebar glass-card" :class="{ collapsed: appStore.sidebarCollapsed }">
      <div class="logo">
        <el-icon :size="24" color="#58a6ff"><FirstAidKit /></el-icon>
        <span v-show="!appStore.sidebarCollapsed" class="logo-text">AI健康管理</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="appStore.sidebarCollapsed"
        background-color="transparent"
        text-color="#8b949e"
        active-text-color="#58a6ff"
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
          <span class="page-title">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <el-badge :value="userStore.unreadCount" :hidden="userStore.unreadCount === 0" :max="99" class="header-badge">
            <el-button :icon="'Bell'" text @click="router.push('/notification')" />
          </el-badge>
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-info">
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
      <!-- AI健康咨询浮动机器人 -->
      <ChatBot />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Fold, Expand, ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import ChatBot from '@/views/chat/ChatBot.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const mainMenuItems = [
  { path: '/dashboard', title: '工作台', icon: 'Odometer' },
  { path: '/checkin/calendar', title: '每日打卡', icon: 'Calendar' },
  { path: '/food', title: '饮食记录', icon: 'Dish' },
  { path: '/water', title: '饮水记录', icon: 'Dish' },
  { path: '/exercise', title: '运动记录', icon: 'Bicycle' },
  { path: '/sleep', title: '睡眠管理', icon: 'Moon' },
  { path: '/body-measurement', title: '身体围度', icon: 'DataLine' },
  { path: '/goal', title: '目标里程碑', icon: 'Trophy' },
  { path: '/statistics', title: '数据看板', icon: 'PieChart' },
  { path: '/health/view', title: '健康档案', icon: 'Monitor' },
  { path: '/health/report', title: 'AI健康报告', icon: 'Document' },
  { path: '/plan/list', title: 'AI计划', icon: 'MagicStick' },
  { path: '/community', title: '健康社区', icon: 'ChatDotRound' },
  { path: '/notification', title: '通知中心', icon: 'Bell' },
  { path: '/profile', title: '个人中心', icon: 'User' }
]

const adminMenuItems = [
  { path: '/admin/user', title: '用户管理', icon: 'UserFilled' },
  { path: '/admin/announcement', title: '公告管理', icon: 'Notification' },
  { path: '/admin/food', title: '食物字典', icon: 'Dish' },
  { path: '/admin/exercise', title: '运动字典', icon: 'Bicycle' },
  { path: '/admin/notification', title: '发送通知', icon: 'Message' },
  { path: '/admin/feedback', title: '计划反馈', icon: 'ChatDotSquare' },
  { path: '/admin/audit', title: '审计日志', icon: 'Document' }
]

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

.sidebar {
  width: 220px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 16px 0;
  transition: width 0.25s ease;
  border-right: 1px solid rgba(48, 54, 61, 0.5);

  &.collapsed {
    width: 64px;
  }
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px 20px;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 16px;
  gap: 16px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.header-badge {
  :deep(.el-badge__content) {
    border: 2px solid #0d1117;
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-base);
  transition: background 0.2s ease;

  &:hover {
    background: rgba(88, 166, 255, 0.08);
  }
}

.username {
  font-size: 14px;
  color: var(--text-primary);
}

.content {
  flex: 1;
  overflow: auto;
  border-radius: var(--radius-base);
}

.menu-group-title {
  font-size: 11px;
  font-weight: 600;
  color: #484f58;
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 16px 20px 8px;
  border-top: 1px solid rgba(48, 54, 61, 0.5);
  margin-top: 4px;
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

    &.collapsed {
      width: 220px;
      transform: translateX(0);
    }
  }

  .main-container {
    padding: 8px;
    gap: 8px;
  }

  .header {
    height: 48px;
    padding: 0 12px;
  }

  .username {
    display: none;
  }

  .page-title {
    font-size: 14px;
  }

  .content {
    border-radius: 6px;
  }
}

@media (max-width: 480px) {
  .header {
    height: 44px;
    padding: 0 8px;
  }

  .user-info {
    gap: 4px;
    padding: 2px 4px;
  }

  :deep(.el-menu-item) {
    font-size: 13px;
    padding-left: 12px !important;
  }
}
</style>
