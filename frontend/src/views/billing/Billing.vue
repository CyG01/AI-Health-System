<template>
  <div class="billing-page">
    <div class="page-header">
      <h2>计费与消费明细</h2>
      <p class="page-desc">查看AI服务用量、消费明细和订阅信息</p>
    </div>

    <!-- 用量概览 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <el-statistic title="今日Token消耗" :value="summary?.tokenUsed || 0" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <el-statistic title="今日API调用" :value="summary?.apiCalls || 0" suffix="次" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <el-statistic title="今日费用" :value="summary?.cost || 0" prefix="¥" :precision="2" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <el-statistic title="订阅等级" :value="subscription?.tier || 'free'">
            <template #suffix>
              <el-tag :type="tierTagType" size="small">{{ subscription?.tier || '免费版' }}</el-tag>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <!-- 订阅信息 -->
    <div class="section-card glass-card">
      <h3 class="card-title">订阅信息</h3>
      <el-descriptions :column="3" border v-if="subscription" v-loading="loading">
        <el-descriptions-item label="当前等级">{{ subscription.tier }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="subscription.status === 'active' ? 'success' : 'warning'">
            {{ subscription.status === 'active' ? '生效中' : subscription.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="到期时间">{{ subscription.expireTime || '长期有效' }}</el-descriptions-item>
        <el-descriptions-item label="自动续费">{{ subscription.autoRenew ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="月度Token额度">{{ formatTokenQuota(subscription.tokenQuota) }}</el-descriptions-item>
        <el-descriptions-item label="月费">{{ subscription.monthlyPrice ? '¥' + subscription.monthlyPrice : '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无订阅信息" />
    </div>

    <!-- 额度预警 -->
    <div class="section-card glass-card" v-if="quotaWarning">
      <h3 class="card-title">额度预警</h3>
      <el-alert
        v-if="quotaWarning.warningLevel"
        :title="quotaWarning.warningMessage || '额度使用提醒'"
        :type="quotaWarning.warningLevel === 'danger' ? 'error' : quotaWarning.warningLevel || 'info'"
        show-icon
        :closable="false"
      >
        <template v-if="quotaWarning.suggestion" #default>
          <p style="margin:0">{{ quotaWarning.suggestion }}</p>
        </template>
      </el-alert>
      <el-empty v-else description="额度充足" :image-size="60" />
    </div>

    <!-- 月度汇总 -->
    <div class="section-card glass-card">
      <h3 class="card-title">月度用量汇总</h3>
      <el-row :gutter="20" v-if="monthlySummary" v-loading="monthlyLoading">
        <el-col :span="6" v-for="item in monthlyItems" :key="item.label">
          <div class="monthly-stat">
            <div class="monthly-stat-value">{{ item.value }}</div>
            <div class="monthly-stat-label">{{ item.label }}</div>
          </div>
        </el-col>
      </el-row>
      <el-empty v-else description="暂无月度数据" :image-size="60" />
    </div>

    <!-- 消费明细历史 -->
    <div class="section-card glass-card">
      <h3 class="card-title">
        消费明细历史
        <el-select v-model="historyDays" size="small" style="width:120px;margin-left:12px" @change="loadHistory">
          <el-option :value="7" label="近7天" />
          <el-option :value="30" label="近30天" />
          <el-option :value="90" label="近90天" />
        </el-select>
      </h3>
      <el-table :data="usageHistory" stripe v-loading="historyLoading" empty-text="暂无消费记录">
        <el-table-column prop="usageDate" label="日期" width="120" />
        <el-table-column prop="tokensUsed" label="Token消耗" width="130">
          <template #default="{ row }">{{ formatTokenQuota(row.tokensUsed) }}</template>
        </el-table-column>
        <el-table-column prop="apiCalls" label="API调用次数" width="130" />
        <el-table-column prop="cost" label="费用(¥)" width="110">
          <template #default="{ row }">{{ row.cost?.toFixed(4) || '0.0000' }}</template>
        </el-table-column>
        <el-table-column prop="note" label="备注" min-width="150" show-overflow-tooltip />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getBillingSummary, getMonthlySummary, getBillingHistory, getSubscription, getQuotaWarning } from '@/api/billing'

const loading = ref(false)
const monthlyLoading = ref(false)
const historyLoading = ref(false)
const historyDays = ref(30)

const summary = ref(null)
const monthlySummary = ref(null)
const usageHistory = ref([])
const subscription = ref(null)
const quotaWarning = ref(null)

const tierTagType = computed(() => {
  const tier = subscription.value?.tier
  if (tier === 'pro' || tier === 'enterprise') return 'primary'
  if (tier === 'free') return 'info'
  return ''
})

const monthlyItems = computed(() => {
  if (!monthlySummary.value) return []
  return [
    { label: 'Token总量', value: formatTokenQuota(monthlySummary.value.totalTokens) },
    { label: '累计费用', value: '¥' + (monthlySummary.value.totalCost || 0).toFixed(2) },
    { label: '套餐限额', value: formatTokenQuota(monthlySummary.value.quotaLimit) },
    { label: '剩余额度', value: formatTokenQuota(monthlySummary.value.remainingQuota) }
  ]
})

function formatTokenQuota(val) {
  if (val == null) return '-'
  if (val >= 1000000) return (val / 1000000).toFixed(2) + 'M'
  if (val >= 1000) return (val / 1000).toFixed(1) + 'K'
  return String(val)
}

async function loadSummary() {
  try {
    const res = await getBillingSummary()
    summary.value = res.data
  } catch { /* ignore */ }
}

async function loadMonthly() {
  monthlyLoading.value = true
  try {
    const res = await getMonthlySummary()
    monthlySummary.value = res.data
  } finally { monthlyLoading.value = false }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const res = await getBillingHistory({ days: historyDays.value })
    usageHistory.value = res.data || []
  } finally { historyLoading.value = false }
}

async function loadSubscription() {
  loading.value = true
  try {
    const res = await getSubscription()
    subscription.value = res.data
  } finally { loading.value = false }
}

async function loadQuotaWarning() {
  try {
    const res = await getQuotaWarning()
    quotaWarning.value = res.data
  } catch { /* ignore */ }
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
.page-header h2 { margin: 0 0 4px; font-size: 20px; font-weight: 600; }
.page-desc { margin: 0; color: #8b949e; font-size: 14px; }
.stats-row { margin-bottom: 20px; }
.stat-card { text-align: center; }
.section-card { padding: 20px; margin-bottom: 20px; }
.card-title { margin: 0 0 16px; font-size: 16px; font-weight: 600; display: flex; align-items: center; }
.monthly-stat { text-align: center; padding: 16px 0; }
.monthly-stat-value { font-size: 24px; font-weight: 700; color: #58a6ff; }
.monthly-stat-label { font-size: 13px; color: #8b949e; margin-top: 4px; }
</style>