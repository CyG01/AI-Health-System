<template>
  <div class="skeleton-screen">
    <div class="skeleton-header">
      <div class="skeleton-title skeleton-shimmer" />
      <div class="skeleton-subtitle skeleton-shimmer" />
    </div>
    <div class="skeleton-body">
      <div
        v-for="i in rows"
        :key="i"
        class="skeleton-row skeleton-shimmer"
        :style="{ width: rowWidth(i) }"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  rows?: number
  title?: boolean
}>(), {
  rows: 5,
  title: true
})

function rowWidth(idx: number): string {
  const widths = ['100%', '95%', '88%', '92%', '60%', '78%', '85%', '45%']
  return widths[idx % widths.length]
}
</script>

<style scoped lang="scss">
.skeleton-screen {
  padding: var(--space-xl);
  max-width: 1200px;
  margin: 0 auto;
}
.skeleton-header {
  margin-bottom: var(--space-2xl);
}
.skeleton-title {
  height: 24px;
  width: 200px;
  border-radius: var(--radius-sm);
  margin-bottom: var(--space-md);
}
.skeleton-subtitle {
  height: 14px;
  width: 320px;
  border-radius: var(--radius-sm);
}
.skeleton-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-base);
}
.skeleton-row {
  height: 16px;
  border-radius: var(--radius-sm);
}
.skeleton-shimmer {
  background: linear-gradient(90deg, var(--bg-secondary) 25%, var(--bg-elevated) 50%, var(--bg-secondary) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
