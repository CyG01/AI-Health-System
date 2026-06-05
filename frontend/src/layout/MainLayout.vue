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
          v-for="item in menuItems"
          :key="item.path"
          :index="item.path"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
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
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Fold, Expand, ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const menuItems = [
  { path: '/dashboard', title: '工作台', icon: 'Odometer' },
  { path: '/profile', title: '个人中心', icon: 'User' }
]

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
</style>
