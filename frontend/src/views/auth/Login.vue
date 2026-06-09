<template>
  <div class="auth-page">
    <div class="auth-card glass-card">
      <h2 class="page-title">用户登录</h2>
      <p class="page-desc">登录 AI 健康管理系统</p>
      <el-tabs v-model="activeTab" class="auth-tabs">
        <el-tab-pane label="账号登录" name="account">
          <el-form ref="accountFormRef" :model="accountForm" :rules="accountRules" label-position="top" class="auth-form">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="accountForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="accountForm.password" type="password" placeholder="请输入密码" show-password />
            </el-form-item>
            <el-form-item label="验证码" prop="captchaCode">
              <div class="captcha-row">
                <el-input v-model="accountForm.captchaCode" placeholder="请输入验证码" maxlength="6" />
                <div class="captcha-img-box" @click="refreshCaptcha" title="点击刷新验证码">
                  <img v-if="captchaBase64" :src="captchaBase64" alt="验证码" class="captcha-img" />
                  <div v-else class="captcha-placeholder">
                    <el-icon :size="20"><Refresh /></el-icon>
                  </div>
                </div>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" :disabled="loading" class="submit-btn" @click="handleAccountLogin">
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="验证码登录" name="phone">
          <el-form ref="phoneFormRef" :model="phoneForm" :rules="phoneFormRules" label-position="top" class="auth-form">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="phoneForm.phone" placeholder="请输入手机号" maxlength="11" />
            </el-form-item>
            <el-form-item label="验证码" prop="verifyCode">
              <div class="code-row">
                <el-input v-model="phoneForm.verifyCode" placeholder="请输入验证码" maxlength="6" />
                <el-button :disabled="codeSending" @click="handleSendCode">{{ codeButtonText }}</el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" :disabled="loading" class="submit-btn" @click="handlePhoneLogin">
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <div class="auth-links">
        <router-link to="/register">注册账号</router-link>
        <router-link to="/forgot-password">忘记密码</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getCaptcha, login, loginByPhone, sendCode } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { usernameRules, passwordRules, phoneRules as phoneFieldRules, verifyCodeRules } from '@/utils/validate'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeTab = ref('account')
const accountFormRef = ref(null)
const phoneFormRef = ref(null)
const loading = ref(false)
const codeSending = ref(false)
const countdown = ref(0)
let countdownTimer = null

const captchaBase64 = ref('')
const captchaUuid = ref('')

const accountForm = reactive({
  username: '',
  password: '',
  captchaCode: ''
})

const phoneForm = reactive({
  phone: '',
  verifyCode: ''
})

const accountRules = {
  username: usernameRules,
  password: passwordRules,
  captchaCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

const phoneFormRules = {
  phone: phoneFieldRules,
  verifyCode: verifyCodeRules
}

const codeButtonText = computed(() => {
  return countdown.value > 0 ? `${countdown.value}s后重试` : '获取验证码'
})

async function fetchCaptcha() {
  try {
    const res = await getCaptcha()
    if (res.data) {
      captchaBase64.value = res.data.base64
      captchaUuid.value = res.data.uuid
    }
  } catch {
    captchaBase64.value = ''
    captchaUuid.value = ''
  }
}

function refreshCaptcha() {
  captchaBase64.value = ''
  captchaUuid.value = ''
  fetchCaptcha()
}

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
  if (!phoneFormRef.value) return
  try {
    await phoneFormRef.value.validateField('phone')
  } catch {
    return
  }
  try {
    await sendCode({ phone: phoneForm.phone })
    ElMessage.success('验证码已发送')
    startCountdown()
  } catch {
    codeSending.value = false
  }
}

async function handleLoginSuccess(res) {
  userStore.setAuth(res.data)
  userStore.fetchUnreadCount()
  ElMessage.success('登录成功')
  const redirect = route.query.redirect || '/dashboard'
  await router.push(redirect)
}

async function handleAccountLogin() {
  if (!accountFormRef.value) return
  await accountFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await login({
        username: accountForm.username,
        password: accountForm.password,
        captchaCode: accountForm.captchaCode,
        captchaUuid: captchaUuid.value
      })
      await handleLoginSuccess(res)
    } catch {
      refreshCaptcha()
      accountForm.captchaCode = ''
    } finally {
      loading.value = false
    }
  })
}

async function handlePhoneLogin() {
  if (!phoneFormRef.value) return
  await phoneFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await loginByPhone({
        phone: phoneForm.phone,
        verifyCode: phoneForm.verifyCode
      })
      await handleLoginSuccess(res)
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => {
  fetchCaptcha()
})

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

.auth-tabs {
  margin-top: 16px;

  :deep(.el-tabs__item) {
    font-size: 14px;
    color: var(--text-secondary);
  }

  :deep(.el-tabs__item.is-active) {
    color: var(--color-primary);
  }

  :deep(.el-tabs__active-bar) {
    background-color: var(--color-primary);
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }
}

.auth-form {
  margin-top: 8px;
}

.code-row {
  display: flex;
  gap: 12px;
  width: 100%;

  .el-input {
    flex: 1;
  }
}

.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
  align-items: center;

  .el-input {
    flex: 1;
  }
}

.captcha-img-box {
  width: 120px;
  height: 40px;
  flex-shrink: 0;
  cursor: pointer;
  border-radius: var(--radius-base);
  overflow: hidden;
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(13, 17, 23, 0.6);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    border-color: var(--color-primary);
    box-shadow: 0 0 8px rgba(88, 166, 255, 0.25);
  }
}

.captcha-img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.captcha-placeholder {
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.submit-btn {
  width: 100%;
}

.auth-links {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  font-size: 12px;
}
</style>
