const THEME_KEY = 'app-theme'

/**
 * 获取系统主题偏好
 */
function getSystemPreference() {
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

/**
 * 获取当前保存的主题
 */
export function getTheme() {
  return localStorage.getItem(THEME_KEY) || getSystemPreference() || 'dark'
}

/**
 * 设置主题
 * @param {'dark' | 'light'} theme
 */
export function setTheme(theme) {
  localStorage.setItem(THEME_KEY, theme)
  applyTheme(theme)
}

/**
 * 切换主题
 */
export function toggleTheme() {
  const current = document.documentElement.classList.contains('dark') ? 'dark' : 'light'
  setTheme(current === 'dark' ? 'light' : 'dark')
}

/**
 * 应用主题到 DOM
 */
function applyTheme(theme) {
  if (theme === 'dark') {
    document.documentElement.classList.add('dark')
  } else {
    document.documentElement.classList.remove('dark')
  }
}

/**
 * 初始化主题（App.vue 中调用）
 */
export function initTheme() {
  applyTheme(getTheme())

  // 监听系统主题变化
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
    // 仅在用户未手动设置时跟随系统
    if (!localStorage.getItem(THEME_KEY)) {
      applyTheme(e.matches ? 'dark' : 'light')
    }
  })
}