<template>
  <div class="billing-page">
    <div class="page-header">
      <h2 class="text-xl font-semibold">{{ $t('billing.title') || '计费与消费明细' }}</h2>
      <p class="text-sm text-secondary">{{ $t('billing.desc') || '查看AI服务用量、消费明细和订阅信息' }}</p>
    </div>

    <!-- 用量概览 -->
    <NGrid :x-gap="16" :y-gap="16" :cols="4" class="mb-5" item-responsive responsive="screen">
      <NGi span="4 m:1">
        <NCard size="small" class="text-center">
          <NStatistic :label="$t('billing.todayToken') || '今日Token消耗'" :value="(summary?.inputTokens || 0) + (summary?.outputTokens || 0)" />
        </NCard>
      </NGi>
      <NGi span="4 m:1">
        <NCard size="small" class="text-center">
          <NStatistic :label="$t('billing.todayApi') || '今日API调用'" :value="summary?.apiCallCount || 0">
            <template #suffix>{{ $t('billing.times') || '次' }}</template>
          </NStatistic>
        </NCard>
      </NGi>
      <NGi span="4 m:1">
        <NCard size="small" class="text-center">
          <NStatistic :label="$t('billing.todayCost') || '今日费用'" :value="summary?.dailyCost || 0">
            <template #prefix>&#165;</template>
          </NStatistic>
        </NCard>
      </NGi>
      <NGi span="4 m:1">
        <NCard size="small" class="text-center">
          <NStatistic :label="$t('billing.tier') || '订阅等级'" :value="subscription?.tier || 'free'">
            <template #suffix>
              <NTag :type="tierTagType" size="small">{{ subscription?.tier || '免费版' }}</NTag>
            </template>
          </NStatistic>
        </NCard>
      </NGi>
    </NGrid>

    <!-- 订阅信息 -->
    <NCard :title="$t('billing.subscription') || '订阅信息'" class="mb-5">
      <NSpin :show="loading">
        <NDescriptions v-if="subscription" :column="3" bordered label-placement="left">
          <NDescriptionsItem :label="$t('billing.currentTier') || '当前等级'">
            {{ subscription.tier }}
          </NDescriptionsItem>
          <NDescriptionsItem :label="$t('billing.status') || '状态'">
            <NTag :type="subscription.status === 'active' ? 'success' : 'warning'" size="small">
              {{ subscription.status === 'active' ? '生效中' : subscription.status }}
            </NTag>
          </NDescriptionsItem>
          <NDescriptionsItem :label="$t('billing.expireTime') || '到期时间'">
            {{ subscription.endTime || '长期有效' }}
          </NDescriptionsItem>
          <NDescriptionsItem :label="$t('billing.autoRenew') || '自动续费'">
            {{ subscription.autoRenew ? '是' : '否' }}
          </NDescriptionsItem>
          <NDescriptionsItem :label="$t('billing.monthlyTokenQuota') || '月度Token额度'">
            {{ formatTokenQuota(subscription.customTokenQuotaM) }}
          </NDescriptionsItem>
          <NDescriptionsItem :label="$t('billing.monthlyPrice') || '月费'">
            {{ subscription.customPrice ? '¥' + subscription.customPrice : '-' }}
          </NDescriptionsItem>
        </NDescriptions>
        <NEmpty v-else :description="$t('billing.noSubscription') || '暂无订阅信息'" />
      </NSpin>
    </NCard>

    <!-- 额度预警 -->
    <NCard v-if="quotaWarning" :title="$t('billing.quotaWarning') || '额度预警'" class="mb-5">
      <NAlert
        v-if="quotaWarning.warningLevel"
        :title="quotaWarning.warningMessage || '额度使用提醒'"
        :type="quotaWarning.warningLevel === 'danger' ? 'error' : (quotaWarning.warningLevel as 'info' | 'warning' | 'error') || 'info'"
      >
        <p v-if="quotaWarning.suggestion" class="m-0">{{ quotaWarning.suggestion }}</p>
      </NAlert>
      <NEmpty v-else :description="$t('billing.quotaOk') || '额度充足'" />
    </NCard>

    <!-- 月度汇总 -->
    <NCard :title="$t('billing.monthlySummary') || '月度用量汇总'" class="mb-5">
      <NSpin :show="monthlyLoading">
        <NGrid v-if="monthlySummary" :x-gap="16" :y-gap="16" :cols="4" item-responsive responsive="screen">
          <NGi v-for="item in monthlyItems" :key="item.label" span="4 m:1">
            <div class="text-center py-4">
              <div class="text-2xl font-bold text-[#4ECDC4]">{{ item.value }}</div>
              <div class="text-[13px] text-secondary mt-1">{{ item.label }}</div>
            </div>
          </NGi>
        </NGrid>
        <NEmpty v-else :description="$t('billing.noMonthlyData') || '暂无月度数据'" />
      </NSpin>
    </NCard>

    <!-- 消费明细历史 -->
    <NCard :title="$t('billing.history') || '消费明细历史'">
      <template #header-extra>
        <NSelect
          v-model:value="historyDays"
          :options="historyDaysOptions"
          size="small"
          class="w-[120px]"
          @update:value="loadHistory"
        />
      </template>
      <NDataTable
        :data="usageHistory"
        :columns="historyColumns"
        :loading="historyLoading"
        :bordered="false"
        striped
        :scroll-x="1100"
      />
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import {
  NCard, NGrid, NGi, NStatistic, NTag, NDescriptions, NDescriptionsItem,
  NDataTable, NEmpty, NAlert, NSpin, NSelect,
  useMessage
} from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import { fetchGetBillingSummary, fetchGetMonthlySummary, fetchGetBillingHistory, fetchGetSubscription, fetchGetQuotaWarning } from '@/service/api'

