<template>
  <div class="refund-invoice-page">
    <div class="page-header">
      <h2 class="text-xl font-semibold">{{ $t('billing.refundInvoice') || '退款与发票' }}</h2>
      <p class="text-sm text-secondary">{{ $t('billing.refundInvoiceDesc') || '管理退款申请和发票开具' }}</p>
    </div>

    <NTabs v-model:value="activeTab" type="line">
      <!-- 退款管理 -->
      <NTabPane :name="$t('billing.refund') || '退款管理'" tab="退款管理">
        <NGrid :x-gap="16" :y-gap="16" :cols="2" item-responsive responsive="screen">
          <NGi span="2 m:1">
            <NCard :title="$t('billing.refundStatus') || '退款状态'">
              <NSpin :show="refundLoading">
                <NDescriptions v-if="refundStatus" :column="1" bordered label-placement="left">
                  <NDescriptionsItem :label="$t('billing.currentTier') || '当前等级'">
                    {{ refundStatus.tier }}
                  </NDescriptionsItem>
                  <NDescriptionsItem :label="$t('billing.status') || '订阅状态'">
                    <NTag :type="refundStatus.status === 'refunded' ? 'error' : (refundStatus.status === 'active' ? 'success' : 'warning')" size="small">
                      {{ statusLabel(refundStatus.status) }}
                    </NTag>
                  </NDescriptionsItem>
                  <NDescriptionsItem :label="$t('billing.expireTime') || '到期时间'">
                    {{ refundStatus.expireTime || '-' }}
                  </NDescriptionsItem>
                </NDescriptions>
                <NEmpty v-else :description="$t('common.noData') || '暂无信息'" />
              </NSpin>
            </NCard>
          </NGi>
          <NGi span="2 m:1">
            <NCard :title="$t('billing.applyRefund') || '申请退款'">
              <NForm :model="refundForm" @submit.prevent="handleRefund" label-placement="left" label-width="80px">
                <NFormItem :label="$t('billing.refundReason') || '退款原因'" required>
                  <NInput
                    v-model:value="refundForm.reason"
                    type="textarea"
                    :rows="3"
                    :placeholder="$t('billing.refundReasonPlaceholder') || '请描述退款原因...'"
                  />
                </NFormItem>
                <NFormItem>
                  <NButton type="error" :loading="refundSubmitting" attr-type="submit">
                    {{ $t('billing.submitRefund') || '提交退款申请' }}
                  </NButton>
                </NFormItem>
              </NForm>
              <NAlert :title="$t('common.tip') || '提示'" type="info" class="mt-3">
                7天内无理由退款，超过7天按剩余天数比例退款
              </NAlert>
            </NCard>
          </NGi>
        </NGrid>
      </NTabPane>

      <!-- 发票管理 -->
      <NTabPane :name="$t('billing.invoice') || '发票管理'" tab="发票管理">
        <NGrid :x-gap="16" :y-gap="16" :cols="2" item-responsive responsive="screen">
          <NGi span="2 m:1">
            <NCard :title="$t('billing.applyInvoice') || '申请开具发票'">
              <NForm :model="invoiceForm" @submit.prevent="handleInvoice" label-placement="left" label-width="90px">
                <NFormItem :label="$t('billing.orderNo') || '关联订单号'" required>
                  <NInput v-model:value="invoiceForm.orderNo" :placeholder="$t('billing.orderNoPlaceholder') || '请输入订单号'" />
                </NFormItem>
                <NFormItem :label="$t('billing.invoiceType') || '发票类型'" required>
                  <NRadioGroup v-model:value="invoiceForm.invoiceType">
                    <NRadio value="personal">{{ $t('billing.personal') || '个人' }}</NRadio>
                    <NRadio value="enterprise">{{ $t('billing.enterprise') || '企业' }}</NRadio>
                  </NRadioGroup>
                </NFormItem>
                <NFormItem :label="$t('billing.invoiceTitle') || '发票抬头'" required>
                  <NInput v-model:value="invoiceForm.invoiceTitle" :placeholder="$t('billing.invoiceTitlePlaceholder') || '请输入发票抬头'" />
                </NFormItem>
                <NFormItem v-if="invoiceForm.invoiceType === 'enterprise'" :label="$t('billing.taxNumber') || '税号'" required>
                  <NInput v-model:value="invoiceForm.taxNumber" :placeholder="$t('billing.taxNumberPlaceholder') || '请输入企业税号'" />
                </NFormItem>
                <NFormItem>
                  <NButton type="primary" :loading="invoiceSubmitting" attr-type="submit">
                    {{ $t('billing.submitInvoice') || '提交申请' }}
                  </NButton>
                </NFormItem>
              </NForm>
            </NCard>
          </NGi>
          <NGi span="2 m:1">
            <NCard :title="$t('billing.myInvoices') || '我的发票'">
              <NDataTable
                :data="invoiceList"
                :columns="invoiceColumns"
                :loading="invoiceListLoading"
                :bordered="false"
                striped
                :max-height="400"
              />
            </NCard>
          </NGi>
        </NGrid>
      </NTabPane>
    </NTabs>

    <!-- 发票详情弹窗 -->
    <NModal
      v-model:show="invoiceDialogVisible"
      preset="card"
      :title="$t('billing.invoiceDetail') || '发票详情'"
      style="width: 500px; max-width: 90vw"
    >
      <NDescriptions v-if="invoiceDetail" :column="1" bordered label-placement="left">
        <NDescriptionsItem :label="$t('billing.invoiceNo') || '发票号'">
          {{ invoiceDetail.invoiceNo }}
        </NDescriptionsItem>
        <NDescriptionsItem :label="$t('billing.invoiceType') || '类型'">
          {{ invoiceDetail.invoiceType === 'personal' ? '个人' : '企业' }}
        </NDescriptionsItem>
        <NDescriptionsItem :label="$t('billing.invoiceTitle') || '抬头'">
          {{ invoiceDetail.invoiceTitle }}
        </NDescriptionsItem>
        <NDescriptionsItem v-if="invoiceDetail.taxNumber" :label="$t('billing.taxNumber') || '税号'">
          {{ invoiceDetail.taxNumber }}
        </NDescriptionsItem>
        <NDescriptionsItem :label="$t('billing.amount') || '金额'">
          ¥{{ invoiceDetail.amount?.toFixed(2) || '-' }}
        </NDescriptionsItem>
        <NDescriptionsItem :label="$t('billing.status') || '状态'">
          {{ invoiceDetail.status }}
        </NDescriptionsItem>
        <NDescriptionsItem :label="$t('billing.createTime') || '开具时间'">
          {{ invoiceDetail.createTime || '-' }}
        </NDescriptionsItem>
      </NDescriptions>
      <template #footer>
        <div class="flex justify-end">
          <NButton @click="invoiceDialogVisible = false">{{ $t('common.close') || '关闭' }}</NButton>
        </div>
      </template>
    </NModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import {
  NTabs, NTabPane, NGrid, NGi, NCard, NDescriptions, NDescriptionsItem,
  NForm, NFormItem, NInput, NButton, NTag, NDataTable, NEmpty, NAlert,
  NRadioGroup, NRadio, NModal, NSpin,
  useMessage, useDialog
} from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import { fetchApplyRefund, fetchGetRefundStatus, fetchApplyInvoice, fetchGetInvoiceList, fetchGetInvoiceDetail, fetchCancelInvoice } from '@/service/api'

