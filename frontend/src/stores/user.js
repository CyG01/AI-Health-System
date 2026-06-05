import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { refreshToken as refreshTokenApi, logout as logoutApi } from '@/api/auth'
import { getProfile } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  const isLoggedIn = computed(() => !!accessToken.value)
  const isAdmin = computed(() => userInfo.value?.role === 'admin')

  function setAuth(loginData) {
    accessToken.value = loginData.token
    refreshToken.value = loginData.refreshToken
    userInfo.value = loginData.userInfo
    localStorage.setItem('accessToken', loginData.token)
    localStorage.setItem('refreshToken', loginData.refreshToken)
    localStorage.setItem('userInfo', JSON.stringify(loginData.userInfo))
  }

  function updateUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  function clearAuth() {
    accessToken.value = ''
    refreshToken.value = ''
    userInfo.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')
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
