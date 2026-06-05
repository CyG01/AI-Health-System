<template>
  <div class="auth-page">
    <div class="auth-card glass-card">
      <h2 class="page-title">忘记密码</h2>
      <p class="page-desc">通过手机号验证码重置密码</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="auth-form">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="验证码" prop="verifyCode">
          <div class="code-row">
            <el-input v-model="form.verifyCode" placeholder="请输入验证码" maxlength="6" />
            <el-button :disabled="codeSending" @click="handleSendCode">{{ codeButtonText }}</el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" placeholder="请输入新密码" show-password maxlength="20" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入新密码" show-password maxlength="20" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" :disabled="loading" class="submit-btn" @click="handleReset">
            重置密码
          </el-button>
        </el-form-item>
      </el-form>
      <div class="auth-links">
        <router-link to="/login">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { resetPassword, sendCode } from '@/api/auth'
import {
  phoneRules,
  verifyCodeRules,
  createConfirmPasswordRule
} from '@/utils/validate'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const codeSending = ref(false)
const countdown = ref(0)
let countdownTimer = null

const form = reactive({
  phone: '',
  verifyCode: '',
  newPassword: '',
  confirmPassword: ''
})

const rules = {
  phone: phoneRules,
  verifyCode: verifyCodeRules,
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须在6-20个字符之间', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{6,20}$/, message: '密码必须包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [createConfirmPasswordRule(() => form.newPassword)]
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

async function handleReset() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await resetPassword({
        phone: form.phone,
        verifyCode: form.verifyCode,
        newPassword: form.newPassword,
        confirmPassword: form.confirmPassword
      })
      ElMessage.success('密码重置成功')
      await router.push('/login')
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
