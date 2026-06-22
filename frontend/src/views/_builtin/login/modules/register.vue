<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRouterPush } from '@/hooks/common/router';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { useCaptcha } from '@/hooks/business/captcha';
import { fetchRegister } from '@/service/api/auth';
import { $t } from '@/locales';

defineOptions({
  name: 'Register'
});

const { toggleLoginModule } = useRouterPush();
const { formRef, validate } = useNaiveForm();
const { label, isCounting, loading, getCaptcha } = useCaptcha();

interface FormModel {
  userName: string;
  phone: string;
  code: string;
  password: string;
  confirmPassword: string;
  disclaimerAccepted: boolean;
}

const model: FormModel = reactive({
  userName: '',
  phone: '',
  code: '',
  password: '',
  confirmPassword: '',
  disclaimerAccepted: false
});

const rules = computed<Record<keyof FormModel, App.Global.FormRule[]>>(() => {
  const { formRules, createConfirmPwdRule } = useFormRules();

  return {
    userName: formRules.userName,
    phone: formRules.phone,
    code: formRules.code,
    password: formRules.pwd,
    confirmPassword: createConfirmPwdRule(model.password),
    disclaimerAccepted: [
      {
        required: true,
        validator: (_rule: any, value: boolean) => {
          if (!value) {
            return new Error($t('page.auth.disclaimerRejectMsg'));
          }
          return true;
        },
        trigger: ['change']
      }
    ]
  };
});

const submitting = ref(false);

async function handleSubmit() {
  await validate();

  if (!model.disclaimerAccepted) {
    window.$message?.warning($t('page.auth.disclaimerRejectMsg'));
    return;
  }

  submitting.value = true;
  try {
    await fetchRegister({
      username: model.userName,
      password: model.password,
      confirmPassword: model.confirmPassword,
      phone: model.phone,
      verifyCode: model.code,
      disclaimerAccepted: model.disclaimerAccepted
    });
    window.$message?.success($t('page.login.common.validateSuccess'));
    toggleLoginModule('pwd-login');
  } catch {
    // Error handled by request interceptor
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <NForm ref="formRef" :model="model" :rules="rules" size="large" :show-label="false" @keyup.enter="handleSubmit">
    <NFormItem path="userName">
      <NInput v-model:value="model.userName" :placeholder="$t('page.login.common.userNamePlaceholder')" />
    </NFormItem>
    <NFormItem path="phone">
      <NInput v-model:value="model.phone" :placeholder="$t('page.login.common.phonePlaceholder')" maxlength="11" />
    </NFormItem>
    <NFormItem path="code">
      <div class="w-full flex-y-center gap-16px">
        <NInput v-model:value="model.code" :placeholder="$t('page.login.common.codePlaceholder')" maxlength="6" class="flex-1" />
        <NButton size="large" :disabled="isCounting" :loading="loading" @click="getCaptcha(model.phone)">
          {{ label }}
        </NButton>
      </div>
    </NFormItem>
    <NFormItem path="password">
      <NInput
        v-model:value="model.password"
        type="password"
        show-password-on="click"
        :placeholder="$t('page.login.common.passwordPlaceholder')"
      />
    </NFormItem>
    <NFormItem path="confirmPassword">
      <NInput
        v-model:value="model.confirmPassword"
        type="password"
        show-password-on="click"
        :placeholder="$t('page.login.common.confirmPasswordPlaceholder')"
      />
    </NFormItem>
    <NFormItem path="disclaimerAccepted">
      <NCheckbox v-model:checked="model.disclaimerAccepted">
        {{ $t('page.login.register.agreement') }}
        <NButton quaternary size="tiny" type="primary" @click.stop>
          {{ $t('page.auth.disclaimer') }}
        </NButton>
      </NCheckbox>
    </NFormItem>
    <NSpace vertical :size="18" class="w-full">
      <NButton type="primary" size="large" round block :loading="submitting" @click="handleSubmit">
        {{ $t('common.confirm') }}
      </NButton>
      <NButton size="large" round block @click="toggleLoginModule('pwd-login')">
        {{ $t('page.login.common.back') }}
      </NButton>
    </NSpace>
  </NForm>
</template>

<style scoped></style>
