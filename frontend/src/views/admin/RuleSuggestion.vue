<template>
  <div class="admin-rule-page">
    <div class="page-header">
      <h2>安全规则建议审核</h2>
      <p class="page-desc">审核AI系统从采样分析中生成的规则优化建议</p>
    </div>

    <el-table :data="items" stripe v-loading="loading" empty-text="暂无待审核建议">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="ruleType" label="规则类型" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ row.ruleType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="suggestion" label="建议内容" min-width="280" show-overflow-tooltip />
      <el-table-column prop="reason" label="建议理由" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.reason || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'pending' ? 'warning' : (row.status === 'approved' ? 'success' : 'danger')" size="small">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="160">
        <template #default="{ row }">{{ row.createTime }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'pending'">
            <el-button type="success" size="small" :loading="approvingId === row.id" @click="handleApprove(row)">通过</el-button>
            <el-button type="danger" size="small" :loading="rejectingId === row.id" @click="handleReject(row)">拒绝</el-button>
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
import { useUserStore } from '@/stores/user'
import { getPendingRuleSuggestions, approveRuleSuggestion, rejectRuleSuggestion } from '@/api/admin'

const userStore = useUserStore()
const items = ref([])
const loading = ref(false)
const approvingId = ref(null)
const rejectingId = ref(null)

function statusLabel(s) {
  const map = { pending: '待审核', approved: '已通过', rejected: '已拒绝' }
  return map[s] || s
}

async function loadItems() {
  loading.value = true
  try {
    const res = await getPendingRuleSuggestions()
    items.value = res.data || []
  } finally { loading.value = false }
}

async function handleApprove(row) {
  approvingId.value = row.id
  try {
    const reviewerName = userStore.userInfo?.username || 'admin'
    const adminId = userStore.userInfo?.id
    await approveRuleSuggestion(row.id, reviewerName, adminId)
    ElMessage.success('规则建议已采纳')
    loadItems()
  } catch { ElMessage.error('操作失败') }
  finally { approvingId.value = null }
}

async function handleReject(row) {
  rejectingId.value = row.id
  try {
    const reviewerName = userStore.userInfo?.username || 'admin'
    const adminId = userStore.userInfo?.id
    await rejectRuleSuggestion(row.id, reviewerName, adminId)
    ElMessage.success('已拒绝')
    loadItems()
  } catch { ElMessage.error('操作失败') }
  finally { rejectingId.value = null }
}

onMounted(loadItems)
</script>

<style scoped>
.admin-rule-page { padding: 20px 0; }
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0 0 4px; font-size: 20px; font-weight: 600; }
.page-desc { margin: 0; color: #8b949e; font-size: 14px; }
.processed-text { color: #8b949e; font-size: 13px; }
</style>