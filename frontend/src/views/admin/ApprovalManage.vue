<template>
  <div class="admin-approval-page">
    <div class="page-header">
      <h2>操作审批管理</h2>
      <p class="page-desc">审核用户提交的敏感操作申请</p>
    </div>

    <el-table :data="items" stripe v-loading="loading" empty-text="暂无待审批申请">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column prop="actionType" label="操作类型" width="140">
        <template #default="{ row }">
          <el-tag :type="actionTagType(row.actionType)" size="small">{{ row.actionType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="detail" label="申请详情" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'pending' ? 'warning' : (row.status === 'approved' ? 'success' : 'danger')" size="small">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="160">
        <template #default="{ row }">{{ row.createTime }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getPendingApprovals, approveRequest, rejectRequest } from '@/api/admin'

const userStore = useUserStore()
const items = ref([])
const loading = ref(false)
const approvingId = ref(null)
const rejectingId = ref(null)

function actionTagType(action) {
  const map = { 'DELETE_ACCOUNT': 'danger', 'DATA_EXPORT': 'warning', 'REFUND': 'warning', 'SUBSCRIPTION_UPGRADE': 'primary' }
  return map[action] || 'info'
}

function statusLabel(s) {
  const map = { pending: '待审批', approved: '已通过', rejected: '已拒绝' }
  return map[s] || s
}

async function loadItems() {
  loading.value = true
  try {
    const res = await getPendingApprovals()
    items.value = res.data || []
  } finally { loading.value = false }
}

async function handleApprove(row) {
  approvingId.value = row.id
  try {
    const approverName = userStore.userInfo?.username || 'admin'
    const approverId = userStore.userInfo?.id
    await approveRequest(row.id, { approverName, reason: '', approverId })
    ElMessage.success('审批通过')
    loadItems()
  } catch { ElMessage.error('审批失败') }
  finally { approvingId.value = null }
}

async function handleReject(row) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝审批', { inputType: 'textarea' })
    rejectingId.value = row.id
    const approverName = userStore.userInfo?.username || 'admin'
    const approverId = userStore.userInfo?.id
    await rejectRequest(row.id, { approverName, reason, approverId })
    ElMessage.success('已拒绝')
    loadItems()
  } catch { if (rejectingId.value) { rejectingId.value = null } }
}
onMounted(loadItems)
</script>

<style scoped>
.admin-approval-page { padding: 20px 0; }
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0 0 4px; font-size: 20px; font-weight: 600; }
.page-desc { margin: 0; color: #8b949e; font-size: 14px; }
.processed-text { color: #8b949e; font-size: 13px; }
</style>