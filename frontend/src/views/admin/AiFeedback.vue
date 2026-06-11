<template>
  <div class="admin-ai-feedback-page">
    <div class="page-header">
      <h2>AI反馈审核</h2>
      <p class="page-desc">审核用户对AI建议的评价反馈</p>
    </div>

    <el-table :data="items" stripe v-loading="loading" empty-text="暂无待审核反馈">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column prop="feedbackType" label="反馈类型" width="110">
        <template #default="{ row }">
          <el-tag :type="row.feedbackType === 'positive' ? 'success' : (row.feedbackType === 'negative' ? 'danger' : 'info')" size="small">
            {{ feedbackTypeLabel(row.feedbackType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="反馈内容" min-width="220" show-overflow-tooltip />
      <el-table-column prop="aiModel" label="AI模型" width="130" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.reviewStatus === 'pending' ? 'warning' : (row.reviewStatus === 'approved' ? 'success' : 'info')" size="small">
            {{ reviewStatusLabel(row.reviewStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="160">
        <template #default="{ row }">{{ row.createTime }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <template v-if="row.reviewStatus === 'pending'">
            <el-button type="success" size="small" :loading="reviewingId === row.id" @click="handleReview(row, 'approved')">通过</el-button>
            <el-button type="info" size="small" :loading="reviewingId === row.id" @click="handleReview(row, 'rejected')">忽略</el-button>
          </template>
          <span v-else class="processed-text">已处理</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPendingAiFeedbacks, reviewAiFeedback } from '@/api/aiFeedback'

const items = ref([])
const loading = ref(false)
const reviewingId = ref(null)

function feedbackTypeLabel(t) {
  const map = { positive: '好评', negative: '差评', suggestion: '建议' }
  return map[t] || t
}

function reviewStatusLabel(s) {
  const map = { pending: '待审核', approved: '已通过', rejected: '已忽略' }
  return map[s] || s
}

async function loadItems() {
  loading.value = true
  try {
    const res = await getPendingAiFeedbacks()
    items.value = res.data || []
    if (Array.isArray(items.value)) {
      items.value = items.value.filter(i => i.reviewStatus === 'pending')
    }
  } finally { loading.value = false }
}

async function handleReview(row, result) {
  reviewingId.value = row.id
  try {
    await reviewAiFeedback(row.id, result)
    ElMessage.success(result === 'approved' ? '已通过' : '已忽略')
    loadItems()
  } catch { ElMessage.error('操作失败') }
  finally { reviewingId.value = null }
}

onMounted(loadItems)
</script>

<style scoped>
.admin-ai-feedback-page { padding: 20px 0; }
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0 0 4px; font-size: 20px; font-weight: 600; }
.page-desc { margin: 0; color: #8b949e; font-size: 14px; }
.processed-text { color: #8b949e; font-size: 13px; }
</style>