<template>
  <div class="enterprise-page">
    <div class="page-header">
      <h2>企业版定制化订阅</h2>
      <p class="page-desc">为团队定制Token额度与价格方案</p>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <div class="section-card glass-card">
          <h3 class="card-title">当前配置</h3>
          <el-descriptions :column="1" border v-if="config" v-loading="configLoading">
            <el-descriptions-item label="订阅等级">{{ config.tier || '-' }}</el-descriptions-item>
            <el-descriptions-item label="团队人数">{{ config.teamSize || '-' }}</el-descriptions-item>
            <el-descriptions-item label="月度Token额度">{{ formatQuota(config.tokenQuota) }}</el-descriptions-item>
            <el-descriptions-item label="月费">¥{{ config.monthlyPrice || '-' }}</el-descriptions-item>
            <el-descriptions-item label="到期时间">{{ config.expireTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="config.status === 'active' ? 'success' : 'warning'">{{ config.status }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="尚未配置企业版" :image-size="80" />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="section-card glass-card">
          <h3 class="card-title">{{ config ? '更新配置' : '激活企业版' }}</h3>
          <el-form :model="form" label-width="160px" @submit.prevent="handleActivate">
            <el-form-item label="团队人数" required>
              <el-input-number v-model="form.teamSize" :min="5" :max="10000" :step="5" />
            </el-form-item>
            <el-form-item label="月度Token额度(百万)" required>
              <el-input-number v-model="form.customTokenQuotaM" :min="1" :max="1000" :step="1" />
              <span class="form-hint">1M ≈ 75万词</span>
            </el-form-item>
            <el-form-item label="自定义价格(元/月)" required>
              <el-input-number v-model="form.customPrice" :min="99" :max="99999" :step="1" :precision="2" />
            </el-form-item>
            <el-form-item label="订阅月数" required>
              <el-input-number v-model="form.months" :min="1" :max="60" :step="1" />
            </el-form-item>
            <el-form-item label="支付渠道">
              <el-select v-model="form.channel">
                <el-option value="alipay" label="支付宝" />
                <el-option value="wechat" label="微信支付" />
                <el-option value="bank" label="银行转账" />
              </el-select>
            </el-form-item>
            <el-form-item label="订单号">
              <el-input v-model="form.orderNo" placeholder="购买时填写的订单号" />
            </el-form-item>
            <el-form-item>
              <dl>
                <dt>预估总费用:</dt>
                <dd class="total-price">¥{{ estimatedTotal }}</dd>
              </dl>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" native-type="submit" :loading="submitting">
                {{ config ? '更新配置' : '激活企业版' }}
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { activateEnterprisePlan, updateEnterpriseConfig, getEnterpriseConfig } from '@/api/enterprise'

const configLoading = ref(false)
const submitting = ref(false)
const config = ref(null)

const form = ref({
  teamSize: 10,
  customTokenQuotaM: 10,
  customPrice: 999,
  months: 12,
  channel: 'alipay',
  orderNo: ''
})

const estimatedTotal = computed(() => (form.value.customPrice * form.value.months).toFixed(2))

function formatQuota(val) {
  if (val == null) return '-'
  if (val >= 1000000) return (val / 1000000).toFixed(2) + 'M'
  return String(val)
}

async function loadConfig() {
  configLoading.value = true
  try {
    const res = await getEnterpriseConfig()
    if (res.data && res.data.message) {
      config.value = null
    } else {
      config.value = res.data
      if (config.value) {
        form.value.teamSize = config.value.teamSize || 10
        form.value.customTokenQuotaM = config.value.tokenQuota ? Math.round(config.value.tokenQuota / 1000000) : 10
        form.value.customPrice = config.value.monthlyPrice || 999
      }
    }
  } catch { config.value = null }
  finally { configLoading.value = false }
}

async function handleActivate() {
  submitting.value = true
  try {
    if (config.value) {
      await updateEnterpriseConfig({
        teamSize: form.value.teamSize,
        customTokenQuotaM: form.value.customTokenQuotaM,
        customPrice: form.value.customPrice
      })
      ElMessage.success('配置已更新')
    } else {
      await activateEnterprisePlan(form.value)
      ElMessage.success('企业版已激活')
    }
    loadConfig()
  } catch { ElMessage.error('操作失败') }
  finally { submitting.value = false }
}

onMounted(loadConfig)
</script>

<style scoped>
.enterprise-page { padding: 0; }
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0 0 4px; font-size: 20px; font-weight: 600; }
.page-desc { margin: 0; color: #8b949e; font-size: 14px; }
.section-card { padding: 20px; }
.card-title { margin: 0 0 16px; font-size: 16px; font-weight: 600; }
.form-hint { margin-left: 8px; color: #8b949e; font-size: 12px; }
.total-price { font-size: 28px; font-weight: 700; color: #58a6ff; margin: 0; }
</style>