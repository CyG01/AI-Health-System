import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { refreshToken as refreshTokenApi, logout as logoutApi } from '@/api/auth'
import { getProfile } from '@/api/user'

/**
 * 安全读取 localStorage — 带 try-catch 防护
 * 防止隐私模式 / storage 满 / 反序列化异常导致白屏
 */
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
    // 静默失败 — storage 满或隐私模式禁止写入
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
  const accessToken = ref(safeGetItem('accessToken'))
  const refreshToken = ref(safeGetItem('refreshToken'))
  const userInfo = ref(safeGetJSON('userInfo'))

  const isLoggedIn = computed(() => !!accessToken.value)
  const isAdmin = computed(() => userInfo.value?.role === 'admin')

  function setAuth(loginData) {
    accessToken.value = loginData.token
    refreshToken.value = loginData.refreshToken
    userInfo.value = loginData.userInfo
    safeSetItem('accessToken', loginData.token)
    safeSetItem('refreshToken', loginData.refreshToken)
    safeSetItem('userInfo', JSON.stringify(loginData.userInfo))
  }

  function updateUserInfo(info) {
    userInfo.value = info
    safeSetItem('userInfo', JSON.stringify(info))
  }

  function clearAuth() {
    accessToken.value = ''
    refreshToken.value = ''
    userInfo.value = null
    safeRemoveItem('accessToken')
    safeRemoveItem('refreshToken')
    safeRemoveItem('userInfo')
  }

  async function refreshAccessToken() {
    const res = await refreshTokenApi(refreshToken.value)
    setAuth(res.data)
    return res.data.token
  }

  async function fetchProfile() {
    const res = await getProfile()
    updateUserInfo(res.data)
    return res.data
  }

  async function logout() {
    try {
      await logoutApi()
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
    setAuth,
    updateUserInfo,
    clearAuth,
    refreshAccessToken,
    fetchProfile,
    logout
  }
})
