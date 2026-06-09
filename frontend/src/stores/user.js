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
  const accessToken = ref(safeGetItem('accessToken'))
  const refreshToken = ref(safeGetItem('refreshToken'))
  const userInfo = ref(safeGetJSON('userInfo'))
  const unreadCount = ref(0)

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
    safeSetItem('accessToken', loginData.accessToken)
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
    return res.data.accessToken
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
    unreadCount,
    getAccessToken,
    getRefreshToken,
    setAuth,
    updateUserInfo,
    clearAuth,
    refreshAccessToken,
    fetchProfile,
    fetchUnreadCount,
    clearUnreadCount,
    logout
  }
})
