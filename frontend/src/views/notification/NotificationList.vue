<template>
  <div class="notification-page">
    <div class="glass-card page-card">
      <div class="page-header">
        <h2 class="page-title">通知中心</h2>
        <el-button type="primary" text @click="handleMarkAllRead">全部已读</el-button>
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
                :icon="'Delete'"
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getNotificationList, markAsRead, markAllAsRead, deleteNotification } from '@/api/notification'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

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

onMounted(() => {
  fetchData()
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
