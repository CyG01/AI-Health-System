<template>
  <div class="skeleton-screen">
    <div class="skeleton-header">
      <div class="skeleton-title skeleton-shimmer" />
      <div class="skeleton-subtitle skeleton-shimmer" />
    </div>
    <div class="skeleton-body">
      <div v-for="i in rows" :key="i" class="skeleton-row skeleton-shimmer" :style="{ width: rowWidth(i) }" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  rows: { type: Number, default: 5 },
  title: { type: Boolean, default: true }
})

function rowWidth(idx) {
  const widths = ['100%', '95%', '88%', '92%', '60%', '78%', '85%', '45%']
  return widths[idx % widths.length]
}
</script>

<style scoped lang="scss">
.skeleton-screen {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}
.skeleton-header {
  margin-bottom: 32px;
}
.skeleton-title {
  height: 24px;
  width: 200px;
  border-radius: 6px;
  margin-bottom: 12px;
}
.skeleton-subtitle {
  height: 14px;
  width: 320px;
  border-radius: 4px;
}
.skeleton-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.skeleton-row {
  height: 16px;
  border-radius: 4px;
}
.skeleton-shimmer {
  background: linear-gradient(90deg, #161b22 25%, #1c2333 50%, #161b22 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>