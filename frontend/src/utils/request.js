import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

let isRefreshing = false
const requests = []

function onRefreshed(newAccessToken) {
  requests.forEach((callback) => callback(newAccessToken))
  requests.length = 0
}

request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.getAccessToken()) {
      config.headers.Authorization = `Bearer ${userStore.getAccessToken()}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  async (error) => {
    const userStore = useUserStore()
    const originalRequest = error.config

    if (error.response) {
      const { status, data } = error.response

      if (status === 401 && !originalRequest?.skipAuthRefresh && userStore.getRefreshToken()) {
        if (isRefreshing) {
          return new Promise((resolve) => {
            requests.push((newToken) => {
              originalRequest.headers.Authorization = `Bearer ${newToken}`
              resolve(request(originalRequest))
            })
          })
        }

        isRefreshing = true

        try {
          const newToken = await userStore.refreshAccessToken()
          onRefreshed(newToken)
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          return request(originalRequest)
        } catch (refreshError) {
          userStore.clearAuth()
          router.push('/login')
          ElMessage.error('登录已过期，请重新登录')
          return Promise.reject(refreshError)
        } finally {
          isRefreshing = false
        }
      }

      if (status === 401) {
        userStore.clearAuth()
        router.push('/login')
        ElMessage.error(data?.msg || '未登录或token已过期')
      } else if (status === 403) {
        ElMessage.error(data?.msg || '无权限访问')
      } else if (status === 404) {
        ElMessage.error(data?.msg || '资源不存在')
      } else {
        ElMessage.error(data?.msg || '系统繁忙，请稍后重试')
      }
    } else {
      ElMessage.error('网络异常，请检查连接')
    }
    return Promise.reject(error)
  }
)

export default request
