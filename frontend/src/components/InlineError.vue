<template>
  <div class="inline-error">
    <NIcon :size="24" color="var(--color-danger, #f85149)">
      <WarningIcon />
    </NIcon>
    <span class="error-msg">{{ message }}</span>
    <NButton v-if="retryable" size="small" type="primary" secondary @click="$emit('retry')">
      <template #icon>
        <NIcon :size="14">
          <RefreshIcon />
        </NIcon>
      </template>
      重试
    </NButton>
  </div>
</template>

<script setup lang="ts">
import { NIcon, NButton } from 'naive-ui'
import { Warning as WarningIcon, Refresh as RefreshIcon } from '@vicons/ionicons5'

withDefaults(defineProps<{
  message?: string
  retryable?: boolean
}>(), {
  message: '加载失败，请重试',
  retryable: true
})

defineEmits<{
  (e: 'retry'): void
}>()
</script>

<style scoped lang="scss">
.inline-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-xl);
  border: 1px dashed rgba(248, 81, 73, 0.3);
  border-radius: var(--radius-base);
  background: var(--color-danger-bg, rgba(248, 81, 73, 0.06));
  text-align: center;
}

.error-msg {
  font-size: var(--text-sm);
  color: var(--color-danger, #f85149);
}
</style>
