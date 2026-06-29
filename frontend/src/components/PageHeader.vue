<script setup lang="ts">
import { NPageHeader, NSpace } from 'naive-ui';

defineOptions({ name: 'PageHeader' });

interface Props {
  title: string;
  subtitle?: string;
  showBack?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  subtitle: undefined,
  showBack: false
});

const emit = defineEmits<{
  (e: 'back'): void;
}>();

function handleBack() {
  emit('back');
}
</script>

<template>
  <div class="flex items-center justify-between mb-16px">
    <div class="flex items-baseline gap-12px">
      <NPageHeader
        :title="props.title"
        :subtitle="props.subtitle"
        :show-back="props.showBack"
        @back="handleBack"
      />
    </div>
    <NSpace>
      <slot name="actions" />
    </NSpace>
  </div>
</template>

<style scoped>
:deep(.n-page-header__title) {
  font-weight: 800;
  color: var(--sport-text-primary);
  position: relative;
}

:deep(.n-page-header__title)::after {
  content: '';
  display: block;
  width: 40px;
  height: 3px;
  background: var(--gradient-warm);
  border-radius: 2px;
  margin-top: 4px;
}

:deep(.n-page-header__subtitle) {
  color: var(--sport-text-secondary);
}
</style>
