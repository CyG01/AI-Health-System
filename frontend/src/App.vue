<template>
  <el-config-provider :locale="zhCn">
    <div class="app-container">
      <!-- 全局页面加载进度条 -->
      <div v-if="pageLoading" class="global-progress">
        <div class="progress-bar" />
      </div>
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const pageLoading = computed(() => appStore.pageLoading)
</script>

<style scoped>
.app-container {
  min-height: 100vh;
}
.global-progress {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
  height: 3px;
  background: transparent;
}
.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #58a6ff, #a371f7);
  animation: progress-slide 1.5s ease-in-out infinite;
  width: 40%;
  border-radius: 0 2px 2px 0;
}
@keyframes progress-slide {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(350%); }
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
