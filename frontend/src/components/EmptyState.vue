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
  opacity: 0.85;
  animation: empty-bounce 2s ease-in-out infinite;
}

@keyframes empty-bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.empty-title {
  font-size: var(--text-md);
  font-weight: 600;
  color: var(--sport-text-primary);
  margin-bottom: var(--space-sm);
}

.empty-desc {
  font-size: var(--text-sm);
  color: var(--sport-text-secondary);
  margin-bottom: var(--space-lg);
  max-width: 360px;
  line-height: 1.6;
}

.empty-state-guide :deep(.n-button--primary-type) {
  background: var(--gradient-warm);
  border: none;
  border-radius: var(--radius-base);
  font-weight: 600;
  transition: transform 0.2s, box-shadow 0.2s;
}

.empty-state-guide :deep(.n-button--primary-type:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 107, 53, 0.3);
}
</style>
