<template>
  <div class="auth-page">
    <!-- Wave Background -->
    <div class="wave-bg" />

    <NCard :bordered="false" class="auth-card">
      <div class="auth-content">
        <!-- Header -->
        <header class="auth-header">
          <div class="logo-area">
            <svg xmlns="http://www.w3.org/2000/svg" class="logo-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
            </svg>
          </div>
          <h3 class="system-title">{{ $t('system.title') }}</h3>
          <div class="header-controls">
            <NButton quaternary circle size="small" @click="toggleLocale">
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

        <!-- Title -->
        <div class="auth-title-area">
          <h3 class="auth-title">{{ $t('page.auth.loginTitle') }}</h3>
          <p class="auth-desc">{{ $t('page.auth.loginDesc') }}</p>
        </div>

        <!-- Login Tabs -->
        <NTabs v-model:value="activeTab" type="line" class="auth-tabs" animated>
          <!-- Account Login Tab -->
          <NTabPane name="account" :tab="$t('page.auth.accountLogin')">
            <NForm
              ref="accountFormRef"
              :model="accountModel"
              :rules="accountRules"
              size="large"
              :show-label="false"
              @keyup.enter="handleAccountLogin"
            >
              <NFormItem path="username">
                <NInput
                  v-model:value="accountModel.username"
                  :placeholder="$t('page.auth.usernamePlaceholder')"
                  clearable
                />
              </NFormItem>
              <NFormItem path="password">
                <NInput
                  v-model:value="accountModel.password"
                  type="password"
                  show-password-on="click"
                  :placeholder="$t('page.auth.passwordPlaceholder')"
                />
              </NFormItem>
              <NFormItem path="captchaCode">
                <div class="captcha-row">
                  <NInput
                    v-model:value="accountModel.captchaCode"
                    :placeholder="$t('page.login.pwdLogin.captchaPlaceholder')"
                    maxlength="6"
                    class="captcha-input"
                  />
                  <div
                    class="captcha-img-box"
                    :title="$t('page.login.pwdLogin.refreshCaptcha')"
                    @click="refreshCaptcha"
                  >
                    <img v-if="captchaBase64" :src="captchaBase64" alt="captcha" class="captcha-img" />
                    <div v-else class="captcha-placeholder">
                      <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46C19.54 15.03 20 13.57 20 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74C4.46 8.97 4 10.43 4 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z" />
                      </svg>
                    </div>
                  </div>
                </div>
              </NFormItem>
              <NSpace vertical :size="20">
                <div class="login-options">
                  <NCheckbox :checked="rememberMe" @update:checked="handleRememberMe">
                    {{ $t('page.auth.rememberMeDesc') }}
                  </NCheckbox>
                  <NButton text type="primary" @click="goToForgotPassword">
                    {{ $t('page.auth.forgotPassword') }}
                  </NButton>
                </div>
                <NButton
                  type="primary"
                  size="large"
                  round
                  block
                  :loading="authStore.loginLoading"
                  @click="handleAccountLogin"
                >
                  {{ $t('page.login.common.confirm') }}
                </NButton>
              </NSpace>
            </NForm>
          </NTabPane>

          <!-- Phone Login Tab -->
          <NTabPane name="phone" :tab="$t('page.auth.phoneLogin')">
            <NForm
              ref="phoneFormRef"
              :model="phoneModel"
              :rules="phoneRules"
              size="large"
              :show-label="false"
              @keyup.enter="handlePhoneLogin"
            >
              <NFormItem path="phone">
                <NInput
                  v-model:value="phoneModel.phone"
                  :placeholder="$t('page.auth.phonePlaceholder')"
                  maxlength="11"
                  clearable
                />
              </NFormItem>
              <NFormItem path="code">
                <div class="code-row">
                  <NInput
                    v-model:value="phoneModel.code"
                    :placeholder="$t('page.auth.codePlaceholder')"
                    maxlength="6"
                    class="code-input"
                  />
                  <NButton
                    size="large"
                    :disabled="isCounting"
                    :loading="codeLoading"
                    @click="handleSendCode"
                  >
                    {{ codeLabel }}
                  </NButton>
                </div>
              </NFormItem>
              <NSpace vertical :size="20">
                <div class="login-options">
                  <NCheckbox :checked="rememberMe" @update:checked="handleRememberMe">
                    {{ $t('page.auth.rememberMeDesc') }}
                  </NCheckbox>
                </div>
                <NButton
                  type="primary"
                  size="large"
                  round
                  block
                  :loading="authStore.loginLoading"
                  @click="handlePhoneLogin"
                >
                  {{ $t('page.login.common.confirm') }}
                </NButton>
              </NSpace>
            </NForm>
          </NTabPane>
        </NTabs>

        <!-- Footer Links -->
        <div class="auth-footer">
          <span class="auth-footer-text">{{ $t('page.auth.noAccount') }}</span>
          <NButton text type="primary" @click="goToRegister">
            {{ $t('page.auth.registerNow') }}
          </NButton>
        </div>
      </div>
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { NButton, NCard, NCheckbox, NForm, NFormItem, NInput, NSpace, NTabPane, NTabs, useMessage } from 'naive-ui';
import type { FormInst, FormRules } from 'naive-ui';
import { useAuthStore } from '@/store/modules/auth';
import { useAppStore } from '@/store/modules/app';
import { fetchCaptcha, fetchSendCode } from '@/service/api';
import { REG_CODE_SIX, REG_PHONE, REG_PWD, REG_USER_NAME } from '@/constants/reg';
import { $t } from '@/locales';

