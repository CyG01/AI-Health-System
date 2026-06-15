<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { loginModuleRecord } from '@/constants/app';
import { useAuthStore } from '@/store/modules/auth';
import { useRouterPush } from '@/hooks/common/router';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { fetchCaptcha } from '@/service/api';
import { $t } from '@/locales';

defineOptions({
  name: 'PwdLogin'
});

const authStore = useAuthStore();
const { toggleLoginModule } = useRouterPush();
const { formRef, validate } = useNaiveForm();

interface FormModel {
  userName: string;
  password: string;
  captchaCode: string;
}

const model: FormModel = reactive({
  userName: '',
  password: '',
  captchaCode: ''
});

const rules = computed<Record<keyof FormModel, App.Global.FormRule[]>>(() => {
  const { formRules, createRequiredRule } = useFormRules();

  return {
    userName: formRules.userName,
    password: formRules.pwd,
    captchaCode: [createRequiredRule($t('page.login.pwdLogin.captchaPlaceholder'))]
  };
});

// Captcha state
const captchaBase64 = ref('');
const captchaUuid = ref('');
const rememberMe = ref(localStorage.getItem('rememberMe') === 'true');

async function refreshCaptcha() {
  try {
    captchaBase64.value = '';
    captchaUuid.value = '';
    const { data, error } = await fetchCaptcha();
    if (data && !error) {
      captchaBase64.value = data.captchaImage || data.base64 || '';
      captchaUuid.value = data.captchaKey || data.uuid || '';
    }
  } catch {
    captchaBase64.value = '';
    captchaUuid.value = '';
  }
}

function handleRememberMe(checked: boolean) {
  rememberMe.value = checked;
  localStorage.setItem('rememberMe', checked ? 'true' : 'false');
}

async function handleSubmit() {
  await validate();
  try {
    await authStore.login(model.userName, model.password, model.captchaCode, captchaUuid.value, rememberMe.value);
    window.$message?.success($t('page.login.common.loginSuccess'));
  } catch {
    refreshCaptcha();
    model.captchaCode = '';
  }
}

onMounted(() => {
  refreshCaptcha();
});
</script>

<template>
  <NForm ref="formRef" :model="model" :rules="rules" size="large" :show-label="false" @keyup.enter="handleSubmit">
    <NFormItem path="userName">
      <NInput v-model:value="model.userName" :placeholder="$t('page.login.common.userNamePlaceholder')" />
    </NFormItem>
    <NFormItem path="password">
      <NInput
        v-model:value="model.password"
        type="password"
        show-password-on="click"
        :placeholder="$t('page.login.common.passwordPlaceholder')"
      />
    </NFormItem>
    <NFormItem path="captchaCode">
      <div class="w-full flex-y-center gap-16px">
        <NInput
          v-model:value="model.captchaCode"
          :placeholder="$t('page.login.pwdLogin.captchaPlaceholder')"
          maxlength="6"
          class="flex-1"
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
    <NSpace vertical :size="24">
      <div class="flex-y-center justify-between">
        <NCheckbox :checked="rememberMe" @update:checked="handleRememberMe">
          {{ $t('page.login.pwdLogin.rememberMe') }}
        </NCheckbox>
        <NButton quaternary @click="toggleLoginModule('reset-pwd')">
          {{ $t('page.login.pwdLogin.forgetPassword') }}
        </NButton>
      </div>
      <NButton type="primary" size="large" round block :loading="authStore.loginLoading" @click="handleSubmit">
        {{ $t('page.login.common.confirm') }}
      </NButton>
      <div class="flex-y-center justify-between gap-12px">
        <NButton class="flex-1" block @click="toggleLoginModule('code-login')">
          {{ $t(loginModuleRecord['code-login']) }}
        </NButton>
        <NButton class="flex-1" block @click="toggleLoginModule('register')">
          {{ $t(loginModuleRecord.register) }}
        </NButton>
      </div>
    </NSpace>
  </NForm>
</template>

<style scoped>
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
</style>
