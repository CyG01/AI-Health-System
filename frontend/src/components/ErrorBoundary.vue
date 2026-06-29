<template>
  <div v-if="error" class="error-boundary">
    <div class="error-boundary-content">
      <NIcon :size="32" color="#f85149">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
      </NIcon>
      <p class="error-title">{{ fallbackTitle }}</p>
      <p class="error-detail">{{ fallbackMessage }}</p>
      <NButton size="small" type="primary" secondary @click="handleReset">
        重试
      </NButton>
    </div>
  </div>
  <slot v-else />
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { NIcon, NButton } from 'naive-ui';

defineOptions({ name: 'ErrorBoundary' });

interface Props {
  fallbackTitle?: string;
  fallbackMessage?: string;
  onError?: ((err: Error, info: string) => void) | null;
}

const props = withDefaults(defineProps<Props>(), {
  fallbackTitle: '组件渲染异常',
  fallbackMessage: '该区域加载出错，请点击重试',
  onError: null
});

const error = ref<Error | null>(null);

// Note: onErrorCaptured is imported from vue
import { onErrorCaptured } from 'vue';

onErrorCaptured((err: Error, _instance, info: string) => {
  error.value = err;
  if (props.onError) {
    props.onError(err, info);
  }
  // 阻止错误继续向上传播
  return false;
});

function handleReset() {
  error.value = null;
}

defineExpose({ reset: handleReset });
</script>

<style scoped>
.error-boundary {
  padding: 16px;
  border: 1px dashed rgba(248, 81, 73, 0.3);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60px;
}

.error-boundary-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  text-align: center;
}

.error-title {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: #f85149;
}

.error-detail {
  margin: 0;
  font-size: 12px;
  opacity: 0.7;
}
</style>
