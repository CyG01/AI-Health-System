<template>
  <div class="empty-state-guide">
    <div class="empty-icon">{{ icon }}</div>
    <div class="empty-title">{{ title }}</div>
    <div class="empty-desc">{{ description }}</div>
    <NButton v-if="actionText" type="primary" size="medium" @click="$emit('action')">
      {{ actionText }}
    </NButton>
  </div>
</template>

<script setup lang="ts">
import { NButton } from 'naive-ui'

withDefaults(defineProps<{
  icon?: string
  title?: string
  description?: string
  actionText?: string
}>(), {
  icon: '📋',
  title: '暂无数据',
  description: '',
  actionText: ''
})

defineEmits<{
  (e: 'action'): void
}>()
</script>

<style scoped lang="scss">
.empty-state-guide {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-3xl) var(--space-xl);
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: var(--space-base);
  opacity: 0.7;
}

.empty-title {
  font-size: var(--text-md);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--space-sm);
}

.empty-desc {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin-bottom: var(--space-lg);
  max-width: 360px;
  line-height: 1.6;
}
</style>
