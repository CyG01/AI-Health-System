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
