import { useUserStore } from '@/stores/user'

const BASE_URL = '/api'

/**
 * SSE 流式请求客户端
 * 支持 401 Token 刷新重试，统一处理流解析
 *
 * @param {string} url - 请求路径（相对 /api）
 * @param {object} data - 请求体数据
 * @param {function} onMessage - 接收到数据时回调 (chunkText: string) => void
 * @param {function} [onError] - 错误回调 (error: Error) => void
 * @returns {Promise<void>} - 流式传输完成 resolve
 */
export function createSSEStream(url, data, onMessage, onError) {
  let onErrorCb = onError || (() => {})
  let resolvePromise = null
  let rejectPromise = null

  const promise = new Promise((resolve, reject) => {
    resolvePromise = resolve
    rejectPromise = reject
  })

  function doFetch(accessToken) {
    fetch(`${BASE_URL}${url}`, {
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
      if (!response.body) {
        throw new Error('Response body is empty')
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
          if (onErrorCb) onErrorCb(err)
          rejectPromise(err)
        })
      }
      processText()
    }).catch(err => {
      if (onErrorCb) onErrorCb(err)
      rejectPromise(err)
    })
  }

  setTimeout(() => {
    const userStore = useUserStore()
    doFetch(userStore.getAccessToken())
  }, 0)

  return promise
}
