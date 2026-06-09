<template>
  <div class="admin-feedback-page">
    <div class="page-header"><h2>计划反馈管理</h2></div>
    <el-table :data="items" stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="planId" label="计划ID" width="80" />
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column prop="content" label="反馈内容" min-width="200" show-overflow-tooltip />
      <el-table-column label="满意" width="80">
        <template #default="{ row }">
          <el-tag :type="row.satisfied ? 'success' : 'danger'" size="small">
            {{ row.satisfied ? '满意' : '不满意' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="160">
        <template #default="{ row }">{{ row.createTime }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="handleAdjust(row.id)" :loading="adjustingId === row.id">
            触发调整
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminPlanFeedbacks, triggerPlanAdjust } from '@/api/admin'

const items = ref([])
const loading = ref(false)
const adjustingId = ref(null)

async function loadItems() {
  loading.value = true
  try {
    const res = await getAdminPlanFeedbacks()
    items.value = res.data?.records || res.data || []
  } finally {
    loading.value = false
  }
}

async function handleAdjust(id) {
  adjustingId.value = id
  try {
    await triggerPlanAdjust(id)
    ElMessage.success('已触发 AI 计划重新调整')
    loadItems()
  } finally {
    adjustingId.value = null
  }
}

onMounted(loadItems)
</script>

<style scoped>
.admin-feedback-page { padding: 20px 0; }
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; font-weight: 600; }
</style>