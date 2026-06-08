<template>
  <div ref="chartRoot" class="base-chart" />
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import echarts from '@/utils/echarts'

const props = defineProps({
  option: {
    type: Object,
    required: true
  }
})

const chartRoot = ref(null)
let chartInstance = null

function initChart() {
  if (!chartRoot.value) return
  chartInstance = echarts.init(chartRoot.value, null, { locale: 'ZH' })
  chartInstance.setOption(props.option)
}

function resizeChart() {
  if (chartInstance && !chartInstance.isDisposed()) {
    chartInstance.resize()
  }
}

watch(() => props.option, (newVal) => {
  if (chartInstance && !chartInstance.isDisposed()) {
    chartInstance.setOption(newVal, true)
  }
}, { deep: true })

onMounted(() => {
  initChart()
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  if (chartInstance && !chartInstance.isDisposed()) {
    chartInstance.dispose()
  }
})
</script>

<style scoped lang="scss">
.base-chart {
  width: 100%;
  height: 100%;
  min-height: 0;
}
</style>
