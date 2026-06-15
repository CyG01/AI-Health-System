<template>
  <div class="p-1">
    <n-spin :show="pageLoading">
      <n-card class="max-w-[720px]">
        <h2 class="text-xl font-semibold mb-1">个人中心</h2>
        <p class="text-gray-400 text-sm mb-6">管理您的个人信息与账号安全</p>

        <!-- Profile Header -->
        <div class="flex items-center gap-5 mt-6">
          <n-avatar :size="72" :src="profileForm.avatar || undefined" round>
            {{ avatarText }}
          </n-avatar>
          <div class="flex flex-col gap-1">
            <span class="text-base font-semibold">{{ profileForm.nickname || profileForm.username || '用户' }}</span>
            <span class="text-xs text-gray-400">{{ roleText }}</span>
          </div>
        </div>

        <n-divider />

        <!-- Basic Info Section -->
        <h3 class="text-sm font-semibold mb-4">基本信息</h3>
        <n-form
          ref="profileFormRef"
          :model="profileForm"
          :rules="profileRules"
          label-placement="left"
          label-width="80"
          class="profile-form"
        >
          <n-form-item label="用户名">
            <n-input v-model:value="profileForm.username" disabled />
          </n-form-item>
          <n-form-item label="手机号">
            <n-input v-model:value="profileForm.phone" disabled />
          </n-form-item>
          <n-form-item label="昵称" path="nickname">
            <n-input v-model:value="profileForm.nickname" placeholder="请输入昵称" :maxlength="20" />
          </n-form-item>
          <n-form-item label="头像">
            <div class="flex items-center gap-3">
              <n-upload
                :show-file-list="false"
                :before-upload="beforeAvatarUpload"
                :custom-request="handleAvatarUpload"
                accept="image/*"
              >
                <n-avatar :size="72" :src="profileForm.avatar || undefined" round class="cursor-pointer hover:opacity-80 transition-opacity">
                  {{ avatarText }}
                </n-avatar>
              </n-upload>
              <span class="text-xs text-gray-400">点击更换头像</span>
            </div>
          </n-form-item>
          <n-form-item label="头像URL" path="avatar">
            <n-input v-model:value="profileForm.avatar" placeholder="或直接输入头像URL" :maxlength="500" />
          </n-form-item>
          <n-form-item label="性别" path="gender">
            <n-radio-group v-model:value="profileForm.gender">
              <n-radio :value="0">未知</n-radio>
              <n-radio :value="1">男</n-radio>
              <n-radio :value="2">女</n-radio>
            </n-radio-group>
          </n-form-item>
          <n-form-item label="年龄" path="age">
            <n-input-number v-model:value="profileForm.age" :min="1" :max="150" class="w-[200px]" />
          </n-form-item>
          <n-form-item>
            <n-button type="primary" :loading="profileLoading" :disabled="profileLoading" @click="handleSaveProfile">
              保存信息
            </n-button>
          </n-form-item>
        </n-form>

        <n-divider />

        <!-- Password Section -->
        <h3 class="text-sm font-semibold mb-4">修改密码</h3>
        <n-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-placement="left"
          label-width="100"
          class="profile-form"
        >
          <n-form-item label="原密码" path="oldPassword">
            <n-input
              v-model:value="passwordForm.oldPassword"
              type="password"
              show-password-on="click"
              placeholder="请输入原密码"
              :maxlength="20"
            />
          </n-form-item>
          <n-form-item label="新密码" path="newPassword">
            <n-input
              v-model:value="passwordForm.newPassword"
              type="password"
              show-password-on="click"
              placeholder="请输入新密码"
              :maxlength="20"
            />
          </n-form-item>
          <n-form-item label="确认密码" path="confirmPassword">
            <n-input
              v-model:value="passwordForm.confirmPassword"
              type="password"
              show-password-on="click"
              placeholder="请再次输入新密码"
              :maxlength="20"
            />
          </n-form-item>
          <n-form-item>
            <n-button type="primary" :loading="passwordLoading" :disabled="passwordLoading" @click="handleUpdatePassword">
              修改密码
            </n-button>
          </n-form-item>
        </n-form>

        <!-- Danger Zone -->
        <n-card class="mt-6" :bordered="true" style="border-color: #d03050">
          <template #header>
            <span style="color: #d03050" class="font-semibold">危险操作</span>
          </template>
          <p class="text-gray-400 text-[13px] mb-4">注销账号后所有数据将被永久删除，此操作不可恢复。</p>
          <n-button type="error" secondary @click="handleDeactivate">注销账号</n-button>
        </n-card>
      </n-card>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  NAvatar, NButton, NCard, NDivider, NForm, NFormItem,
  NInput, NInputNumber, NRadio, NRadioGroup, NSpin, NUpload,
  useMessage, useDialog
} from 'naive-ui'
import type { FormInst, FormRules, UploadFileInfo, UploadCustomRequestOptions } from 'naive-ui'
import { useAuthStore } from '@/store/modules/auth'
import {
  fetchUpdateProfile,
  fetchUpdatePassword,
  fetchUploadAvatar,
  fetchDeactivateAccount
} from '@/service/api/user'
import { fetchGetUserInfo } from '@/service/api'
import {
  passwordRules as basePasswordRules,
  createConfirmPasswordRule,
  createNewPasswordRule
} from '@/utils/validate'

