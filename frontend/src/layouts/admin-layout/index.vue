<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import {
  NLayout,
  NLayoutHeader,
  NLayoutSider,
  NLayoutContent,
  NMenu,
  NIcon,
  NAvatar,
  NDropdown,
  NBadge,
  NButton,
  NTooltip,
  NDrawer,
  NDrawerContent
} from 'naive-ui';
import { Icon } from '@iconify/vue';
import { useAppStore } from '@/store/modules/app';
import { useAuthStore } from '@/store/modules/auth';
import { useThemeStore } from '@/store/modules/theme';

defineOptions({ name: 'AdminLayout' });

const router = useRouter();
const route = useRoute();
const appStore = useAppStore();
const authStore = useAuthStore();
const themeStore = useThemeStore();

const siderCollapsed = ref(false);
const mobileMenuShow = ref(false);

const activeKey = computed(() => route.name as string);

const isDark = computed(() => themeStore.darkMode);

const adminMenus = computed(() => [
  {
    label: '数据概览',
    key: 'admin_dashboard',
    icon: 'mdi:view-dashboard',
    path: '/admin/dashboard'
  },
  {
    label: '用户管理',
    key: 'admin_user-manage',
    icon: 'mdi:account-group',
    path: '/admin/user-manage'
  },
  {
    label: '内容管理',
    key: 'content',
    icon: 'mdi:file-document-multiple',
    children: [
      { label: '运动库管理', key: 'admin_exercise-manage', path: '/admin/exercise-manage' },
      { label: '食物库管理', key: 'admin_food-manage', path: '/admin/food-manage' },
      { label: '公告管理', key: 'admin_announcement-manage', path: '/admin/announcement-manage' }
    ]
  },
  {
    label: 'AI运营',
    key: 'ai-ops',
    icon: 'mdi:robot',
    children: [
      { label: 'AI反馈管理', key: 'admin_ai-feedback', path: '/admin/ai-feedback' },
      { label: '计划反馈', key: 'admin_plan-feedback', path: '/admin/plan-feedback' },
      { label: 'LLM成本监控', key: 'admin_llm-cost-monitor', path: '/admin/llm-cost-monitor' },
      { label: 'LLM运维', key: 'admin_llm-ops', path: '/admin/llm-ops' }
    ]
  },
  {
    label: '系统管理',
    key: 'system',
    icon: 'mdi:cog',
    children: [
      { label: '审批管理', key: 'admin_approval-manage', path: '/admin/approval-manage' },
      { label: '通知发送', key: 'admin_notification-send', path: '/admin/notification-send' },
      { label: '规则建议', key: 'admin_rule-suggestion', path: '/admin/rule-suggestion' },
      { label: '审计日志', key: 'admin_audit-log', path: '/admin/audit-log' }
    ]
  }
]);

const userOptions = [
  { label: '个人中心', key: 'profile', icon: 'mdi:account' },
  { label: '系统设置', key: 'settings', icon: 'mdi:cog' },
  { type: 'divider', key: 'd1' },
  { label: '退出登录', key: 'logout', icon: 'mdi:logout' }
];

function handleMenuSelect(key: string, item: any) {
  const path = item?.path;
  if (path) {
    router.push(path);
  }
  if (appStore.isMobile) {
    mobileMenuShow.value = false;
  }
}

function handleUserSelect(key: string) {
  if (key === 'logout') {
    authStore.logout();
  } else if (key === 'profile') {
    router.push('/settings/profile');
  } else if (key === 'settings') {
    appStore.openThemeDrawer();
  }
}

function toggleSider() {
  siderCollapsed.value = !siderCollapsed.value;
}

function goToUserSite() {
  router.push('/dashboard');
}

function toggleDark() {
  themeStore.toggleThemeScheme();
}

watch(
  () => appStore.isMobile,
  (isMobile) => {
    if (isMobile) {
      siderCollapsed.value = true;
    }
  }
);

onMounted(() => {
  if (appStore.isMobile) {
    siderCollapsed.value = true;
  }
});
</script>

