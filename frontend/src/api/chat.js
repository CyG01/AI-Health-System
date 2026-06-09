import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const BASE_URL = '/api'

export function createSession() {
  return request({ url: '/chat/session/create', method: 'post' })
}

export function getSessionList() {
  return request({ url: '/chat/session/list', method: 'get' })
}

export function getMessages(sessionId) {
  return request({ url: `/chat/session/${sessionId}/messages`, method: 'get' })
}

export function deleteSession(sessionId) {
  return request({ url: `/chat/session/${sessionId}`, method: 'delete' })
}

export function sendMessage(data) {
  let onMessage = null
  let onError = null
  let resolvePromise = null
  let rejectPromise = null

  const promise = new Promise((resolve, reject) => {
    resolvePromise = resolve
    rejectPromise = reject
  })

  function doFetch(accessToken) {
    fetch(`${BASE_URL}/chat/send`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': accessToken ? `Bearer ${accessToken}` : ''
      },
      body: JSON.stringify(data)
    }).then(response => {
      if (response.status === 401) {
        const userStore = useUserStore()
        const refreshToken = userStore.getRefreshToken()
        if (refreshToken) {
          return fetch(`${BASE_URL}/auth/refresh`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${refreshToken}` }
          }).then(refreshRes => {
            if (!refreshRes.ok) throw new Error('Token expired')
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
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      function processText() {
        reader.read().then(({ done, value }) => {
          if (done) { resolvePromise(); return }
          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''
          for (const line of lines) {
            if (line.startsWith('data:')) {
              const text = line.substring(5).trim()
              if (text && onMessage) onMessage(text)
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
    then: (fn) => promise.then(fn),
    catch: (fn) => promise.catch(fn),
    onMessage: (fn) => { onMessage = fn; return { then: (fn2) => promise.then(fn2), catch: (fn2) => promise.catch(fn2), onError: (fn2) => { onError = fn2; return { then: (fn3) => promise.then(fn3), catch: (fn3) => promise.catch(fn3) } } } },
    onError: (fn) => { onError = fn; return { then: (fn2) => promise.then(fn2), catch: (fn2) => promise.catch(fn2) } }
  }
}