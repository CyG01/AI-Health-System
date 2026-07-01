<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import {
  NLayout,
  NLayoutHeader,
  NLayoutContent,
  NLayoutFooter,
  NIcon,
  NAvatar,
  NDropdown,
  NBadge,
  NButton,
  NTooltip,
  NDrawer,
  NDrawerContent,
  NSwitch
} from 'naive-ui';
import { Icon } from '@iconify/vue';
import { useAppStore } from '@/store/modules/app';
import { useAuthStore } from '@/store/modules/auth';
import { useThemeStore } from '@/store/modules/theme';
import FamilySwitcher from '@/components/FamilySwitcher.vue';

defineOptions({ name: 'UserLayout' });

const router = useRouter();
const route = useRoute();
const appStore = useAppStore();
const authStore = useAuthStore();
const themeStore = useThemeStore();

const mobileMenuShow = ref(false);

const activeKey = computed(() => {
  const path = route.path;
  if (path.startsWith('/dashboard')) return 'dashboard';
  if (path.startsWith('/health')) return 'health';
  if (path.startsWith('/exercise')) return 'exercise';
  if (path.startsWith('/food')) return 'food';
  if (path.startsWith('/plan')) return 'plan';
  if (path.startsWith('/chat')) return 'chat';
  if (path.startsWith('/community')) return 'community';
  if (path.startsWith('/statistics')) return 'statistics';
  return 'dashboard';
});

const userNavItems = [
  { label: '首页', key: 'dashboard', icon: 'mdi:home', path: '/dashboard' },
  { label: '健康档案', key: 'health', icon: 'mdi:heart-pulse', path: '/health/view' },
  { label: '运动', key: 'exercise', icon: 'mdi:run', path: '/exercise/record' },
  { label: '饮食', key: 'food', icon: 'mdi:food-apple', path: '/food/record' },
  { label: '健康计划', key: 'plan', icon: 'mdi:calendar-check', path: '/plan/list' },
  { label: 'AI助手', key: 'chat', icon: 'mdi:robot', path: '/chat/chat-bot' },
  { label: '社区', key: 'community', icon: 'mdi:account-group', path: '/community/feed' },
  { label: '数据统计', key: 'statistics', icon: 'mdi:chart-line', path: '/statistics/dashboard' }
];

const userOptions = [
  { label: '个人中心', key: 'profile', icon: 'mdi:account' },
  { label: '身体数据', key: 'body', icon: 'mdi:scale-bathroom' },
  { label: '目标设置', key: 'goal', icon: 'mdi:target' },
  { label: '消息通知', key: 'notification', icon: 'mdi:bell' },
  { label: '设置', key: 'settings', icon: 'mdi:cog' },
  { type: 'divider', key: 'd1' },
  { label: '管理后台', key: 'admin', icon: 'mdi:view-dashboard' },
  { label: '退出登录', key: 'logout', icon: 'mdi:logout' }
];

function handleNavSelect(key: string) {
  const item = userNavItems.find(i => i.key === key);
  if (item?.path) {
    router.push(item.path);
  }
  if (appStore.isMobile) {
    mobileMenuShow.value = false;
  }
}

function handleUserSelect(key: string) {
  switch (key) {
    case 'logout':
      authStore.logout();
      break;
    case 'profile':
      router.push('/settings/profile');
      break;
    case 'body':
      router.push('/body/measurement');
      break;
    case 'goal':
      router.push('/goal/milestones');
      break;
    case 'notification':
      router.push('/notification/notification-list');
      break;
    case 'settings':
      router.push('/settings/notification-preference');
      break;
    case 'admin':
      router.push('/admin/dashboard');
      break;
  }
}

function goToAdmin() {
  router.push('/admin/dashboard');
}

const isAdmin = computed(() => {
  return authStore.userInfo.roles?.includes('admin') || authStore.userInfo.roles?.includes('super_admin');
});

const isDark = computed(() => themeStore.darkMode);

function toggleDark() {
  themeStore.toggleThemeScheme();
}
</script>