<template>
  <NLayout class="admin-layout h-full">
    <!-- Sidebar (desktop) -->
    <NLayoutSider
      v-if="!appStore.isMobile"
      bordered
      collapse-mode="width"
      :collapsed="siderCollapsed"
      :collapsed-width="64"
      :width="240"
      show-trigger="bar"
      :native-scrollbar="false"
      @collapse="siderCollapsed = true"
      @expand="siderCollapsed = false"
      class="admin-sider"
    >
      <div class="sider-header">
        <div class="logo" @click="router.push('/admin/dashboard')">
          <div class="logo-icon-wrapper">
            <NIcon size="24" class="logo-icon">
              <Icon icon="mdi:heart-pulse" />
            </NIcon>
          </div>
          <span v-if="!siderCollapsed" class="logo-text">管理后台</span>
        </div>
      </div>
      <div class="sider-menu">
        <NMenu
          :value="activeKey"
          :options="adminMenus"
          :collapsed="siderCollapsed"
          :collapsed-width="64"
          :collapsed-icon-size="22"
          @update:value="handleMenuSelect"
        />
      </div>
      <div v-if="!siderCollapsed" class="sider-footer">
        <span class="footer-text">AI Health System v2.0</span>
      </div>
    </NLayoutSider>

    <!-- Mobile drawer -->
    <NDrawer v-model:show="mobileMenuShow" :width="280" placement="left">
      <NDrawerContent title="管理后台" closable>
        <NMenu
          :value="activeKey"
          :options="adminMenus"
          @update:value="handleMenuSelect"
        />
      </NDrawerContent>
    </NDrawer>

    <!-- Main area -->
    <NLayout>
      <!-- Header -->
      <NLayoutHeader bordered class="admin-header">
        <div class="header-left">
          <NButton v-if="appStore.isMobile" text @click="mobileMenuShow = true" class="header-action">
            <NIcon size="20"><Icon icon="mdi:menu" /></NIcon>
          </NButton>
          <NButton v-else text @click="toggleSider" class="header-action">
            <NIcon size="20">
              <Icon :icon="siderCollapsed ? 'mdi:menu-open' : 'mdi:menu'" />
            </NIcon>
          </NButton>
          <div class="breadcrumb">
            <span class="breadcrumb-label">管理后台</span>
            <Icon icon="mdi:chevron-right" class="breadcrumb-sep" />
            <span class="breadcrumb-current">{{ route.meta.title || '首页' }}</span>
          </div>
        </div>
        <div class="header-right">
          <!-- Theme toggle -->
          <NTooltip placement="bottom">
            <template #trigger>
              <NButton text class="header-action" @click="toggleDark">
                <NIcon size="20">
                  <Icon :icon="isDark ? 'mdi:white-balance-sunny' : 'mdi:weather-night'" />
                </NIcon>
              </NButton>
            </template>
            <span>{{ isDark ? '亮色模式' : '暗色模式' }}</span>
          </NTooltip>

          <!-- User site link -->
          <NTooltip placement="bottom">
            <template #trigger>
              <NButton text class="header-action" @click="goToUserSite">
                <NIcon size="20"><Icon icon="mdi:web" /></NIcon>
              </NButton>
            </template>
            <span>用户端</span>
          </NTooltip>

          <!-- Notifications -->
          <NTooltip placement="bottom">
            <template #trigger>
              <NButton text class="header-action">
                <NBadge :value="3" type="error">
                  <NIcon size="20"><Icon icon="mdi:bell" /></NIcon>
                </NBadge>
              </NButton>
            </template>
            <span>通知</span>
          </NTooltip>

          <!-- Settings -->
          <NTooltip placement="bottom">
            <template #trigger>
              <NButton text class="header-action" @click="appStore.openThemeDrawer">
                <NIcon size="20"><Icon icon="mdi:cog" /></NIcon>
              </NButton>
            </template>
            <span>主题设置</span>
          </NTooltip>

          <!-- User dropdown -->
          <NDropdown :options="userOptions" trigger="click" @select="handleUserSelect">
            <div class="user-chip">
              <NAvatar round size="32" class="user-avatar">
                {{ authStore.userInfo.nickname?.charAt(0) || 'A' }}
              </NAvatar>
              <span v-if="!appStore.isMobile" class="user-name">
                {{ authStore.userInfo.nickname || '管理员' }}
              </span>
            </div>
          </NDropdown>
        </div>
      </NLayoutHeader>

      <!-- Content -->
      <NLayoutContent class="admin-content">
        <div class="content-wrapper">
          <slot />
        </div>
      </NLayoutContent>
    </NLayout>
  </NLayout>
