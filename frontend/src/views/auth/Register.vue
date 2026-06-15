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
          <h3 class="auth-title">{{ $t('page.auth.registerTitle') }}</h3>
          <p class="auth-desc">{{ $t('page.auth.registerDesc') }}</p>
        </div>

        <!-- Register Form -->
        <NForm
          ref="formRef"
          :model="model"
          :rules="rules"
          size="large"
          :show-label="false"
          class="auth-form"
          @keyup.enter="handlePreRegister"
        >
          <NFormItem path="username">
            <NInput
              v-model:value="model.username"
              :placeholder="$t('page.auth.usernamePlaceholder')"
              maxlength="20"
              clearable
            />
          </NFormItem>
          <NFormItem path="password">
            <NInput
              v-model:value="model.password"
              type="password"
              show-password-on="click"
              :placeholder="$t('page.auth.passwordPlaceholder')"
              maxlength="20"
            />
          </NFormItem>
          <NFormItem path="confirmPassword">
            <NInput
              v-model:value="model.confirmPassword"
              type="password"
              show-password-on="click"
              :placeholder="$t('page.auth.confirmPasswordPlaceholder')"
              maxlength="20"
            />
          </NFormItem>
          <NFormItem path="phone">
            <NInput
              v-model:value="model.phone"
              :placeholder="$t('page.auth.phonePlaceholder')"
              maxlength="11"
              clearable
            />
          </NFormItem>
          <NFormItem path="code">
            <div class="code-row">
              <NInput
                v-model:value="model.code"
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
            <NButton
              type="primary"
              size="large"
              round
              block
              :loading="loading"
              @click="handlePreRegister"
            >
              {{ $t('page.login.register.title') }}
            </NButton>
          </NSpace>
        </NForm>

        <!-- Footer Links -->
        <div class="auth-footer">
          <span class="auth-footer-text">{{ $t('page.auth.alreadyHaveAccount') }}</span>
          <NButton text type="primary" @click="goToLogin">
            {{ $t('page.auth.backToLogin') }}
          </NButton>
        </div>
      </div>
    </NCard>

    <!-- Medical Disclaimer Modal -->
    <MedicalDisclaimerModal
      :visible="showDisclaimer"
      @confirm="handleDisclaimerConfirm"
      @reject="handleDisclaimerReject"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onUnmounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { NButton, NCard, NForm, NFormItem, NInput, NSpace, useMessage } from 'naive-ui';
import type { FormInst, FormRules } from 'naive-ui';
import { useAppStore } from '@/store/modules/app';
import { fetchRegister, fetchSendCode } from '@/service/api';
import { REG_CODE_SIX, REG_PHONE, REG_PWD, REG_USER_NAME } from '@/constants/reg';
import { $t } from '@/locales';
import MedicalDisclaimerModal from '@/components/MedicalDisclaimerModal.vue';

defineOptions({ name: 'AuthRegister' });

const router = useRouter();
const message = useMessage();
const appStore = useAppStore();

// ---------- Locale toggle ----------
function toggleLocale() {
  const target = appStore.locale === 'zh-CN' ? 'en-US' : 'zh-CN';
  appStore.changeLocale(target);
}

// ====================== Form ======================
const formRef = ref<FormInst | null>(null);
const loading = ref<boolean>(false);
const showDisclaimer = ref<boolean>(false);

interface RegisterFormModel {
  username: string;
  password: string;
  confirmPassword: string;
  phone: string;
  code: string;
}

const model = reactive<RegisterFormModel>({
  username: '',
  password: '',
  confirmPassword: '',
  phone: '',
  code: ''
});

const rules = computed<FormRules>(() => ({
  username: [
    { required: true, message: $t('form.userName.required'), trigger: 'blur' },
    { pattern: REG_USER_NAME, message: $t('form.userName.invalid'), trigger: 'change' }
  ],
  password: [
    { required: true, message: $t('form.pwd.required'), trigger: 'blur' },
    { pattern: REG_PWD, message: $t('form.pwd.invalid'), trigger: 'change' }
  ],
  confirmPassword: [
    { required: true, message: $t('form.confirmPwd.required'), trigger: 'blur' },
    {
      asyncValidator: (_rule: any, value: string) => {
        if (value.trim() !== '' && value !== model.password) {
          return Promise.reject($t('form.confirmPwd.invalid'));
        }
        return Promise.resolve();
      },
      trigger: 'input'
    }
  ],
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
  if (!model.phone || !REG_PHONE.test(model.phone)) {
    message.error($t('form.phone.invalid'));
    return;
  }
  if (codeLoading.value || isCounting.value) return;

  codeLoading.value = true;
  try {
    await fetchSendCode({ phone: model.phone, type: 'register' });
    message.success($t('page.auth.sendCodeSuccess'));
    startCountdown();
  } catch {
    // Error handled by interceptor
  } finally {
    codeLoading.value = false;
  }
}

// ====================== Register Flow ======================

/** Validate form, then show disclaimer modal */
async function handlePreRegister() {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  showDisclaimer.value = true;
}

/** User accepted disclaimer - perform registration */
async function handleDisclaimerConfirm() {
  showDisclaimer.value = false;
  loading.value = true;
  try {
    await fetchRegister({
      username: model.username,
      password: model.password,
      phone: model.phone,
      code: model.code
    });
    message.success($t('page.auth.registerSuccess'));
    await router.push('/login');
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false;
  }
}

/** User rejected disclaimer */
function handleDisclaimerReject() {
  showDisclaimer.value = false;
  message.info($t('page.auth.disclaimerRejectMsg'));
}

// ====================== Navigation ======================
function goToLogin() {
  router.push('/login');
}

// ====================== Lifecycle ======================
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

.auth-form {
  margin-top: 20px;
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
