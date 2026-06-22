<template>
  <div class="notification-page">
    <div class="glass-card page-card">
      <div class="page-header">
        <h2 class="page-title">{{ $t('notification.center') || '通知中心' }}</h2>
        <div class="header-actions">
          <NButton text type="default" @click="showPreferenceDialog = true">
            <template #icon><NIcon><SettingsOutline /></NIcon></template>
            {{ $t('notification.preference') || '通知偏好' }}
          </NButton>
          <NButton text type="primary" @click="handleMarkAllRead">{{ $t('notification.markAllRead') || '全部已读' }}</NButton>
        </div>
      </div>

      <div class="notification-list" :class="{ 'is-loading': loading }">
        <template v-if="list.length > 0">
          <div
            v-for="item in list"
            :key="item.id"
            :class="['notification-card', 'glass-card', { unread: item.isRead === 0 }]"
            @click="handleClickNotification(item)"
          >
            <div class="card-left">
              <div v-if="item.isRead === 0" class="unread-dot-wrap">
                <span class="unread-dot-circle"></span>
              </div>
              <div v-else class="unread-dot-wrap"></div>
              <div class="card-content">
                <h4 class="card-title">{{ item.title }}</h4>
                <p class="card-text">{{ item.content }}</p>
                <span class="card-meta">{{ item.createTime }}</span>
              </div>
            </div>
            <div class="card-right">
              <NTag
                :type="item.type === 'system' ? 'default' : item.type === 'remind' ? 'warning' : 'error'"
                size="small"
                :bordered="false"
              >
                {{ item.type === 'system' ? '系统' : item.type === 'remind' ? '提醒' : '告警' }}
              </NTag>
              <NButton
                type="error"
                size="small"
                text
                @click.stop="handleDelete(item)"
              >
                <template #icon><NIcon><TrashOutline /></NIcon></template>
              </NButton>
            </div>
          </div>
        </template>
        <NEmpty
          v-else-if="!loading"
          :description="$t('notification.empty') || '暂无通知'"
        />
      </div>

      <div class="pagination-wrap" v-if="total > 0">
        <NPagination
          v-model:page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          :item-count="total"
          show-size-picker
          @update:page="fetchData"
          @update:page-size="fetchData"
        />
      </div>
    </div>

    <!-- 通知偏好设置对话框 -->
    <NModal
      v-model:show="showPreferenceDialog"
      preset="card"
      :title="$t('notification.prefSettings') || '通知偏好设置'"
      style="width: 460px; max-width: 90vw"
      @after-leave="loadPreferences"
    >
      <NForm :model="prefForm" label-placement="left" label-width="110px">
        <NFormItem label="总开关">
          <NSwitch
            v-model:value="prefForm.notificationEnabled"
            :checked-value="1"
            :unchecked-value="0"
          />
        </NFormItem>
        <NFormItem label="每日提醒时间">
          <NTimePicker
            v-model:formatted-value="prefForm.reminderTime"
            format="HH:mm"
            value-format="HH:mm"
            clearable
            style="width:100%"
          />
        </NFormItem>
        <NDivider />
        <NFormItem label="运动提醒">
          <NSwitch
            v-model:value="prefForm.notifyExercise"
            :checked-value="1"
            :unchecked-value="0"
          />
        </NFormItem>
        <NFormItem label="饮食提醒">
          <NSwitch
            v-model:value="prefForm.notifyDiet"
            :checked-value="1"
            :unchecked-value="0"
          />
        </NFormItem>
        <NFormItem label="打卡提醒">
          <NSwitch
            v-model:value="prefForm.notifyCheckin"
            :checked-value="1"
            :unchecked-value="0"
          />
        </NFormItem>
        <NDivider />
        <NFormItem label="安静时段">
          <div style="display:flex;align-items:center;gap:8px">
            <NTimePicker
              v-model:formatted-value="prefForm.quietStart"
              format="HH:mm"
              value-format="HH:mm"
              clearable
              style="width:120px"
            />
            <span style="color:var(--text-secondary)">至</span>
            <NTimePicker
              v-model:formatted-value="prefForm.quietEnd"
              format="HH:mm"
              value-format="HH:mm"
              clearable
              style="width:120px"
            />
          </div>
        </NFormItem>
      </NForm>
      <template #footer>
        <div style="display:flex;justify-content:flex-end;gap:10px">
          <NButton @click="showPreferenceDialog = false">取消</NButton>
          <NButton type="primary" :loading="savingPrefs" @click="savePreferences">保存</NButton>
        </div>
      </template>
    </NModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  NButton, NIcon, NTag, NEmpty, NModal, NForm, NFormItem,
  NSwitch, NTimePicker, NDivider, NPagination,
  useMessage, useDialog
} from 'naive-ui'
import { SettingsOutline, TrashOutline } from '@vicons/ionicons5'
import { fetchGetNotificationList, fetchMarkAsRead, fetchMarkAllAsRead, fetchDeleteNotification, fetchGetNotificationPreference, fetchUpdateNotificationPreference } from '@/service/api'
import { useAuthStore } from '@/store/modules/auth'

