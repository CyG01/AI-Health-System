<template>
  <div class="medical-disclaimer-banner">
    <el-alert
      :title="title"
      type="warning"
      :closable="false"
      show-icon
    >
      <template #default>
        <p>{{ content }}</p>
      </template>
    </el-alert>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: '医疗免责声明'
  },
  content: {
    type: String,
    default: '本内容由AI生成，仅供参考，不构成医疗诊断或处方。如有健康问题，请及时咨询专业医生。'
  }
})
</script>

<style scoped>
.medical-disclaimer-banner {
  margin-bottom: 16px;
}

.medical-disclaimer-banner :deep(.el-alert) {
  border-radius: 8px;
  border-left: 4px solid #d29922;
  background: rgba(210, 153, 34, 0.08);
}

.medical-disclaimer-banner :deep(.el-alert__title) {
  font-weight: 700;
  font-size: 13px;
  color: #d29922;
}

.medical-disclaimer-banner :deep(.el-alert p) {
  margin: 4px 0 0 0;
  font-size: 12px;
  color: #8b949e;
  line-height: 1.6;
}
</style>