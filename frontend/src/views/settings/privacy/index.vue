<template>
  <div class="privacy-page">
    <div class="page-header">
      <h2 class="text-xl font-semibold">隐私授权设置</h2>
      <p class="text-sm text-secondary">管理您的数据授权偏好，控制数据如何被使用</p>
    </div>

    <NCard class="section-card">
      <NSpin :show="loading">
        <NForm label-placement="left" label-width="160px" @submit.prevent="handleSave">
          <NDivider>数据授权开关</NDivider>

          <NFormItem label="数据用于模型训练">
            <NSwitch
              v-model:value="form.dataConsentForModel"
              :checked-value="1"
              :unchecked-value="0"
            />
            <span class="form-hint">允许将您的匿名化数据用于改进 AI 模型</span>
          </NFormItem>

          <NFormItem label="数据用于个性化推荐">
            <NSwitch
              v-model:value="form.dataConsentForRecommend"
              :checked-value="1"
              :unchecked-value="0"
            />
            <span class="form-hint">允许基于您的数据提供个性化健康推荐</span>
          </NFormItem>

          <NDivider>授权信息</NDivider>

          <NDescriptions label-placement="left" bordered :column="1" size="small">
            <NDescriptionsItem label="模型训练授权">
              <NTag :type="form.dataConsentForModel === 1 ? 'success' : 'warning'" size="small">
                {{ form.dataConsentForModel === 1 ? '已授权' : '未授权' }}
              </NTag>
            </NDescriptionsItem>
            <NDescriptionsItem label="个性化推荐授权">
              <NTag :type="form.dataConsentForRecommend === 1 ? 'success' : 'warning'" size="small">
                {{ form.dataConsentForRecommend === 1 ? '已授权' : '未授权' }}
              </NTag>
            </NDescriptionsItem>
          </NDescriptions>

          <div class="mt-6">
            <NFormItem>
              <div class="flex gap-2.5">
                <NButton type="primary" attr-type="submit" :loading="saving">保存设置</NButton>
                <NButton @click="loadConsent" :loading="loading">重置</NButton>
              </div>
            </NFormItem>
          </div>
        </NForm>
      </NSpin>
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  NCard, NForm, NFormItem, NSwitch, NDivider,
  NButton, NSpin, NTag, NDescriptions, NDescriptionsItem,
  useMessage
} from 'naive-ui'
import { fetchGetPrivacyConsent, fetchUpdatePrivacyConsent } from '@/service/api'

defineOptions({ name: 'PrivacySettings' })
const message = useMessage()

const loading = ref(false)
const saving = ref(false)
const form = reactive({
  dataConsentForModel: 0,
  dataConsentForRecommend: 0
})

async function loadConsent() {
  loading.value = true
  try {
    const { data } = await fetchGetPrivacyConsent()
    if (data) {
      form.dataConsentForModel = data.dataConsentForModel ?? 0
      form.dataConsentForRecommend = data.dataConsentForRecommend ?? 0
    }
  } catch { /* use defaults */ }
  finally { loading.value = false }
}

async function handleSave() {
  saving.value = true
  try {
    await fetchUpdatePrivacyConsent({
      dataConsentForModel: form.dataConsentForModel,
      dataConsentForRecommend: form.dataConsentForRecommend
    })
    message.success('隐私授权设置已保存')
  } catch {
    message.error('保存失败')
  }
  finally { saving.value = false }
}

onMounted(loadConsent)
</script>

<style scoped>
.privacy-page { padding: 0; }

.page-header { margin-bottom: 20px; }

.section-card { padding: 4px; }

.form-hint {
  margin-left: 10px;
  color: #8b949e;
  font-size: 13px;
}

.text-secondary {
  color: #8b949e;
}
</style>