interface NotificationItem {
  id: number | string
  title: string
  content: string
  createTime: string
  isRead: number
  type: 'system' | 'remind' | 'alert'
}

interface NotificationPreference {
  notificationEnabled: number
  reminderTime: string
  notifyExercise: number
  notifyDiet: number
  notifyCheckin: number
  quietStart: string
  quietEnd: string
}

defineOptions({ name: 'NotificationList' })

const message = useMessage()
const dialog = useDialog()

const authStore = useAuthStore()
const loading = ref(false)
const list = ref<NotificationItem[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

// 通知偏好设置
const showPreferenceDialog = ref(false)
const savingPrefs = ref(false)
const prefForm = reactive<NotificationPreference>({
  notificationEnabled: 1,
  reminderTime: '',
  notifyExercise: 1,
  notifyDiet: 1,
  notifyCheckin: 1,
  quietStart: '',
  quietEnd: ''
})

async function fetchData() {
  loading.value = true
  try {
    const { data } = await fetchGetNotificationList({
      page: page.value,
      size: size.value
    })
    if (data) {
      list.value = (data as any).records || []
      total.value = (data as any).total || 0
    }
  } finally {
    loading.value = false
  }
}

async function handleClickNotification(item: NotificationItem) {
  if (item.isRead === 0) {
    try {
      await fetchMarkAsRead(item.id as number)
      item.isRead = 1
      // TODO: authStore doesn't have fetchUnreadCount - implement notification badge separately
    } catch {
      // handled by interceptor
    }
  }
}

async function handleMarkAllRead() {
  try {
    await fetchMarkAllAsRead()
    list.value.forEach(item => (item.isRead = 1))
    // TODO: clearUnreadCount - implement notification badge separately
    message.success('已全部标为已读')
  } catch {
    // handled by interceptor
  }
}

function handleDelete(item: NotificationItem) {
  dialog.warning({
    title: '确认删除',
    content: '确定要删除该通知吗？',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await fetchDeleteNotification(item.id as number)
        message.success('已删除')
        fetchData()
        // TODO: authStore doesn't have fetchUnreadCount - implement notification badge separately
      } catch {
        // handled by interceptor
      }
    }
  })
}

// 加载通知偏好
async function loadPreferences() {
  try {
    const { data } = await fetchGetNotificationPreference()
    if (data) {
      Object.assign(prefForm, data)
      if (prefForm.notificationEnabled == null) prefForm.notificationEnabled = 1
      if (prefForm.notifyExercise == null) prefForm.notifyExercise = 1
      if (prefForm.notifyDiet == null) prefForm.notifyDiet = 1
      if (prefForm.notifyCheckin == null) prefForm.notifyCheckin = 1
    }
  } catch { /* ignore */ }
}

// 保存通知偏好
async function savePreferences() {
  savingPrefs.value = true
  try {
    await fetchUpdateNotificationPreference(prefForm as any)
    message.success('通知偏好已保存')
    showPreferenceDialog.value = false
  } catch { /* handled by interceptor */ }
  finally { savingPrefs.value = false }
}

onMounted(() => {
  fetchData()
  loadPreferences()
})
</script>

<style scoped lang="scss">
.notification-page {
  height: 100%;
}

.page-card {
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.notification-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;

  &.is-loading { opacity: 0.7; pointer-events: none; }
}

.notification-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border: 1px solid #30363d;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;

  &.unread {
    border-color: rgba(88, 166, 255, 0.35);
    background: rgba(88, 166, 255, 0.04);
  }

  &:hover {
    border-color: rgba(88, 166, 255, 0.5);
  }
}

.card-left {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.unread-dot-wrap {
  margin-top: 8px;
  min-width: 8px;
}

.unread-dot-circle {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #58a6ff;
}

.card-content {
  flex: 1;
  min-width: 0;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #c9d1d9;
  margin: 0 0 6px;
}

.card-text {
  font-size: 13px;
  color: #8b949e;
  margin: 0 0 8px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  font-size: 12px;
  color: #484f58;
}

.card-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  margin-left: 16px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