defineOptions({ name: 'UserProfile' })

// --- Interfaces ---

interface ProfileFormData {
  username: string
  phone: string
  nickname: string
  avatar: string
  gender: number
  age: number | null
  role: string
}

interface PasswordFormData {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

// --- State ---

const authStore = useAuthStore()
const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const profileFormRef = ref<FormInst | null>(null)
const passwordFormRef = ref<FormInst | null>(null)
const pageLoading = ref(false)
const profileLoading = ref(false)
const passwordLoading = ref(false)

const profileForm = reactive<ProfileFormData>({
  username: '',
  phone: '',
  nickname: '',
  avatar: '',
  gender: 0,
  age: null,
  role: 'user'
})

const passwordForm = reactive<PasswordFormData>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// --- Validation rules ---

const profileRules: FormRules = {
  nickname: [{ max: 20, message: '昵称长度不能超过20个字符', trigger: 'blur' }],
  avatar: [{ max: 500, message: '头像地址长度不能超过500个字符', trigger: 'blur' }],
  age: [{ type: 'number', min: 1, max: 150, message: '年龄必须在1-150之间', trigger: ['blur', 'change'] }]
}

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    ...basePasswordRules,
    createNewPasswordRule(() => passwordForm.oldPassword)
  ],
  confirmPassword: [createConfirmPasswordRule(() => passwordForm.newPassword)]
}

// --- Computed ---

const avatarText = computed(() => {
  const name = profileForm.nickname || profileForm.username || 'U'
  return name.charAt(0).toUpperCase()
})

const roleText = computed(() => {
  return profileForm.role === 'admin' ? '管理员' : '普通用户'
})

// --- Helpers ---

function fillProfileForm(data: Partial<ProfileFormData>): void {
  profileForm.username = data.username || ''
  profileForm.phone = data.phone || ''
  profileForm.nickname = data.nickname || ''
  profileForm.avatar = data.avatar || ''
  profileForm.gender = data.gender ?? 0
  profileForm.age = data.age ?? null
  profileForm.role = data.role || 'user'
}

// --- Actions ---

async function loadProfile(): Promise<void> {
  pageLoading.value = true
  try {
    const { data, error } = await fetchGetUserInfo()
    if (!error && data) {
      fillProfileForm(data as any)
      Object.assign(authStore.userInfo, data)
    }
  } finally {
    pageLoading.value = false
  }
}

async function handleSaveProfile(): Promise<void> {
  if (!profileFormRef.value) return
  try {
    await profileFormRef.value.validate()
  } catch {
    return
  }

  profileLoading.value = true
  try {
    const res = await fetchUpdateProfile({
      nickname: profileForm.nickname,
      gender: profileForm.gender as any
    })
    if (res.data) {
      fillProfileForm(res.data as any)
      Object.assign(authStore.userInfo, res.data)
    }
    message.success('个人信息已更新')
  } finally {
    profileLoading.value = false
  }
}

async function handleUpdatePassword(): Promise<void> {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
  } catch {
    return
  }

  passwordLoading.value = true
  try {
    await fetchUpdatePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword
    })
    authStore.logout()
    router.push('/login')
    message.success('密码修改成功，请重新登录')
  } finally {
    passwordLoading.value = false
  }
}

async function handleDeactivate(): Promise<void> {
  const confirmed = await new Promise<boolean>((resolve) => {
    dialog.warning({
      title: '注销账号',
      content: '确定要注销账号吗？所有数据将被永久删除，此操作不可恢复！',
      positiveText: '确认注销',
      negativeText: '取消',
      onPositiveClick: () => resolve(true),
      onNegativeClick: () => resolve(false)
    })
  })
  if (!confirmed) return

  try {
    await fetchDeactivateAccount()
    authStore.logout()
    router.push('/login')
    message.success('账号已注销')
  } catch {
    // cancelled or handled by interceptor
  }
}

function beforeAvatarUpload({ file }: { file: UploadFileInfo }): boolean {
  const rawFile = file.file
  if (!rawFile) return false
  if (!rawFile.type.startsWith('image/')) {
    message.error('只能上传图片文件')
    return false
  }
  if (rawFile.size / 1024 / 1024 > 2) {
    message.error('头像大小不能超过 2MB')
    return false
  }
  return true
}

async function handleAvatarUpload({ file }: UploadCustomRequestOptions): Promise<void> {
  const rawFile = file.file
  if (!rawFile) return
  const formData = new FormData()
  formData.append('file', rawFile)
  try {
    const res = await fetchUploadAvatar(formData)
    const avatarUrl = res.data || ''
    profileForm.avatar = avatarUrl
    Object.assign(authStore.userInfo, { avatar: avatarUrl })
    message.success('头像上传成功')
  } catch {
    message.error('头像上传失败')
  }
}

// --- Lifecycle ---

onMounted(() => {
  if (authStore.userInfo) {
    fillProfileForm(authStore.userInfo as any)
  }
  loadProfile()
})
</script>

<style scoped>
.profile-form {
  margin-top: 8px;
}
</style>
