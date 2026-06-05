<template>
  <div class="auth-page">
    <div class="auth-card glass-card">
      <h2 class="page-title">用户注册</h2>
      <p class="page-desc">创建您的 AI 健康管理账号</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="auth-form">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" maxlength="20" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password maxlength="20" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password maxlength="20" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="验证码" prop="verifyCode">
          <div class="code-row">
            <el-input v-model="form.verifyCode" placeholder="请输入验证码" maxlength="6" />
            <el-button :disabled="codeSending" @click="handleSendCode">{{ codeButtonText }}</el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" :disabled="loading" class="submit-btn" @click="handleRegister">
            注册
          </el-button>
        </el-form-item>
      </el-form>
      <div class="auth-links">
        <router-link to="/login">已有账号，去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register, sendCode } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import {
  usernameRules,
  passwordRules,
  phoneRules,
  verifyCodeRules,
  createConfirmPasswordRule
} from '@/utils/validate'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const codeSending = ref(false)
const countdown = ref(0)
let countdownTimer = null

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  phone: '',
  verifyCode: ''
})

const rules = {
  username: usernameRules,
  password: passwordRules,
  confirmPassword: [createConfirmPasswordRule(() => form.password)],
  phone: phoneRules,
  verifyCode: verifyCodeRules
}

const codeButtonText = computed(() => {
  return countdown.value > 0 ? `${countdown.value}s后重试` : '获取验证码'
})

function startCountdown() {
  countdown.value = 60
  codeSending.value = true
  countdownTimer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
      codeSending.value = false
    }
  }, 1000)
}

async function handleSendCode() {
  if (!formRef.value) return
  try {
    await formRef.value.validateField('phone')
  } catch {
    return
  }
  try {
    await sendCode({ phone: form.phone })
    ElMessage.success('验证码已发送')
    startCountdown()
  } catch {
    codeSending.value = false
  }
}

async function handleRegister() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await register({
        username: form.username,
        password: form.password,
        confirmPassword: form.confirmPassword,
        phone: form.phone,
        verifyCode: form.verifyCode
      })
      userStore.setAuth(res.data)
      ElMessage.success('注册成功')
      await router.push('/dashboard')
    } finally {
      loading.value = false
    }
  })
}

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<style scoped lang="scss">
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: var(--bg-primary);
}

.auth-card {
  width: 420px;
  padding: 40px;
}

.auth-form {
  margin-top: 24px;
}

.code-row {
  display: flex;
  gap: 12px;
  width: 100%;

  .el-input {
    flex: 1;
  }
}

.submit-btn {
  width: 100%;
}

.auth-links {
  margin-top: 16px;
  font-size: 12px;
}
</style>
