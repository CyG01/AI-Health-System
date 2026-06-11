<template>
  <div class="profile-page" v-loading="pageLoading">
    <div class="profile-card glass-card">
      <h2 class="page-title">个人中心</h2>
      <p class="page-desc">管理您的个人信息与账号安全</p>

      <div class="profile-header">
        <el-avatar :size="72" :src="profileForm.avatar">
          {{ avatarText }}
        </el-avatar>
        <div class="profile-meta">
          <span class="profile-name">{{ profileForm.nickname || profileForm.username || '用户' }}</span>
          <span class="profile-role">{{ roleText }}</span>
        </div>
      </div>

      <el-divider />

      <h3 class="section-title">基本信息</h3>
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="80px" class="profile-form">
        <el-form-item label="用户名">
          <el-input v-model="profileForm.username" disabled />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="profileForm.phone" disabled />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="20" />
        </el-form-item>
        <el-form-item label="头像">
          <div class="avatar-upload">
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
              :http-request="handleAvatarUpload"
              accept="image/*"
            >
              <el-avatar :size="72" :src="profileForm.avatar" class="clickable-avatar">
                {{ avatarText }}
              </el-avatar>
              <div class="upload-hint">点击更换头像</div>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="头像URL" prop="avatar">
          <el-input v-model="profileForm.avatar" placeholder="或直接输入头像URL" maxlength="500" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="profileForm.gender">
            <el-radio :value="0">未知</el-radio>
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="profileForm.age" :min="1" :max="150" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="profileLoading" :disabled="profileLoading" @click="handleSaveProfile">
            保存信息
          </el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <h3 class="section-title">修改密码</h3>
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px" class="profile-form">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" show-password maxlength="20" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password maxlength="20" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password maxlength="20" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="passwordLoading" :disabled="passwordLoading" @click="handleUpdatePassword">
            修改密码
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { updateProfile, updatePassword, uploadAvatar } from '@/api/user'
import {
  passwordRules as basePasswordRules,
  createConfirmPasswordRule,
  createNewPasswordRule
} from '@/utils/validate'

const userStore = useUserStore()
const profileFormRef = ref(null)
const passwordFormRef = ref(null)
const pageLoading = ref(false)
const profileLoading = ref(false)
const passwordLoading = ref(false)

const profileForm = reactive({
  username: '',
  phone: '',
  nickname: '',
  avatar: '',
  gender: 0,
  age: null,
  role: 'user'
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileRules = {
  nickname: [{ max: 20, message: '昵称长度不能超过20个字符', trigger: 'blur' }],
  avatar: [{ max: 500, message: '头像地址长度不能超过500个字符', trigger: 'blur' }],
  age: [{ type: 'number', min: 1, max: 150, message: '年龄必须在1-150之间', trigger: 'blur' }]
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    ...basePasswordRules,
    createNewPasswordRule(() => passwordForm.oldPassword)
  ],
  confirmPassword: [createConfirmPasswordRule(() => passwordForm.newPassword)]
}

const avatarText = computed(() => {
  const name = profileForm.nickname || profileForm.username || 'U'
  return name.charAt(0).toUpperCase()
})

const roleText = computed(() => {
  return profileForm.role === 'admin' ? '管理员' : '普通用户'
})

function fillProfileForm(data) {
  profileForm.username = data.username || ''
  profileForm.phone = data.phone || ''
  profileForm.nickname = data.nickname || ''
  profileForm.avatar = data.avatar || ''
  profileForm.gender = data.gender ?? 0
  profileForm.age = data.age ?? null
  profileForm.role = data.role || 'user'
}

async function loadProfile() {
  pageLoading.value = true
  try {
    const data = await userStore.fetchProfile()
    fillProfileForm(data)
  } finally {
    pageLoading.value = false
  }
}

async function handleSaveProfile() {
  if (!profileFormRef.value) return
  await profileFormRef.value.validate(async (valid) => {
    if (!valid) return
    profileLoading.value = true
    try {
      const res = await updateProfile({
        nickname: profileForm.nickname,
        avatar: profileForm.avatar,
        gender: profileForm.gender,
        age: profileForm.age
      })
      fillProfileForm(res.data)
      userStore.updateUserInfo(res.data)
      ElMessage.success('个人信息已更新')
    } finally {
      profileLoading.value = false
    }
  })
}

async function handleUpdatePassword() {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return
    passwordLoading.value = true
    try {
      await updatePassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword,
        confirmPassword: passwordForm.confirmPassword
      })
      ElMessage.success('密码修改成功')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
      passwordFormRef.value.resetFields()
    } finally {
      passwordLoading.value = false
    }
  })
}

function beforeAvatarUpload(file) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB')
    return false
  }
  return true
}

async function handleAvatarUpload({ file }) {
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await uploadAvatar(formData)
    profileForm.avatar = res.data
    userStore.updateUserInfo({ ...userStore.userInfo, avatar: res.data })
    ElMessage.success('头像上传成功')
  } catch {
    ElMessage.error('头像上传失败')
  }
}

onMounted(() => {
  if (userStore.userInfo) {
    fillProfileForm(userStore.userInfo)
  }
  loadProfile()
})
</script>

<style scoped lang="scss">
.profile-page {
  padding: 4px;
}

.profile-card {
  padding: 32px;
  max-width: 720px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-top: 24px;
}

.profile-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.profile-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.profile-role {
  font-size: 12px;
  color: var(--text-secondary);
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.profile-form {
  margin-top: 8px;
}

:deep(.el-divider) {
  border-color: transparent;
  margin: 28px 0;
}

.avatar-upload {
  display: flex;
  align-items: center;
}

.avatar-uploader {
  cursor: pointer;
}

.clickable-avatar {
  cursor: pointer;
  transition: opacity 0.2s;
}

.clickable-avatar:hover {
  opacity: 0.8;
}

.upload-hint {
  display: inline-block;
  margin-left: 12px;
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
