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

// 请求缓存（GET 请求 5 秒内复用）
const requestCache = new Map()
const CACHE_TTL = 5000

function getCacheKey(config) {
  return `${config.method}:${config.url}:${JSON.stringify(config.params || {})}`
}

function setCache(key, data) {
  requestCache.set(key, { data, timestamp: Date.now() })
}

function getCache(key) {
  const entry = requestCache.get(key)
  if (entry && Date.now() - entry.timestamp < CACHE_TTL) {
    return entry.data
  }
  requestCache.delete(key)
  return null
}

// 请求去重：相同请求进行中时复用 Promise
const pendingRequests = new Map()

function getPendingKey(config) {
  const { method, url, params, data } = config
  return `${method}:${url}:${JSON.stringify(params || {})}:${JSON.stringify(data || {})}`
}

request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.getAccessToken()) {
      config.headers.Authorization = `Bearer ${userStore.getAccessToken()}`
    }

    // GET 请求检查缓存
    if (config.method === 'get' && config.cache !== false) {
      const cacheKey = getCacheKey(config)
      const cached = getCache(cacheKey)
      if (cached) {
        config._cached = true
        config._cachedData = cached
        // 直接返回缓存数据
        return Promise.resolve({ ...config, adapter: null })
      }
      config._cacheKey = cacheKey
    }

    // 请求去重：相同请求进行中时，复用第一个请求的 Promise
    if (config.deduplicate !== false) {
      const pendingKey = getPendingKey(config)
      if (pendingRequests.has(pendingKey)) {
        const pendingPromise = pendingRequests.get(pendingKey)
        // 返回 pending promise，避免重复请求
        return new Promise((resolve) => {
          pendingPromise.then(resolve).catch(() => {
            // pending 失败则放行当前请求
            pendingRequests.delete(pendingKey)
            resolve(config)
          })
        })
      }
      config._pendingKey = pendingKey
    }

    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    // 处理缓存的响应（没有 data 的情况说明走了缓存）
    if (response.config?._cached) {
      return response.config._cachedData
    }

    // 清理去重标记
    if (response.config?._pendingKey) {
      pendingRequests.delete(response.config._pendingKey)
    }

    const res = response.data

    // GET 请求存入缓存
    if (response.config?.method === 'get' && response.config?._cacheKey) {
      setCache(response.config._cacheKey, res)
    }

    if (res.code === 200) {
      return res
    }

    // 业务错误不弹窗，让调用方自行处理
    if (response.config?.silent) {
      return Promise.reject(new Error(res.msg || '请求失败'))
    }

    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  async (error) => {
    const userStore = useUserStore()
    const originalRequest = error.config

    if (!originalRequest) {
      return Promise.reject(error)
    }

    // 清理去重标记
    if (originalRequest._pendingKey) {
      pendingRequests.delete(originalRequest._pendingKey)
    }

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
        } catch {
          userStore.clearAuth()
          router.push('/login')
          ElMessage.error('登录已过期，请重新登录')
          return Promise.reject(new Error('Token refresh failed'))
        } finally {
          isRefreshing = false
        }
      }

      if (status === 401) {
        userStore.clearAuth()
        router.push('/login')
        ElMessage.error(data?.msg || '未登录或token已过期')
      } else if (!originalRequest?.silent) {
        if (status === 403) ElMessage.error(data?.msg || '无权限访问')
        else if (status === 404) ElMessage.error(data?.msg || '资源不存在')
        else ElMessage.error(data?.msg || '系统繁忙，请稍后重试')
      }
    } else if (!originalRequest?.silent) {
      ElMessage.error('网络异常，请检查连接')
    }
    return Promise.reject(error)
  }
)

export default request