defineOptions({ name: 'AuthLogin' });

const router = useRouter();
const message = useMessage();
const authStore = useAuthStore();
const appStore = useAppStore();

// ---------- Locale toggle ----------
function toggleLocale() {
  const target = appStore.locale === 'zh-CN' ? 'en-US' : 'zh-CN';
  appStore.changeLocale(target);
}

// ---------- Tab state ----------
const activeTab = ref<string>('account');

// ---------- Remember me ----------
const rememberMe = ref<boolean>(localStorage.getItem('rememberMe') === 'true');

function handleRememberMe(checked: boolean) {
  rememberMe.value = checked;
  localStorage.setItem('rememberMe', checked ? 'true' : 'false');
}

// ====================== Account Login ======================
const accountFormRef = ref<FormInst | null>(null);

interface AccountFormModel {
  username: string;
  password: string;
  captchaCode: string;
}

const accountModel = reactive<AccountFormModel>({
  username: '',
  password: '',
  captchaCode: ''
});

const accountRules = computed<FormRules>(() => ({
  username: [
    { required: true, message: $t('form.userName.required'), trigger: 'blur' },
    { pattern: REG_USER_NAME, message: $t('form.userName.invalid'), trigger: 'change' }
  ],
  password: [
    { required: true, message: $t('form.pwd.required'), trigger: 'blur' },
    { pattern: REG_PWD, message: $t('form.pwd.invalid'), trigger: 'change' }
  ],
  captchaCode: [
    { required: true, message: $t('page.login.pwdLogin.captchaPlaceholder'), trigger: 'blur' }
  ]
}));

// ---------- Captcha ----------
const captchaBase64 = ref<string>('');
const captchaId = ref<string>('');

async function refreshCaptcha() {
  try {
    captchaBase64.value = '';
    captchaId.value = '';
    const { data, error } = await fetchCaptcha();
    if (!error && data) {
      captchaBase64.value = data.captchaImage || '';
      captchaId.value = data.captchaId || '';
    }
  } catch {
    captchaBase64.value = '';
    captchaId.value = '';
  }
}

async function handleAccountLogin() {
  if (!accountFormRef.value) return;
  try {
    await accountFormRef.value.validate();
  } catch {
    return;
  }
  try {
    await authStore.login(
      accountModel.username,
      accountModel.password,
      accountModel.captchaCode,
      captchaId.value,
      true
    );
  } catch {
    refreshCaptcha();
    accountModel.captchaCode = '';
  }
}

// ====================== Phone Login ======================
const phoneFormRef = ref<FormInst | null>(null);

interface PhoneFormModel {
  phone: string;
  code: string;
}

