import { createApp } from 'vue'
import './plugins/assets'
import { setupAppVersionNotification, setupDayjs, setupIconifyOffline, setupLoading, setupNProgress } from './plugins'
import { setupStore } from './store'
import { setupRouter } from './router'
import { getLocale, setupI18n } from './locales'
import App from './App.vue'

async function setupApp() {
  setupLoading()

  setupNProgress()

  setupIconifyOffline()

  setupDayjs()

  const app = createApp(App)

  setupStore(app)

  try {
    // Safety timeout: if router setup takes longer than 15s (e.g., backend
    // unreachable causing hanging API calls in route guard), show an error
    // page instead of leaving the loading overlay stuck indefinitely.
    const timeout = new Promise<never>((_, reject) =>
      setTimeout(() => reject(new Error('Router initialization timed out after 15s')), 15_000)
    );
    await Promise.race([setupRouter(app), timeout]);
  } catch (error) {
    console.error('[App] Router setup failed:', error);
    // Clear the loading spinner so the user doesn't see a stuck screen
    const appEl = document.getElementById('app');
    if (appEl) {
      appEl.innerHTML = `
        <div style="display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;background:#0d1117;color:#e6edf3;font-family:sans-serif">
          <h2 style="margin-bottom:12px">应用初始化失败</h2>
          <p style="color:#8b949e;margin-bottom:24px">路由加载出错，请确认后端服务已启动后刷新重试</p>
          <button onclick="location.reload()" style="padding:8px 24px;border-radius:6px;border:1px solid #30363d;background:#21262d;color:#58a6ff;cursor:pointer;font-size:14px">
            刷新页面
          </button>
        </div>`;
    }
    return;
  }

  setupI18n(app)

  setupAppVersionNotification()

  app.mount('#app')
}

setupApp()
