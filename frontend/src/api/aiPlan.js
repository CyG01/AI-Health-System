import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const BASE_URL = '/api'

export function generatePlan(data) {
  return request({
    url: '/ai-plan/generate',
    method: 'post',
    data
  })
}

/**
 * 流式生成 AI 计划（SSE）
 * 直接使用 fetch 以支持 ReadableStream，但通过统一的拦截器机制处理 token 和 base URL
 */
export function generatePlanStream(data) {
  let onMessage = null
  let onError = null
  let resolvePromise = null
  let rejectPromise = null

  const promise = new Promise((resolve, reject) => {
    resolvePromise = resolve
    rejectPromise = reject
  })

  function doFetch(accessToken) {
    fetch(`${BASE_URL}/ai-plan/generate-stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': accessToken ? `Bearer ${accessToken}` : ''
      },
      body: JSON.stringify(data)
    }).then(response => {
      // 401 时尝试刷新 token 重试
      if (response.status === 401) {
        const userStore = useUserStore()
        const refreshToken = userStore.getRefreshToken()
        if (refreshToken) {
          return fetch(`${BASE_URL}/auth/refresh`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${refreshToken}`
            }
          }).then(refreshRes => {
            if (!refreshRes.ok) throw new Error('Token expired, please login again')
            return refreshRes.json()
          }).then(res => {
            if (res.code === 200 && res.data) {
              userStore.setAuth(res.data)
              return doFetch(res.data.accessToken)
            }
            throw new Error('Token refresh failed')
          })
        }
        throw new Error('Login expired')
      }
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      function processText() {
        reader.read().then(({ done, value }) => {
          if (done) {
            resolvePromise()
            return
          }
          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''

          for (const line of lines) {
            if (line.startsWith('data:')) {
              const text = line.substring(5).trim()
              if (text && onMessage) {
                onMessage(text)
              }
            }
          }
          processText()
        }).catch(err => {
          if (onError) onError(err)
          rejectPromise(err)
        })
      }
      processText()
    }).catch(err => {
      if (onError) onError(err)
      rejectPromise(err)
    })
  }

  setTimeout(() => {
    const userStore = useUserStore()
    doFetch(userStore.getAccessToken())
  }, 0)

  return {
    then: (...args) => promise.then(...args),
    catch: (...args) => promise.catch(...args),
    finally: (...args) => promise.finally(...args),
    set onMessage(fn) { onMessage = fn },
    set onError(fn) { onError = fn }
  }
}

export function getPlanList() {
  return request({
    url: '/ai-plan/list',
    method: 'get'
  })
}

export function getPlanDetail(id) {
  return request({
    url: `/ai-plan/${id}`,
    method: 'get'
  })
}

export function activePlan(id) {
  return request({
    url: `/ai-plan/${id}/active`,
    method: 'put'
  })
}

export function deletePlan(id) {
  return request({
    url: `/ai-plan/${id}`,
    method: 'delete'
  })
}

export function adjustPlan(id) {
  return request({
    url: `/plan-adjust/adjust/${id}`,
    method: 'post'
  })
}
