<template>
  <div class="notification-pref-page">
    <div class="page-header">
      <h2>通知偏好设置</h2>
      <p class="page-desc">自定义提醒时间和通知类型，避免打扰</p>
    </div>

    <div class="section-card glass-card">
      <el-form :model="form" label-width="120px" v-loading="loading" @submit.prevent="handleSave">
        <el-divider content-position="left">通知开关</el-divider>
        <el-form-item label="启用通知">
          <el-switch v-model="form.notificationEnabled" active-text="开启" inactive-text="关闭" />
        </el-form-item>

        <el-divider content-position="left">提醒内容</el-divider>
        <el-form-item label="运动提醒">
          <el-switch v-model="form.notifyExercise" :disabled="!form.notificationEnabled" />
          <span class="form-hint">运动时间到时推送提醒</span>
        </el-form-item>
        <el-form-item label="饮食提醒">
          <el-switch v-model="form.notifyDiet" :disabled="!form.notificationEnabled" />
          <span class="form-hint">用餐时间推送饮食记录提醒</span>
        </el-form-item>
        <el-form-item label="打卡提醒">
          <el-switch v-model="form.notifyCheckin" :disabled="!form.notificationEnabled" />
          <span class="form-hint">每日打卡截止前推送提醒</span>
        </el-form-item>

        <el-divider content-position="left">时间设置</el-divider>
        <el-form-item label="提醒时间">
          <el-time-select
            v-model="form.reminderTime"
            :disabled="!form.notificationEnabled"
            placeholder="选择时间"
            start="06:00"
            step="00:30"
            end="23:00"
          />
          <span class="form-hint">每日统一提醒时间</span>
        </el-form-item>

        <el-divider content-position="left">免打扰时段</el-divider>
        <el-form-item label="开始时间">
          <el-time-select
            v-model="form.quietStart"
            :disabled="!form.notificationEnabled"
            placeholder="开始"
            start="18:00"
            step="00:30"
            end="23:30"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-select
            v-model="form.quietEnd"
            :disabled="!form.notificationEnabled"
            placeholder="结束"
            start="06:00"
            step="00:30"
            end="12:00"
          />
          <span class="form-hint">该时段内不推送任何通知</span>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="saving">保存设置</el-button>
          <el-button @click="loadPreference" :loading="loading">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getNotificationPreference, updateNotificationPreference } from '@/api/notificationPreference'

const loading = ref(false)
const saving = ref(false)
const form = reactive({
  notificationEnabled: true,
  notifyExercise: true,
  notifyDiet: true,
  notifyCheckin: true,
  reminderTime: '08:00',
  quietStart: '22:00',
  quietEnd: '07:00'
})

async function loadPreference() {
  loading.value = true
  try {
    const res = await getNotificationPreference()
    if (res.data) {
      const d = res.data
      form.notificationEnabled = d.notificationEnabled ?? true
      form.notifyExercise = d.notifyExercise ?? true
      form.notifyDiet = d.notifyDiet ?? true
      form.notifyCheckin = d.notifyCheckin ?? true
      form.reminderTime = d.reminderTime || '08:00'
      form.quietStart = d.quietStart || '22:00'
      form.quietEnd = d.quietEnd || '07:00'
    }
  } catch { /* use defaults */ }
  finally { loading.value = false }
}

async function handleSave() {
  saving.value = true
  try {
    await updateNotificationPreference({ ...form })
    ElMessage.success('通知偏好已保存')
  } catch { ElMessage.error('保存失败') }
  finally { saving.value = false }
}

onMounted(loadPreference)
</script>

<style scoped>
.notification-pref-page { padding: 0; }
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0 0 4px; font-size: 20px; font-weight: 600; }
.page-desc { margin: 0; color: #8b949e; font-size: 14px; }
.section-card { padding: 20px 30px; }
.form-hint { margin-left: 10px; color: #8b949e; font-size: 13px; }
</style>