defineOptions({ name: 'BillingOverview' })
const message = useMessage()

interface UsageRecord extends Api.Billing.UserUsage {
  note?: string
}

const loading = ref(false)
const monthlyLoading = ref(false)
const historyLoading = ref(false)
const historyDays = ref(30)

const summary = ref<Api.Billing.Balance | null>(null)
const monthlySummary = ref<Api.Billing.MonthlySummary | null>(null)
const usageHistory = ref<UsageRecord[]>([])
const subscription = ref<Api.Billing.Subscription | null>(null)
const quotaWarning = ref<Api.Billing.QuotaWarning | null>(null)

const historyDaysOptions = [
  { label: '近7天', value: 7 },
  { label: '近30天', value: 30 },
  { label: '近90天', value: 90 }
]

const tierTagType = computed<'default' | 'info' | 'success' | 'warning' | 'error'>(() => {
  const tier = subscription.value?.tier
  if (tier === 'pro' || tier === 'enterprise') return 'success'
  if (tier === 'free') return 'info'
  return 'default'
})

const monthlyItems = computed(() => {
  if (!monthlySummary.value) return []
  return [
    { label: 'Token总量', value: formatTokenQuota(monthlySummary.value.monthlyTokens) },
    { label: '累计费用', value: '¥' + (monthlySummary.value.monthlyCost || 0).toFixed(2) },
    { label: '套餐限额', value: formatTokenQuota(monthlySummary.value.monthlyTokenLimitM) },
    { label: '是否超额', value: monthlySummary.value.monthlyLimitExceeded ? '已超额' : '未超额' }
  ]
})

const historyColumns: DataTableColumns<UsageRecord> = [
  { title: '日期', key: 'usageDate', width: 120 },
  {
    title: 'Token消耗', key: 'tokens', width: 130,
    render: (row) => formatTokenQuota((row.inputTokens || 0) + (row.outputTokens || 0))
  },
  { title: 'API调用次数', key: 'apiCallCount', width: 130 },
  {
    title: '费用(¥)', key: 'dailyCost', width: 110,
    render: (row) => row.dailyCost?.toFixed(4) || '0.0000'
  },
  { title: '备注', key: 'note', ellipsis: { tooltip: true } }
]

function formatTokenQuota(val: number | null | undefined): string {
  if (val == null) return '-'
  if (val >= 1000000) return (val / 1000000).toFixed(2) + 'M'
  if (val >= 1000) return (val / 1000).toFixed(1) + 'K'
  return String(val)
}

async function loadSummary() {
  try {
    const { data } = await fetchGetBillingSummary()
    summary.value = data
  } catch { message.error('加载账单概览失败') }
}

async function loadMonthly() {
  monthlyLoading.value = true
  try {
    const { data } = await fetchGetMonthlySummary()
    monthlySummary.value = data
  } finally { monthlyLoading.value = false }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const { data } = await fetchGetBillingHistory({ days: historyDays.value })
    usageHistory.value = data || []
  } finally { historyLoading.value = false }
}

async function loadSubscription() {
  loading.value = true
  try {
    const { data } = await fetchGetSubscription()
    subscription.value = data
  } finally { loading.value = false }
}

async function loadQuotaWarning() {
  try {
    const { data } = await fetchGetQuotaWarning()
    quotaWarning.value = data
  } catch { message.error('加载预警信息失败') }
}

onMounted(() => {
  loadSummary()
  loadMonthly()
  loadHistory()
  loadSubscription()
  loadQuotaWarning()
})
</script>

<style scoped>
.billing-page { padding: 0; }

.page-header { margin-bottom: 20px; }

.text-secondary {
  color: var(--sport-text-tertiary);
}
</style>
