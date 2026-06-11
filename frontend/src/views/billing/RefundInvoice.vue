<template>
  <div class="refund-invoice-page">
    <div class="page-header">
      <h2>退款与发票</h2>
      <p class="page-desc">管理退款申请和发票开具</p>
    </div>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- 退款管理 -->
      <el-tab-pane label="退款管理" name="refund">
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="section-card glass-card">
              <h3 class="card-title">退款状态</h3>
              <el-descriptions :column="1" border v-if="refundStatus" v-loading="refundLoading">
                <el-descriptions-item label="当前等级">{{ refundStatus.tier }}</el-descriptions-item>
                <el-descriptions-item label="订阅状态">
                  <el-tag :type="refundStatus.status === 'refunded' ? 'danger' : (refundStatus.status === 'active' ? 'success' : 'warning')">
                    {{ statusLabel(refundStatus.status) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="到期时间">{{ refundStatus.expireTime || '-' }}</el-descriptions-item>
              </el-descriptions>
              <el-empty v-else description="暂无信息" :image-size="60" />
            </div>
          </el-col>
          <el-col :span="12">
            <div class="section-card glass-card">
              <h3 class="card-title">申请退款</h3>
              <el-form :model="refundForm" @submit.prevent="handleRefund" label-width="80px">
                <el-form-item label="退款原因" required>
                  <el-input
                    v-model="refundForm.reason"
                    type="textarea"
                    :rows="3"
                    placeholder="请描述退款原因..."
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="danger" :loading="refundSubmitting" native-type="submit">提交退款申请</el-button>
                </el-form-item>
              </el-form>
              <el-alert title="提示" type="info" :closable="false" show-icon style="margin-top:12px">
                7天内无理由退款，超过7天按剩余天数比例退款
              </el-alert>
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 发票管理 -->
      <el-tab-pane label="发票管理" name="invoice">
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="section-card glass-card">
              <h3 class="card-title">申请开具发票</h3>
              <el-form :model="invoiceForm" @submit.prevent="handleInvoice" label-width="90px">
                <el-form-item label="关联订单号" required>
                  <el-input v-model="invoiceForm.orderNo" placeholder="请输入订单号" />
                </el-form-item>
                <el-form-item label="发票类型" required>
                  <el-radio-group v-model="invoiceForm.invoiceType">
                    <el-radio value="personal">个人</el-radio>
                    <el-radio value="enterprise">企业</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="发票抬头" required>
                  <el-input v-model="invoiceForm.invoiceTitle" placeholder="请输入发票抬头" />
                </el-form-item>
                <el-form-item label="税号" v-if="invoiceForm.invoiceType === 'enterprise'" required>
                  <el-input v-model="invoiceForm.taxNumber" placeholder="请输入企业税号" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="invoiceSubmitting" native-type="submit">提交申请</el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="section-card glass-card">
              <h3 class="card-title">我的发票</h3>
              <el-table :data="invoiceList" stripe v-loading="invoiceListLoading" empty-text="暂无发票" max-height="400">
                <el-table-column label="发票号" prop="invoiceNo" min-width="140" show-overflow-tooltip />
                <el-table-column label="类型" width="70">
                  <template #default="{ row }">{{ row.invoiceType === 'personal' ? '个人' : '企业' }}</template>
                </el-table-column>
                <el-table-column label="抬头" prop="invoiceTitle" min-width="120" show-overflow-tooltip />
                <el-table-column label="金额(¥)" width="100">
                  <template #default="{ row }">{{ row.amount?.toFixed(2) || '-' }}</template>
                </el-table-column>
                <el-table-column label="状态" width="90">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 'issued' ? 'success' : 'warning'" size="small">
                      {{ row.status === 'issued' ? '已开具' : row.status || '-' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="140">
                  <template #default="{ row }">
                    <el-button text size="small" @click="viewInvoice(row)">详情</el-button>
                    <el-button text size="small" type="danger" @click="handleCancelInvoice(row)" v-if="row.status !== 'cancelled'">作废</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>

    <!-- 发票详情弹窗 -->
    <el-dialog v-model="invoiceDialogVisible" title="发票详情" width="500px">
      <el-descriptions :column="1" border v-if="invoiceDetail">
        <el-descriptions-item label="发票号">{{ invoiceDetail.invoiceNo }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ invoiceDetail.invoiceType === 'personal' ? '个人' : '企业' }}</el-descriptions-item>
        <el-descriptions-item label="抬头">{{ invoiceDetail.invoiceTitle }}</el-descriptions-item>
        <el-descriptions-item label="税号" v-if="invoiceDetail.taxNumber">{{ invoiceDetail.taxNumber }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ invoiceDetail.amount?.toFixed(2) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ invoiceDetail.status }}</el-descriptions-item>
        <el-descriptions-item label="开具时间">{{ invoiceDetail.createTime || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="invoiceDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { applyRefund, getRefundStatus, applyInvoice, getInvoiceList, getInvoiceDetail, cancelInvoice } from '@/api/billing'

const activeTab = ref('refund')

// 退款
const refundLoading = ref(false)
const refundSubmitting = ref(false)
const refundStatus = ref(null)
const refundForm = ref({ reason: '' })

// 发票
const invoiceSubmitting = ref(false)
const invoiceListLoading = ref(false)
const invoiceList = ref([])
const invoiceForm = ref({ orderNo: '', invoiceType: 'personal', invoiceTitle: '', taxNumber: '' })
const invoiceDialogVisible = ref(false)
const invoiceDetail = ref(null)

function statusLabel(s) {
  const map = { active: '生效中', cancelled: '已取消', refunded: '已退款', expired: '已过期', free: '免费版' }
  return map[s] || s
}

async function loadRefundStatus() {
  refundLoading.value = true
  try { const res = await getRefundStatus(); refundStatus.value = res.data } catch { /* ignore */ }
  finally { refundLoading.value = false }
}

async function handleRefund() {
  if (!refundForm.value.reason.trim()) { ElMessage.warning('请输入退款原因'); return }
  refundSubmitting.value = true
  try {
    await applyRefund(refundForm.value.reason)
    ElMessage.success('退款申请已提交')
    loadRefundStatus()
  } catch { ElMessage.error('退款申请失败') }
  finally { refundSubmitting.value = false }
}

async function loadInvoiceList() {
  invoiceListLoading.value = true
  try { const res = await getInvoiceList(); invoiceList.value = res.data || [] } catch { /* ignore */ }
  finally { invoiceListLoading.value = false }
}

async function handleInvoice() {
  if (!invoiceForm.value.orderNo.trim()) { ElMessage.warning('请输入订单号'); return }
  if (!invoiceForm.value.invoiceTitle.trim()) { ElMessage.warning('请输入发票抬头'); return }
  if (invoiceForm.value.invoiceType === 'enterprise' && !invoiceForm.value.taxNumber.trim()) { ElMessage.warning('请输入税号'); return }
  invoiceSubmitting.value = true
  try {
    await applyInvoice(invoiceForm.value)
    ElMessage.success('发票申请已提交')
    invoiceForm.value = { orderNo: '', invoiceType: 'personal', invoiceTitle: '', taxNumber: '' }
    loadInvoiceList()
  } catch { ElMessage.error('发票申请失败') }
  finally { invoiceSubmitting.value = false }
}

async function viewInvoice(row) {
  try {
    const res = await getInvoiceDetail(row.id)
    invoiceDetail.value = res.data
    invoiceDialogVisible.value = true
  } catch { ElMessage.error('获取发票详情失败') }
}

async function handleCancelInvoice(row) {
  try {
    await ElMessageBox.confirm('确定要作废该发票吗？', '确认', { type: 'warning' })
    await cancelInvoice(row.id)
    ElMessage.success('发票已作废')
    loadInvoiceList()
  } catch { /* cancelled */ }
}

onMounted(() => {
  loadRefundStatus()
  loadInvoiceList()
})
</script>

<style scoped>
.refund-invoice-page { padding: 0; }
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0 0 4px; font-size: 20px; font-weight: 600; }
.page-desc { margin: 0; color: #8b949e; font-size: 14px; }
.section-card { padding: 20px; height: 100%; }
.card-title { margin: 0 0 16px; font-size: 16px; font-weight: 600; }
</style>