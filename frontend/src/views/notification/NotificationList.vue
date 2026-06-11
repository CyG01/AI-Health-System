<template>
  <div class="notification-page">
    <div class="glass-card page-card">
      <div class="page-header">
        <h2 class="page-title">通知中心</h2>
        <div class="header-actions">
          <el-button type="default" text @click="showPreferenceDialog = true">
            <el-icon><Setting /></el-icon> 通知偏好
          </el-button>
          <el-button type="primary" text @click="handleMarkAllRead">全部已读</el-button>
        </div>
      </div>

      <div class="notification-list" v-loading="loading">
        <template v-if="list.length > 0">
          <div
            v-for="item in list"
            :key="item.id"
            :class="['notification-card', 'glass-card', { unread: item.isRead === 0 }]"
            @click="handleClickNotification(item)"
          >
            <div class="card-left">
              <el-badge v-if="item.isRead === 0" :is-dot="true" class="unread-dot" />
              <el-badge v-else :value="0" :hidden="true" />
              <div class="card-content">
                <h4 class="card-title">{{ item.title }}</h4>
                <p class="card-text">{{ item.content }}</p>
                <span class="card-meta">{{ item.createTime }}</span>
              </div>
            </div>
            <div class="card-right">
              <el-tag
                :type="item.type === 'system' ? '' : item.type === 'remind' ? 'warning' : 'danger'"
                size="small"
                effect="dark"
              >
                {{ item.type === 'system' ? '系统' : item.type === 'remind' ? '提醒' : '告警' }}
              </el-tag>
              <el-button
                type="danger"
                size="small"
                text
                :icon="Delete"
                @click.stop="handleDelete(item)"
              />
            </div>
          </div>
        </template>
        <el-empty
          v-else-if="!loading"
          description="暂无通知"
          :image-size="80"
        />
      </div>

      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </div>

    <!-- 通知偏好设置对话框 -->
    <el-dialog v-model="showPreferenceDialog" title="通知偏好设置" width="460px" @close="loadPreferences">
      <el-form :model="prefForm" label-width="110px" label-position="left">
        <el-form-item label="总开关">
          <el-switch v-model="prefForm.notificationEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="每日提醒时间">
          <el-time-picker v-model="prefForm.reminderTime" format="HH:mm" value-format="HH:mm" placeholder="选择时间" style="width:100%" />
        </el-form-item>
        <el-divider />
        <el-form-item label="运动提醒">
          <el-switch v-model="prefForm.notifyExercise" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="饮食提醒">
          <el-switch v-model="prefForm.notifyDiet" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="打卡提醒">
          <el-switch v-model="prefForm.notifyCheckin" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-divider />
        <el-form-item label="安静时段">
          <div style="display:flex;align-items:center;gap:8px">
            <el-time-picker v-model="prefForm.quietStart" format="HH:mm" value-format="HH:mm" placeholder="开始" style="width:120px" />
            <span style="color:#8b949e">至</span>
            <el-time-picker v-model="prefForm.quietEnd" format="HH:mm" value-format="HH:mm" placeholder="结束" style="width:120px" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPreferenceDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingPrefs" @click="savePreferences">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Setting } from '@element-plus/icons-vue'
import { getNotificationList, markAsRead, markAllAsRead, deleteNotification } from '@/api/notification'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

// 通知偏好设置
const showPreferenceDialog = ref(false)
const savingPrefs = ref(false)
const prefForm = reactive({
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
    const res = await getNotificationList({
      page: page.value,
      size: size.value
    })
    if (res.data) {
      list.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

async function handleClickNotification(item) {
  if (item.isRead === 0) {
    try {
      await markAsRead(item.id)
      item.isRead = 1
      userStore.fetchUnreadCount()
    } catch {
      // handled by interceptor
    }
  }
}

async function handleMarkAllRead() {
  try {
    await markAllAsRead()
    list.value.forEach(item => (item.isRead = 1))
    userStore.clearUnreadCount()
    ElMessage.success('已全部标为已读')
  } catch {
    // handled by interceptor
  }
}

function handleDelete(item) {
  ElMessageBox.confirm('确定要删除该通知吗？', '确认删除', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteNotification(item.id)
      ElMessage.success('已删除')
      fetchData()
      userStore.fetchUnreadCount()
    } catch {
      // handled by interceptor
    }
  }).catch(() => {})
}

// 加载通知偏好
async function loadPreferences() {
  try {
    const res = await request({ url: '/notification-preference', method: 'get' })
    if (res.data) {
      Object.assign(prefForm, res.data)
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
    await request({ url: '/notification-preference', method: 'put', data: prefForm })
    ElMessage.success('通知偏好已保存')
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

.unread-dot {
  margin-top: 8px;
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
