import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { refreshToken as refreshTokenApi, logout as logoutApi } from '@/api/auth'
import { getProfile } from '@/api/user'
import { getUnreadCount } from '@/api/notification'

function safeGetItem(key, fallback = '') {
  try {
    return localStorage.getItem(key) ?? fallback
  } catch {
    return fallback
  }
}

function safeGetJSON(key, fallback = null) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch {
    return fallback
  }
}

function safeSetItem(key, value) {
  try {
    localStorage.setItem(key, value)
  } catch {
    // 静默失败
  }
}

function safeRemoveItem(key) {
  try {
    localStorage.removeItem(key)
  } catch {
    // 静默失败
  }
}

export const useUserStore = defineStore('user', () => {
  // accessToken 仅存内存（防 XSS 窃取），refreshToken 存 localStorage（页面刷新恢复登录）
  const accessToken = ref('')
  const refreshToken = ref(safeGetItem('refreshToken'))
  const userInfo = ref(safeGetJSON('userInfo'))
  const unreadCount = ref(0)

  // Token 主动刷新定时器
  let refreshTimerId = null
  // accessToken 2小时，提前5分钟刷新 = 115 分钟 = 6900000 ms
  const REFRESH_BEFORE_MS = 115 * 60 * 1000

  const isLoggedIn = computed(() => !!accessToken.value)
  const isAdmin = computed(() => userInfo.value?.role === 'admin')

  function getAccessToken() {
    return accessToken.value
  }

  function getRefreshToken() {
    return refreshToken.value
  }

  function setAuth(loginData) {
    accessToken.value = loginData.accessToken
    refreshToken.value = loginData.refreshToken
    userInfo.value = loginData.userInfo
    safeSetItem('refreshToken', loginData.refreshToken)
    safeSetItem('userInfo', JSON.stringify(loginData.userInfo))
    scheduleTokenRefresh()
  }

  function updateUserInfo(info) {
    userInfo.value = info
    safeSetItem('userInfo', JSON.stringify(info))
  }

  function clearAuth() {
    clearTokenRefreshTimer()
    accessToken.value = ''
    refreshToken.value = ''
    userInfo.value = null
    safeRemoveItem('refreshToken')
    safeRemoveItem('userInfo')
  }

  function clearTokenRefreshTimer() {
    if (refreshTimerId) {
      clearTimeout(refreshTimerId)
      refreshTimerId = null
    }
  }

  function scheduleTokenRefresh() {
    clearTokenRefreshTimer()
    if (!accessToken.value) return
    refreshTimerId = setTimeout(async () => {
      try {
        await refreshAccessToken()
      } catch {
        // 刷新失败，让 401 响应处理器负责跳转
      }
    }, REFRESH_BEFORE_MS)
  }

  async function refreshAccessToken() {
    const res = await refreshTokenApi(refreshToken.value)
    setAuth(res.data)
    return res.data.accessToken
  }

  // 页面刷新后用 refreshToken 恢复会话
  async function initAuth() {
    if (!accessToken.value && refreshToken.value) {
      try {
        const res = await refreshTokenApi(refreshToken.value)
        setAuth(res.data)
      } catch {
        clearAuth()
      }
    }
  }

  async function fetchProfile() {
    const res = await getProfile()
    updateUserInfo(res.data)
    return res.data
  }

  async function fetchUnreadCount() {
    try {
      const res = await getUnreadCount()
      if (res.code === 200) {
        unreadCount.value = res.data || 0
      }
    } catch {
      // 静默失败
    }
  }

  function clearUnreadCount() {
    unreadCount.value = 0
  }

  async function logout() {
    try {
      await logoutApi(refreshToken.value)
    } finally {
      clearAuth()
    }
  }

  return {
    accessToken,
    refreshToken,
    userInfo,
    isLoggedIn,
    isAdmin,
    unreadCount,
    getAccessToken,
    getRefreshToken,
    setAuth,
    updateUserInfo,
    clearAuth,
    refreshAccessToken,
    scheduleTokenRefresh,
    initAuth,
    fetchProfile,
    fetchUnreadCount,
    clearUnreadCount,
    logout
  }
})
