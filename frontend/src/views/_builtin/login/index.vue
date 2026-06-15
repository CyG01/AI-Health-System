<script setup lang="ts">
import { computed } from 'vue';
import type { Component } from 'vue';
import { loginModuleRecord } from '@/constants/app';
import { useAppStore } from '@/store/modules/app';
import { $t } from '@/locales';
import PwdLogin from './modules/pwd-login.vue';
import CodeLogin from './modules/code-login.vue';
import Register from './modules/register.vue';
import ResetPwd from './modules/reset-pwd.vue';
import BindWechat from './modules/bind-wechat.vue';

interface Props {
  /** The login module */
  module?: LoginModule;
}

const props = withDefaults(defineProps<Props>(), {
  module: 'pwd-login'
});

const appStore = useAppStore();

interface LoginModuleItem {
  label: App.I18n.I18nKey;
  component: Component;
}

const moduleMap: Record<LoginModule, LoginModuleItem> = {
  'pwd-login': { label: loginModuleRecord['pwd-login'], component: PwdLogin },
  'code-login': { label: loginModuleRecord['code-login'], component: CodeLogin },
  register: { label: loginModuleRecord.register, component: Register },
  'reset-pwd': { label: loginModuleRecord['reset-pwd'], component: ResetPwd },
  'bind-wechat': { label: loginModuleRecord['bind-wechat'], component: BindWechat }
};

const activeModule = computed(() => moduleMap[props.module]);
</script>

<template>
  <div class="login-page">
    <!-- Wave Background -->
    <div class="wave-bg" />

    <NCard :bordered="false" class="login-card">
      <div class="login-content">
        <header class="login-header">
          <div class="logo-area">
            <svg xmlns="http://www.w3.org/2000/svg" class="logo-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
            </svg>
          </div>
          <h3 class="system-title">{{ $t('system.title') }}</h3>
          <div class="header-controls">
            <!-- Language Switch -->
            <NButton quaternary circle size="small" @click="appStore.changeLocale(appStore.locale === 'zh-CN' ? 'en-US' : 'zh-CN')">
              <template #icon>
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="2" y1="12" x2="22" y2="12" />
                  <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
                </svg>
              </template>
            </NButton>
          </div>
        </header>

        <main class="login-main">
          <h3 class="module-title">{{ $t(activeModule.label) }}</h3>
          <div class="module-content">
            <Transition name="fade-slide" mode="out-in" appear>
              <component :is="activeModule.component" />
            </Transition>
          </div>
        </main>
      </div>
    </NCard>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.wave-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    radial-gradient(ellipse at 20% 50%, rgba(120, 119, 198, 0.3) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 20%, rgba(255, 119, 198, 0.15) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 80%, rgba(120, 219, 198, 0.15) 0%, transparent 50%);
}

.wave-bg::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40%;
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1440 320'%3E%3Cpath fill='rgba(255,255,255,0.05)' d='M0,160L48,170.7C96,181,192,203,288,192C384,181,480,139,576,133.3C672,128,768,160,864,181.3C960,203,1056,213,1152,208C1248,203,1344,181,1392,170.7L1440,160L1440,320L1392,320C1344,320,1248,320,1152,320C1056,320,960,320,864,320C768,320,672,320,576,320C480,320,384,320,288,320C192,320,96,320,48,320L0,320Z'/%3E%3C/svg%3E") no-repeat bottom;
  background-size: cover;
}

.login-card {
  position: relative;
  z-index: 4;
  width: auto;
  border-radius: 12px;
}

.login-content {
  width: 400px;
}

@media (max-width: 640px) {
  .login-content {
    width: 300px;
  }
}

.login-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo-area {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.logo-icon {
  width: 28px;
  height: 28px;
}

.system-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--n-text-color);
  flex: 1;
  text-align: center;
  margin: 0;
}

@media (min-width: 640px) {
  .system-title {
    font-size: 28px;
  }
}

.header-controls {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.login-main {
  padding-top: 24px;
}

.module-title {
  font-size: 18px;
  font-weight: 500;
  color: var(--n-color-target, #18a058);
  margin: 0;
}

.module-content {
  padding-top: 24px;
}

/* Transition */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(10px);
}
</style>