const phoneModel = reactive<PhoneFormModel>({
  phone: '',
  code: ''
});

const phoneRules = computed<FormRules>(() => ({
  phone: [
    { required: true, message: $t('form.phone.required'), trigger: 'blur' },
    { pattern: REG_PHONE, message: $t('form.phone.invalid'), trigger: 'change' }
  ],
  code: [
    { required: true, message: $t('form.code.required'), trigger: 'blur' },
    { pattern: REG_CODE_SIX, message: $t('form.code.invalid'), trigger: 'change' }
  ]
}));

// ---------- Send Code Countdown ----------
const codeLoading = ref<boolean>(false);
const isCounting = ref<boolean>(false);
const countdown = ref<number>(0);
let countdownTimer: ReturnType<typeof setInterval> | null = null;

const codeLabel = computed<string>(() => {
  if (codeLoading.value) return $t('common.loading');
  if (isCounting.value) return $t('page.auth.reGetCode', { time: countdown.value });
  return $t('page.auth.getCode');
});

function startCountdown() {
  countdown.value = 60;
  isCounting.value = true;
  countdownTimer = setInterval(() => {
    countdown.value -= 1;
    if (countdown.value <= 0) {
      stopCountdown();
    }
  }, 1000);
}

function stopCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer);
    countdownTimer = null;
  }
  isCounting.value = false;
}

async function handleSendCode() {
  if (!phoneModel.phone || !REG_PHONE.test(phoneModel.phone)) {
    message.error($t('form.phone.invalid'));
    return;
  }
  if (codeLoading.value || isCounting.value) return;

  codeLoading.value = true;
  try {
    await fetchSendCode({ phone: phoneModel.phone, type: 'login' });
    message.success($t('page.auth.sendCodeSuccess'));
    startCountdown();
  } catch {
    // Error handled by interceptor
  } finally {
    codeLoading.value = false;
  }
}

async function handlePhoneLogin() {
  if (!phoneFormRef.value) return;
  try {
    await phoneFormRef.value.validate();
  } catch {
    return;
  }
  try {
    await authStore.loginByPhone(phoneModel.phone, phoneModel.code, true);
  } catch {
    // Error handled by interceptor
  }
}

// ====================== Navigation ======================
function goToRegister() {
  router.push('/register');
}

function goToForgotPassword() {
  router.push('/forgot-password');
}

// ====================== Lifecycle ======================
onMounted(() => {
  refreshCaptcha();
});

onUnmounted(() => {
  stopCountdown();
});
</script>

<style scoped>
.auth-page {
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

.auth-card {
  position: relative;
  z-index: 4;
  width: auto;
  border-radius: 12px;
}

.auth-content {
  width: 420px;
}

@media (max-width: 640px) {
  .auth-content {
    width: 320px;
  }
}

.auth-header {
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
  font-size: 20px;
  font-weight: 600;
  color: var(--n-text-color);
  flex: 1;
  text-align: center;
  margin: 0;
}

@media (min-width: 640px) {
  .system-title {
    font-size: 24px;
  }
}

.header-controls {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.auth-title-area {
  padding-top: 24px;
}

.auth-title {
  font-size: 18px;
  font-weight: 500;
  color: var(--n-color-target, #18a058);
  margin: 0;
}

.auth-desc {
  font-size: 13px;
  color: var(--n-text-color-3, #999);
  margin: 6px 0 0;
}

.auth-tabs {
  margin-top: 16px;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.captcha-input {
  flex: 1;
}

.captcha-img-box {
  width: 120px;
  height: 40px;
  flex-shrink: 0;
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--n-border-color, #e0e0e6);
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.04);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.captcha-img-box:hover {
  border-color: var(--n-color-focus, #36ad6a);
  box-shadow: 0 0 8px rgba(54, 173, 106, 0.25);
}

.captcha-img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.captcha-placeholder {
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.code-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.code-input {
  flex: 1;
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.auth-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--n-border-color, #e0e0e6);
}

.auth-footer-text {
  font-size: 13px;
  color: var(--n-text-color-3, #999);
}
</style>