<template>
  <NLayout class="user-layout h-full">
    <!-- Header -->
    <NLayoutHeader class="user-header" :bordered="false">
      <div class="header-container">
        <!-- Logo -->
        <div class="header-left">
          <div class="logo logo-enter" @click="router.push('/dashboard')">
            <div class="logo-icon-wrapper">
              <NIcon size="26" class="logo-icon">
                <Icon icon="mdi:heart-pulse" />
              </NIcon>
            </div>
            <span class="logo-text">健康生活</span>
          </div>
        </div>

        <!-- Desktop Nav -->
        <div class="header-nav">
          <div class="nav-menu">
            <div
              v-for="item in userNavItems"
              :key="item.key"
              class="nav-item"
              :class="{ active: activeKey === item.key }"
              @click="handleNavSelect(item.key)"
            >
              <NIcon size="18" class="nav-icon">
                <Icon :icon="item.icon" />
              </NIcon>
              <span class="nav-text">{{ item.label }}</span>
              <div v-if="activeKey === item.key" class="nav-indicator" />
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="header-right">
          <!-- Dark mode toggle -->
          <NTooltip placement="bottom">
            <template #trigger>
              <NButton text class="action-btn" aria-label="切换暗黑模式" @click="toggleDark">
                <NIcon size="20">
                  <Icon :icon="isDark ? 'mdi:white-balance-sunny' : 'mdi:weather-night'" />
                </NIcon>
              </NButton>
            </template>
            <span>{{ isDark ? '切换亮色' : '切换暗色' }}</span>
          </NTooltip>

          <!-- Notifications -->
          <NTooltip placement="bottom">
            <template #trigger>
              <NButton text class="action-btn" aria-label="通知" @click="router.push('/notification/notification-list')">
                <NBadge :value="5" type="error" :max="99">
                  <NIcon size="20">
                    <Icon icon="mdi:bell-outline" />
                  </NIcon>
                </NBadge>
              </NButton>
            </template>
            <span>消息通知</span>
          </NTooltip>

          <!-- Admin shortcut -->
          <NTooltip v-if="isAdmin" placement="bottom">
            <template #trigger>
              <NButton text class="action-btn admin-btn" aria-label="管理后台" @click="goToAdmin">
                <NIcon size="20">
                  <Icon icon="mdi:shield-crown" />
                </NIcon>
              </NButton>
            </template>
            <span>管理后台</span>
          </NTooltip>

          <!-- Family member switcher -->
          <FamilySwitcher />

          <!-- User avatar -->
          <NDropdown :options="userOptions" trigger="click" @select="handleUserSelect">
            <div class="user-profile">
              <NAvatar round size="36" class="user-avatar">
                {{ authStore.userInfo.nickname?.charAt(0) || 'U' }}
              </NAvatar>
              <div class="user-info">
                <span class="user-name">{{ authStore.userInfo.nickname || '用户' }}</span>
                <span class="user-level">健康达人</span>
              </div>
            </div>
          </NDropdown>

          <!-- Mobile menu -->
          <NButton v-if="appStore.isMobile" text class="mobile-menu-btn" aria-label="打开菜单" @click="mobileMenuShow = true">
            <NIcon size="24">
              <Icon icon="mdi:menu" />
            </NIcon>
          </NButton>
        </div>
      </div>
    </NLayoutHeader>

    <!-- Mobile drawer -->
    <NDrawer v-model:show="mobileMenuShow" :width="300" placement="right" class="mobile-drawer">
      <NDrawerContent title="导航菜单" closable>
        <div class="mobile-nav">
          <div
            v-for="item in userNavItems"
            :key="item.key"
            class="mobile-nav-item"
            :class="{ active: activeKey === item.key }"
            @click="handleNavSelect(item.key)"
          >
            <NIcon size="22" class="nav-icon">
              <Icon :icon="item.icon" />
            </NIcon>
            <span class="nav-text">{{ item.label }}</span>
            <Icon icon="mdi:chevron-right" class="nav-arrow" />
          </div>
        </div>
      </NDrawerContent>
    </NDrawer>

    <!-- Main content -->
    <NLayoutContent class="user-content">
      <div class="content-container">
        <slot />
      </div>
    </NLayoutContent>

    <!-- Footer -->
    <NLayoutFooter class="user-footer">
      <div class="footer-container">
        <div class="footer-left">
          <span class="footer-logo">健康生活</span>
          <span class="footer-copyright">© 2026 AI健康管理系统 版权所有</span>
        </div>
        <div class="footer-right">
          <a href="#" class="footer-link">关于我们</a>
          <a href="#" class="footer-link">使用条款</a>
          <a href="#" class="footer-link">隐私政策</a>
          <a href="#" class="footer-link">帮助中心</a>
        </div>
      </div>
    </NLayoutFooter>
  </NLayout>
</template>

<style scoped lang="scss">
/* ============================================================
   User Layout — "活力运动风" Energetic Sports Theme
   ============================================================ */

.user-layout {
  background: var(--sport-bg-base);
}

/* ---- Header ---- */
.user-header {
  background: var(--sport-bg-surface);
  backdrop-filter: blur(20px);
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: none;
  box-shadow: var(--shadow-sm);
  transition: background-color 0.3s ease, box-shadow 0.3s ease;
}

.user-header::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--gradient-warm);
}

.header-container {
  max-width: 1480px;
  margin: 0 auto;
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
}

/* ---- Logo ---- */
.header-left {
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(1.03);
  }
}

.logo-enter {
  animation: logoEntrance 0.6s ease-out;
}

@keyframes logoEntrance {
  0% { opacity: 0; transform: scale(0.8) rotate(-5deg); }
  60% { transform: scale(1.05) rotate(2deg); }
  100% { opacity: 1; transform: scale(1) rotate(0deg); }
}

.logo-icon-wrapper {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: var(--gradient-warm);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(255, 107, 53, 0.35);
}

.logo-icon {
  color: #fff;
}

