<script setup lang="ts">
import { ref } from 'vue';
import { NButton, NPopconfirm } from 'naive-ui';

defineOptions({ name: 'ConfirmButton' });

interface Props {
  /** Button label text */
  label: string;
  /** Confirmation message */
  confirmMessage: string;
  /** Button type (primary, error, success, warning, info, default, tertiary) */
  type?: 'primary' | 'error' | 'success' | 'warning' | 'info' | 'default' | 'tertiary';
  /** Button size */
  size?: 'tiny' | 'small' | 'medium' | 'large';
  /** Whether to show as text button (no border) */
  text?: boolean;
  /** Whether the button is disabled */
  disabled?: boolean;
  /** Whether to show loading state while async callback runs */
  asyncLoading?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  type: 'default',
  size: 'small',
  text: true,
  disabled: false,
  asyncLoading: false
});

const emit = defineEmits<{
  (e: 'confirm'): void | Promise<void>;
}>();

const loading = ref(false);

async function handleConfirm() {
  if (!props.asyncLoading) {
    emit('confirm');
    return;
  }
  loading.value = true;
  try {
    await emit('confirm');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <NPopconfirm @positive-click="handleConfirm">
    <template #trigger>
      <NButton
        :type="props.type"
        :size="props.size"
        :text="props.text"
        :disabled="props.disabled"
        :loading="loading"
      >
        {{ props.label }}
      </NButton>
    </template>
    {{ props.confirmMessage }}
  </NPopconfirm>
</template>
