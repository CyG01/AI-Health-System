<template>
  <div class="medical-disclaimer-banner">
    <NAlert :title="title" type="warning" :bordered="false">
      {{ content }}
    </NAlert>
  </div>
</template>

<script setup lang="ts">
import { NAlert } from 'naive-ui'

withDefaults(defineProps<{
  title?: string
  content?: string
}>(), {
  title: '医疗免责声明',
  content: '本内容由AI生成，仅供参考，不构成医疗诊断或处方。如有健康问题，请及时咨询专业医生。'
})
</script>

<style scoped lang="scss">
.medical-disclaimer-banner {
  margin-bottom: 16px;
}

.medical-disclaimer-banner :deep(.n-alert) {
  border-radius: 8px;
  border-left: 4px solid #d29922;
  background: rgba(210, 153, 34, 0.08);
}

.medical-disclaimer-banner :deep(.n-alert__header .n-alert__title) {
  font-weight: 700;
  font-size: 13px;
  color: #d29922;
}

.medical-disclaimer-banner :deep(.n-alert__content) {
  font-size: 12px;
  color: #8b949e;
  line-height: 1.6;
}
</style>
