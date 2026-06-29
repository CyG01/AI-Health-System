import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'

import App from './App.vue'
import router from './router'
import './styles/global.scss'
import { initTheme } from './utils/theme.js'

initTheme()

const app = createApp(App)
const pinia = createPinia()

// 全局错误兜底：捕获未被 ErrorBoundary 拦截的组件渲染异常
app.config.errorHandler = (err, instance, info) => {
  console.error('[GlobalErrorHandler]', err, info)
  // 可选：上报错误监控平台（如 Sentry）
}

app.use(pinia)
app.use(router)
app.use(ElementPlus, { size: 'default', zIndex: 3000 })

app.mount('#app')