.logo-text {
  font-size: 20px;
  font-weight: 800;
  background: var(--gradient-warm);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.02em;
}

/* ---- Desktop Nav ---- */
.header-nav {
  flex: 1;
  display: flex;
  justify-content: center;
}

.nav-menu {
  display: flex;
  align-items: center;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all 0.25s ease;
  color: var(--sport-text-secondary);
  font-size: 14px;
  font-weight: 600;
  position: relative;
  user-select: none;

  &:hover {
    color: var(--sport-primary);
    background: var(--sport-primary-subtle);

    .nav-icon {
      animation: iconWiggle 0.3s ease;
    }
  }

  &.active {
    color: var(--sport-primary);
    background: var(--sport-primary-subtle);
  }
}

.nav-indicator {
  position: absolute;
  bottom: 2px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 3px;
  border-radius: var(--radius-full);
  background: var(--gradient-warm);
  animation: indicatorIn 0.25s ease-out;
}

@keyframes indicatorIn {
  from { width: 0; opacity: 0; }
  to { width: 20px; opacity: 1; }
}

@keyframes iconWiggle {
  0% { transform: rotate(0deg); }
  25% { transform: rotate(-5deg); }
  75% { transform: rotate(5deg); }
  100% { transform: rotate(0deg); }
}

.nav-icon {
  font-size: 18px;
  transition: color 0.2s ease;
}

/* ---- Header Right ---- */
.header-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.action-btn {
  padding: 10px;
  border-radius: var(--radius-sm);
  color: var(--sport-text-secondary);
  transition: all 0.2s ease;

  &:hover {
    background: var(--sport-primary-subtle);
    color: var(--sport-primary);
  }
}

.admin-btn:hover {
  background: rgba(168, 85, 247, 0.1);
  color: var(--sport-accent);
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 14px 5px 5px;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all 0.25s ease;
  background: var(--sport-bg-elevated);
  border: 1px solid var(--sport-border-subtle);

  &:hover {
    border-color: var(--sport-primary);
    box-shadow: 0 2px 12px rgba(255, 107, 53, 0.12);
  }
}

.user-avatar {
  background: var(--gradient-warm);
  font-weight: 700;
  color: #fff;
}

.user-info {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.user-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--sport-text-primary);
}

.user-level {
  font-size: 11px;
  font-weight: 600;
  background: var(--gradient-energy);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.mobile-menu-btn {
  display: none;
  padding: 10px;
  color: var(--sport-text-secondary);
}

/* ---- Content ---- */
.user-content {
  flex: 1;
  overflow-x: hidden;
  background: var(--sport-bg-base);
}

.content-container {
  max-width: 1480px;
  margin: 0 auto;
  padding: 28px 28px 32px;
  min-height: calc(100vh - 68px - 72px);
}

/* ---- Footer ---- */
.user-footer {
  background: var(--sport-bg-surface);
  border-top: 1px solid var(--sport-border-subtle);
  padding: 0;
  transition: background-color 0.3s ease;
}

.user-footer::before {
  content: '';
  display: block;
  height: 2px;
  background: var(--gradient-warm);
  opacity: 0.4;
}

.footer-container {
  max-width: 1480px;
  margin: 0 auto;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.footer-logo {
  font-size: 15px;
  font-weight: 800;
  background: var(--gradient-warm);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.footer-copyright {
  font-size: 13px;
  color: var(--sport-text-tertiary);
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.footer-link {
  font-size: 13px;
  color: var(--sport-text-secondary);
  text-decoration: none;
  transition: color 0.2s ease;
  font-weight: 500;

  &:hover {
    color: var(--sport-primary);
  }
}

/* ---- Mobile Nav ---- */
.mobile-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mobile-nav-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: var(--radius-base);
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--sport-text-primary);

  &:hover {
    background: var(--sport-primary-subtle);
  }

  &.active {
    background: var(--gradient-warm);
    color: #fff;

    .nav-arrow {
      color: #fff;
    }
  }
}

.nav-arrow {
  margin-left: auto;
  color: var(--sport-text-tertiary);
  font-size: 18px;
}

/* ---- Responsive ---- */
@media (max-width: 1024px) {
  .header-nav {
    display: none;
  }

  .mobile-menu-btn {
    display: flex;
  }

  .user-info {
    display: none;
  }
}

@media (max-width: 768px) {
  .header-container {
    height: 60px;
    padding: 0 16px;
  }

  .logo-text {
    font-size: 17px;
  }

  .logo-icon-wrapper {
    width: 36px;
    height: 36px;
    border-radius: 11px;
  }

  .content-container {
    padding: 16px;
  }

  .footer-container {
    flex-direction: column;
    height: auto;
    padding: 20px 16px;
    gap: 14px;
    text-align: center;
  }

  .footer-left {
    flex-direction: column;
    gap: 6px;
  }

  .footer-right {
    flex-wrap: wrap;
    justify-content: center;
    gap: 14px;
  }
}
</style>
