<template>
  <div class="notification-pref-page">
    <div class="page-header">
      <h2 class="text-xl font-semibold">{{ $t('settings.notificationPref') || '通知偏好设置' }}</h2>
      <p class="text-sm text-secondary">{{ $t('settings.notificationPrefDesc') || '自定义提醒时间和通知类型，避免打扰' }}</p>
    </div>

    <NCard class="section-card">
      <NSpin :show="loading">
        <NForm :model="form" label-placement="left" label-width="120px" @submit.prevent="handleSave">
          <NDivider>{{ $t('settings.notificationSwitch') || '通知开关' }}</NDivider>
          <NFormItem :label="$t('settings.enableNotification') || '启用通知'">
            <NSwitch
              v-model:value="form.notificationEnabled"
              :checked-value="1"
              :unchecked-value="0"
            />
          </NFormItem>

          <NDivider>{{ $t('settings.reminderContent') || '提醒内容' }}</NDivider>
          <NFormItem :label="$t('settings.exerciseReminder') || '运动提醒'">
            <NSwitch
              v-model:value="form.notifyExercise"
              :disabled="!form.notificationEnabled"
              :checked-value="1"
              :unchecked-value="0"
            />
            <span class="form-hint">{{ $t('settings.exerciseReminderHint') || '运动时间到时推送提醒' }}</span>
          </NFormItem>
          <NFormItem :label="$t('settings.dietReminder') || '饮食提醒'">
            <NSwitch
              v-model:value="form.notifyDiet"
              :disabled="!form.notificationEnabled"
              :checked-value="1"
              :unchecked-value="0"
            />
            <span class="form-hint">{{ $t('settings.dietReminderHint') || '用餐时间推送饮食记录提醒' }}</span>
          </NFormItem>
          <NFormItem :label="$t('settings.checkinReminder') || '打卡提醒'">
            <NSwitch
              v-model:value="form.notifyCheckin"
              :disabled="!form.notificationEnabled"
              :checked-value="1"
              :unchecked-value="0"
            />
            <span class="form-hint">{{ $t('settings.checkinReminderHint') || '每日打卡截止前推送提醒' }}</span>
          </NFormItem>

          <NDivider>{{ $t('settings.timeSettings') || '时间设置' }}</NDivider>
          <NFormItem :label="$t('settings.reminderTime') || '提醒时间'">
            <NTimePicker
              v-model:formatted-value="form.reminderTime"
              format="HH:mm"
              value-format="HH:mm"
              clearable
              :disabled="!form.notificationEnabled"
              class="w-[200px]"
            />
            <span class="form-hint">{{ $t('settings.reminderTimeHint') || '每日统一提醒时间' }}</span>
          </NFormItem>

          <NDivider>{{ $t('settings.quietHours') || '免打扰时段' }}</NDivider>
          <NFormItem :label="$t('settings.quietStart') || '开始时间'">
            <NTimePicker
              v-model:formatted-value="form.quietStart"
              format="HH:mm"
              value-format="HH:mm"
              clearable
              :disabled="!form.notificationEnabled"
              class="w-[200px]"
            />
          </NFormItem>
          <NFormItem :label="$t('settings.quietEnd') || '结束时间'">
            <NTimePicker
              v-model:formatted-value="form.quietEnd"
              format="HH:mm"
              value-format="HH:mm"
              clearable
              :disabled="!form.notificationEnabled"
              class="w-[200px]"
            />
            <span class="form-hint">{{ $t('settings.quietHoursHint') || '该时段内不推送任何通知' }}</span>
          </NFormItem>

          <NFormItem>
            <div class="flex gap-2.5">
              <NButton type="primary" attr-type="submit" :loading="saving">{{ $t('common.save') || '保存设置' }}</NButton>
              <NButton @click="loadPreference" :loading="loading">{{ $t('common.reset') || '重置' }}</NButton>
            </div>
          </NFormItem>
        </NForm>
      </NSpin>
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  NCard, NForm, NFormItem, NSwitch, NTimePicker, NDivider,
  NButton, NSpin,
  useMessage
} from 'naive-ui'
import { fetchGetNotificationPreference, fetchUpdateNotificationPreference } from '@/service/api'

defineOptions({ name: 'NotificationPreference' })
const message = useMessage()

interface NotificationPreferenceForm {
  notificationEnabled: number
  notifyExercise: number
  notifyDiet: number
  notifyCheckin: number
  reminderTime: string
  quietStart: string
  quietEnd: string
}

const loading = ref(false)
const saving = ref(false)
const form = reactive<NotificationPreferenceForm>({
  notificationEnabled: 1,
  notifyExercise: 1,
  notifyDiet: 1,
  notifyCheckin: 1,
  reminderTime: '08:00',
  quietStart: '22:00',
  quietEnd: '07:00'
})

async function loadPreference() {
  loading.value = true
  try {
    const { data } = await fetchGetNotificationPreference()
    if (data) {
      const d = data as any
      form.notificationEnabled = d.notificationEnabled ?? 1
      form.notifyExercise = d.notifyExercise ?? 1
      form.notifyDiet = d.notifyDiet ?? 1
      form.notifyCheckin = d.notifyCheckin ?? 1
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
    await fetchUpdateNotificationPreference({ ...form } as any)
    message.success('通知偏好已保存')
  } catch {
    message.error('保存失败')
  }
  finally { saving.value = false }
}

onMounted(loadPreference)
</script>

<style scoped>
.notification-pref-page { padding: 0; }

.page-header { margin-bottom: 20px; }

.section-card { padding: 4px; }

.form-hint {
  margin-left: 10px;
  color: #8b949e;
  font-size: 13px;
}

.text-secondary {
  color: #8b949e;
}
</style>
