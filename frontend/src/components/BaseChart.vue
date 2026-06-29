<template>
  <div v-if="ready" ref="chartRoot" class="base-chart" />
  <div v-else class="base-chart chart-placeholder">
    <div class="chart-skeleton shimmer" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, markRaw } from 'vue'
import type { ECharts, EChartsOption } from 'echarts'

const props = withDefaults(defineProps<{
  option: EChartsOption
  autoResize?: boolean
  height?: string
}>(), {
  autoResize: true,
  height: '100%'
})

const chartRoot = ref<HTMLDivElement | null>(null)
const ready = ref(false)
let chartInstance: ECharts | null = null
let resizeHandler: (() => void) | null = null

async function initChart() {
  if (ready.value || !chartRoot.value) return

  const [{ default: echarts }] = await Promise.all([import('@/utils/echarts')])

  chartInstance = markRaw(echarts.init(chartRoot.value, undefined, { locale: 'ZH' }))
  chartInstance.setOption(props.option, { notMerge: true })

  if (props.autoResize) {
    resizeHandler = () => {
      if (chartInstance && !chartInstance.isDisposed()) {
        chartInstance.resize()
      }
    }
    window.addEventListener('resize', resizeHandler, { passive: true })
  }

  ready.value = true
}

watch(
  () => props.option,
  newVal => {
    if (chartInstance && !chartInstance.isDisposed()) {
      chartInstance.setOption(newVal, { notMerge: true })
    }
  },
  { deep: false }
)

// IntersectionObserver 懒加载：图表进入视口才初始化
let observer: IntersectionObserver | null = null

onMounted(() => {
  if (window.IntersectionObserver) {
    observer = new IntersectionObserver(
      entries => {
        if (entries[0].isIntersecting) {
          initChart()
          observer?.disconnect()
          observer = null
        }
      },
      { rootMargin: '200px' }
    )
    observer.observe(chartRoot.value!)
  } else {
    initChart()
  }
})

onUnmounted(() => {
  observer?.disconnect()
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
  }
  if (chartInstance && !chartInstance.isDisposed()) {
    chartInstance.dispose()
    chartInstance = null
  }
})

/** 暴露刷新方法供父组件调用 */
defineExpose({
  refresh() {
    if (chartInstance && !chartInstance.isDisposed()) {
      chartInstance.resize()
    }
  }
})
</script>

<style scoped lang="scss">
.base-chart {
  width: 100%;
  height: v-bind(height);
  min-height: 0;
}
.chart-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
}
.chart-skeleton {
  width: 80%;
  height: 60%;
  border-radius: 8px;
}
.shimmer {
  background: linear-gradient(90deg, var(--bg-secondary) 25%, var(--bg-elevated) 50%, var(--bg-secondary) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