</template>

<style scoped lang="scss">
/* ============================================================
   Admin Layout — Theme-responsive (Dark/Light)
   ============================================================ */

.admin-layout {
  background: var(--sport-bg-base);
  transition: background-color 0.3s ease;
}

/* ---- Sidebar ---- */
.admin-sider {
  background: var(--sport-bg-surface);
  border-right: 1px solid var(--sport-border-subtle);
  transition: background-color 0.3s ease, border-color 0.3s ease;

  :deep(.n-menu) {
    background: transparent;
    border-right: none;
    --n-hover-text-color: var(--sport-primary);
    --n-active-text-color: var(--sport-primary);
    --n-hover-color: var(--sport-primary-subtle);
    --n-active-color: var(--sport-primary-subtle);
    --n-item-border-radius: 10px;
    --n-item-height: 42px;
  }

  :deep(.n-menu-item-content) {
    margin: 2px 8px;
    font-weight: 500;
    transition: all 0.2s ease;
  }

  :deep(.n-menu-item-content--selected) {
    background: var(--sport-primary-subtle) !important;
    color: var(--sport-primary) !important;
    font-weight: 600;

    &::before {
      background: var(--sport-primary) !important;
    }
  }

  :deep(.n-submenu-children > .n-menu-item-content) {
    font-size: 13px;
  }
}

.sider-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid var(--sport-border-subtle);
  padding: 0 16px;
  transition: border-color 0.3s ease;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(1.02);
  }
}

.logo-icon-wrapper {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--gradient-warm);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 3px 12px rgba(255, 107, 53, 0.25);
  flex-shrink: 0;
}

.logo-icon {
  color: #fff;
}

.logo-text {
  font-size: 16px;
  font-weight: 800;
  background: var(--gradient-warm);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  white-space: nowrap;
  letter-spacing: -0.02em;
}

.sider-menu {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.sider-footer {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid var(--sport-border-subtle);
  transition: border-color 0.3s ease;

  .footer-text {
    font-size: 11px;
    color: var(--sport-text-tertiary);
    font-weight: 500;
  }
}

/* ---- Header ---- */
.admin-header {
  height: 60px;
  background: var(--sport-bg-surface);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid var(--sport-border-subtle);
  box-shadow: var(--shadow-sm);
  transition: background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-action {
  padding: 8px;
  border-radius: var(--radius-sm);
  color: var(--sport-text-secondary);
  transition: all 0.2s ease;

  &:hover {
    background: var(--sport-primary-subtle);
    color: var(--sport-primary);
  }
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.breadcrumb-label {
  color: var(--sport-text-tertiary);
  font-weight: 500;
}

.breadcrumb-sep {
  font-size: 14px;
  color: var(--sport-text-tertiary);
}

.breadcrumb-current {
  color: var(--sport-text-primary);
  font-weight: 600;
  transition: color 0.3s ease;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 4px;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all 0.2s ease;
  background: var(--sport-bg-elevated);
  border: 1px solid var(--sport-border-subtle);

  &:hover {
    border-color: var(--sport-primary);
    box-shadow: 0 2px 8px rgba(255, 107, 53, 0.1);
  }
}

.user-avatar {
  background: var(--gradient-warm);
  font-weight: 700;
  color: #fff;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--sport-text-primary);
  transition: color 0.3s ease;
}

/* ---- Content ---- */
.admin-content {
  background: var(--sport-bg-base);
  overflow: auto;
  transition: background-color 0.3s ease;
}

.content-wrapper {
  padding: 24px;
  min-height: calc(100vh - 60px);
}

/* ---- Responsive ---- */
@media (max-width: 768px) {
  .content-wrapper {
    padding: 12px;
  }

  .breadcrumb {
    display: none;
  }
}
</style>