defineOptions({ name: 'RefundInvoice' })
const message = useMessage()
const dialog = useDialog()

interface RefundStatus {
  tier: string
  status: string
  expireTime?: string
}

interface Invoice {
  id: number | string
  invoiceNo: string
  invoiceType: string
  invoiceTitle: string
  taxNumber?: string
  amount: number
  status: string
  createTime?: string
}

const activeTab = ref('退款管理')

// 退款
const refundLoading = ref(false)
const refundSubmitting = ref(false)
const refundStatus = ref<RefundStatus | null>(null)
const refundForm = reactive({ reason: '' })

// 发票
const invoiceSubmitting = ref(false)
const invoiceListLoading = ref(false)
const invoiceList = ref<Invoice[]>([])
const invoiceForm = reactive({
  orderNo: '',
  invoiceType: 'personal',
  invoiceTitle: '',
  taxNumber: ''
})
const invoiceDialogVisible = ref(false)
const invoiceDetail = ref<Invoice | null>(null)

function statusLabel(s: string): string {
  const map: Record<string, string> = {
    active: '生效中', cancelled: '已取消', refunded: '已退款', expired: '已过期', free: '免费版'
  }
  return map[s] || s
}

const invoiceColumns: DataTableColumns<Invoice> = [
  { title: '发票号', key: 'invoiceNo', ellipsis: { tooltip: true }, minWidth: 140 },
  {
    title: '类型', key: 'invoiceType', width: 70,
    render: (row) => row.invoiceType === 'personal' ? '个人' : '企业'
  },
  { title: '抬头', key: 'invoiceTitle', ellipsis: { tooltip: true }, minWidth: 120 },
  {
    title: '金额(¥)', key: 'amount', width: 100,
    render: (row) => row.amount?.toFixed(2) || '-'
  },
  {
    title: '状态', key: 'status', width: 90,
    render: (row) => h(NTag, {
      type: row.status === 'issued' ? 'success' : 'warning',
      size: 'small'
    }, { default: () => row.status === 'issued' ? '已开具' : row.status || '-' })
  },
  {
    title: '操作', key: 'actions', width: 140,
    render: (row) => h('div', { class: 'flex gap-2' }, [
      h(NButton, { text: true, size: 'small', onClick: () => viewInvoice(row) }, { default: () => '详情' }),
      row.status !== 'cancelled'
        ? h(NButton, { text: true, size: 'small', type: 'error', onClick: () => handleCancelInvoice(row) }, { default: () => '作废' })
        : null
    ])
  }
]

