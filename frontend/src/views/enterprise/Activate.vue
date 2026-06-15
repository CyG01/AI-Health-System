<template>
  <div class="enterprise-page">
    <div class="page-header">
      <h2 class="text-xl font-semibold">{{ $t('enterprise.title') || '企业版定制化订阅' }}</h2>
      <p class="text-sm text-secondary">{{ $t('enterprise.desc') || '为团队定制Token额度与价格方案' }}</p>
    </div>

    <NGrid :x-gap="16" :y-gap="16" :cols="2" item-responsive responsive="screen">
      <!-- 当前配置 -->
      <NGi span="2 m:1">
        <NCard :title="$t('enterprise.currentConfig') || '当前配置'">
          <NSpin :show="configLoading">
            <NDescriptions v-if="config" :column="1" bordered label-placement="left">
              <NDescriptionsItem :label="$t('enterprise.tier') || '订阅等级'">
                {{ config.tier || '-' }}
              </NDescriptionsItem>
              <NDescriptionsItem :label="$t('enterprise.teamSize') || '团队人数'">
                {{ config.teamSize || '-' }}
              </NDescriptionsItem>
              <NDescriptionsItem :label="$t('enterprise.monthlyTokenQuota') || '月度Token额度'">
                {{ formatQuota(config.tokenQuota) }}
              </NDescriptionsItem>
              <NDescriptionsItem :label="$t('enterprise.monthlyPrice') || '月费'">
                ¥{{ config.monthlyPrice || '-' }}
              </NDescriptionsItem>
              <NDescriptionsItem :label="$t('enterprise.expireTime') || '到期时间'">
                {{ config.expireTime || '-' }}
              </NDescriptionsItem>
              <NDescriptionsItem :label="$t('enterprise.status') || '状态'">
                <NTag :type="config.status === 'active' ? 'success' : 'warning'" size="small">
                  {{ config.status }}
                </NTag>
              </NDescriptionsItem>
            </NDescriptions>
            <NEmpty v-else :description="$t('enterprise.notConfigured') || '尚未配置企业版'" />
          </NSpin>
        </NCard>
      </NGi>

      <!-- 激活/更新表单 -->
      <NGi span="2 m:1">
        <NCard :title="config ? ($t('enterprise.updateConfig') || '更新配置') : ($t('enterprise.activate') || '激活企业版')">
          <NForm :model="form" label-placement="left" label-width="160px" @submit.prevent="handleActivate">
            <NFormItem :label="$t('enterprise.teamSize') || '团队人数'" required>
              <NInputNumber v-model:value="form.teamSize" :min="5" :max="10000" :step="5" class="w-full" />
            </NFormItem>
            <NFormItem :label="$t('enterprise.tokenQuotaM') || '月度Token额度(百万)'" required>
              <div class="flex items-center gap-2 w-full">
                <NInputNumber v-model:value="form.customTokenQuotaM" :min="1" :max="1000" :step="1" class="flex-1" />
                <span class="text-xs text-secondary whitespace-nowrap">1M ≈ 75万词</span>
              </div>
            </NFormItem>
            <NFormItem :label="$t('enterprise.customPrice') || '自定义价格(元/月)'" required>
              <NInputNumber v-model:value="form.customPrice" :min="99" :max="99999" :step="1" :precision="2" class="w-full" />
            </NFormItem>
            <NFormItem :label="$t('enterprise.months') || '订阅月数'" required>
              <NInputNumber v-model:value="form.months" :min="1" :max="60" :step="1" class="w-full" />
            </NFormItem>
            <NFormItem :label="$t('enterprise.channel') || '支付渠道'">
              <NSelect v-model:value="form.channel" :options="channelOptions" class="w-full" />
            </NFormItem>
            <NFormItem :label="$t('enterprise.orderNo') || '订单号'">
              <NInput v-model:value="form.orderNo" :placeholder="$t('enterprise.orderNoPlaceholder') || '购买时填写的订单号'" />
            </NFormItem>
            <NFormItem label=" ">
              <div class="flex items-center gap-3">
                <span class="text-sm">{{ $t('enterprise.estimatedTotal') || '预估总费用:' }}</span>
                <span class="text-3xl font-bold text-[#58a6ff]">¥{{ estimatedTotal }}</span>
              </div>
            </NFormItem>
            <NFormItem label=" ">
              <NButton type="primary" size="large" attr-type="submit" :loading="submitting">
                {{ config ? ($t('enterprise.updateConfig') || '更新配置') : ($t('enterprise.activate') || '激活企业版') }}
              </NButton>
            </NFormItem>
          </NForm>
        </NCard>
      </NGi>
    </NGrid>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  NCard, NGrid, NGi, NDescriptions, NDescriptionsItem, NTag, NEmpty,
  NForm, NFormItem, NInputNumber, NInput, NSelect, NButton, NSpin,
  useMessage
} from 'naive-ui'
import { fetchActivateEnterprisePlan, fetchUpdateEnterpriseConfig, fetchGetEnterpriseConfig } from '@/service/api'

defineOptions({ name: 'EnterpriseActivate' })
const message = useMessage()

interface EnterpriseConfig {
  tier?: string
  teamSize?: number
  tokenQuota?: number
  monthlyPrice?: number
  expireTime?: string
  status?: string
  message?: string
}

interface ActivateForm {
  teamSize: number
  customTokenQuotaM: number
  customPrice: number
  months: number
  channel: string
  orderNo: string
}

const configLoading = ref(false)
const submitting = ref(false)
const config = ref<EnterpriseConfig | null>(null)

const form = ref<ActivateForm>({
  teamSize: 10,
  customTokenQuotaM: 10,
  customPrice: 999,
  months: 12,
  channel: 'alipay',
  orderNo: ''
})

const channelOptions = [
  { label: '支付宝', value: 'alipay' },
  { label: '微信支付', value: 'wechat' },
  { label: '银行转账', value: 'bank' }
]

const estimatedTotal = computed(() => (form.value.customPrice * form.value.months).toFixed(2))

function formatQuota(val: number | null | undefined): string {
  if (val == null) return '-'
  if (val >= 1000000) return (val / 1000000).toFixed(2) + 'M'
  return String(val)
}

async function loadConfig() {
  configLoading.value = true
  try {
    const { data } = await fetchGetEnterpriseConfig()
    if (data && (data as any).message) {
      config.value = null
    } else {
      config.value = data as any
      if (config.value) {
        form.value.teamSize = config.value.teamSize || 10
        form.value.customTokenQuotaM = config.value.tokenQuota
          ? Math.round(config.value.tokenQuota / 1000000)
          : 10
        form.value.customPrice = config.value.monthlyPrice || 999
      }
    }
  } catch {
    config.value = null
  }
  finally { configLoading.value = false }
}

async function handleActivate() {
  submitting.value = true
  try {
    if (config.value) {
      await fetchUpdateEnterpriseConfig({
        teamSize: form.value.teamSize,
        customTokenQuotaM: form.value.customTokenQuotaM,
        customPrice: form.value.customPrice
      } as any)
      message.success('配置已更新')
    } else {
      await fetchActivateEnterprisePlan(form.value as any)
      message.success('企业版已激活')
    }
    loadConfig()
  } catch {
    message.error('操作失败')
  }
  finally { submitting.value = false }
}

onMounted(loadConfig)
</script>

<style scoped>
.enterprise-page { padding: 0; }

.page-header { margin-bottom: 20px; }

.text-secondary {
  color: #8b949e;
}
</style>