async function loadRefundStatus() {
  refundLoading.value = true
  try {
    const { data } = await fetchGetRefundStatus()
    refundStatus.value = data as any
  } catch { /* ignore */ }
  finally { refundLoading.value = false }
}

async function handleRefund() {
  if (!refundForm.reason.trim()) { message.warning('请输入退款原因'); return }
  refundSubmitting.value = true
  try {
    await fetchApplyRefund(refundForm.reason)
    message.success('退款申请已提交')
    loadRefundStatus()
  } catch {
    message.error('退款申请失败')
  }
  finally { refundSubmitting.value = false }
}

async function loadInvoiceList() {
  invoiceListLoading.value = true
  try {
    const { data } = await fetchGetInvoiceList()
    invoiceList.value = (data as any) || []
  } catch { /* ignore */ }
  finally { invoiceListLoading.value = false }
}

async function handleInvoice() {
  if (!invoiceForm.orderNo.trim()) { message.warning('请输入订单号'); return }
  if (!invoiceForm.invoiceTitle.trim()) { message.warning('请输入发票抬头'); return }
  if (invoiceForm.invoiceType === 'enterprise' && !invoiceForm.taxNumber.trim()) {
    message.warning('请输入税号'); return
  }
  invoiceSubmitting.value = true
  try {
    await fetchApplyInvoice({ ...invoiceForm })
    message.success('发票申请已提交')
    invoiceForm.orderNo = ''
    invoiceForm.invoiceType = 'personal'
    invoiceForm.invoiceTitle = ''
    invoiceForm.taxNumber = ''
    loadInvoiceList()
  } catch {
    message.error('发票申请失败')
  }
  finally { invoiceSubmitting.value = false }
}

async function viewInvoice(row: Invoice) {
  try {
    const { data } = await fetchGetInvoiceDetail(row.id as number)
    invoiceDetail.value = data as any
    invoiceDialogVisible.value = true
  } catch {
    message.error('获取发票详情失败')
  }
}

function handleCancelInvoice(row: Invoice) {
  dialog.warning({
    title: '确认',
    content: '确定要作废该发票吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await fetchCancelInvoice(row.id as number)
        message.success('发票已作废')
        loadInvoiceList()
      } catch { /* cancelled */ }
    }
  })
}

onMounted(() => {
  loadRefundStatus()
  loadInvoiceList()
})
</script>

<style scoped>
.refund-invoice-page { padding: 0; }

.page-header { margin-bottom: 20px; }

.text-secondary {
  color: #8b949e;
}
</style>
