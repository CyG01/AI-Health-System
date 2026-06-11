import { useUserStore } from '@/stores/user'

const BASE_URL = '/api'

/** 断点续传重试间隔（毫秒） */
const RESUME_RETRY_DELAY = 2000

/** 最大续传重试次数 */
const MAX_RESUME_RETRIES = 3

/**
 * SSE 流式请求客户端 — 增强版。
 *
 * 新增特性：
 * - cursor 断点续传：断网后自动从上次位置恢复
 * - 打字机动画回调：支持逐字渲染进度回调
 * - 进度提示：每收到数据块后触发 onProgress
 * - AbortController 取消支持
 *
 * @param {string} url - 请求路径（相对 /api）
 * @param {object} data - 请求体数据（含 cursor 字段支持断点续传）
 * @param {object} callbacks - 回调集合
 * @param {function} callbacks.onMessage - 每个增量文本 (text: string) => void
 * @param {function} [callbacks.onProgress] - 进度更新 (receivedChars: number) => void
 * @param {function} [callbacks.onDone] - 流完成
 * @param {function} [callbacks.onError] - 错误 (error: Error) => void
 * @param {function} [callbacks.onResume] - 断点续传中 (cursor: number) => void
 * @returns {{ promise: Promise<void>, abort: () => void }} - 流式控制
 */
export function createSSEStream(url, data, callbacks = {}) {
  const {
    onMessage = () => {},
    onProgress = () => {},
    onDone = () => {},
    onError = () => {},
    onResume = () => {}
  } = callbacks

  const controller = new AbortController()
  let resolvePromise = null
  let rejectPromise = null
  let receivedChars = 0
  let resumeRetries = 0

  const promise = new Promise((resolve, reject) => {
    resolvePromise = resolve
    rejectPromise = reject
  })

  function doFetch(accessToken) {
    const requestData = {
      ...data,
      cursor: receivedChars || undefined
    }

    if (receivedChars > 0) {
      onResume(receivedChars)
    }

    fetch(`${BASE_URL}${url}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': accessToken ? `Bearer ${accessToken}` : ''
      },
      body: JSON.stringify(requestData),
      signal: controller.signal
    }).then(response => {
      if (response.status === 401) {
        return handleTokenRefresh(accessToken)
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

              if (!text) continue

              if (text === '[DONE]') {
                onDone()
                resolvePromise()
                return
              }

              if (text === '[ERROR]') {
                rejectPromise(new Error('AI 服务错误'))
                return
              }

              // 正常增量文本
              receivedChars += text.length
              onMessage(text)
              onProgress(receivedChars)
            }
          }
          processText()
        }).catch(err => {
          handleStreamError(err)
        })
      }

      processText()
    }).catch(err => {
      handleStreamError(err)
    })
  }

  function handleTokenRefresh(oldToken) {
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

  function handleStreamError(err) {
    if (controller.signal.aborted) {
      rejectPromise(new Error('请求已取消'))
      return
    }

    // 网络错误 + 已有部分数据 → 断点续传
    if (!controller.signal.aborted && receivedChars > 0 && resumeRetries < MAX_RESUME_RETRIES) {
      resumeRetries++
      console.warn(`[SSE] 连接中断，${RESUME_RETRY_DELAY}ms 后从 cursor=${receivedChars} 续传 (第${resumeRetries}次)`)
      setTimeout(() => {
        if (!controller.signal.aborted) {
          const userStore = useUserStore()
          doFetch(userStore.getAccessToken())
        }
      }, RESUME_RETRY_DELAY)
      return
    }

    rejectPromise(err)
    onError(err)
  }

  // 立即启动请求
  setTimeout(() => {
    const userStore = useUserStore()
    doFetch(userStore.getAccessToken())
  }, 0)

  return {
    promise,
    get receivedChars() { return receivedChars },
    get resumeRetries() { return resumeRetries },
    abort: () => {
      controller.abort()
    }
  }
}

/**
 * 创建打字机动画版本的 SSE 流。
 * 会将每个增量文本进一步拆分为单个字符发送，实现逐字打印效果。
 *
 * @param {string} url
 * @param {object} data
 * @param {function} onChar - 每个字符回调 (char: string, accumulated: string) => void
 * @param {function} [onDone]
 * @param {function} [onError]
 * @returns {{ promise: Promise<void>, abort: () => void, accumulatedText: string }}
 */
export function createTypewriterStream(url, data, onChar, onDone, onError) {
  let accumulatedText = ''
  const typewriterBuffer = []
  let typing = false

  const stream = createSSEStream(url, data, {
    onMessage: (text) => {
      typewriterBuffer.push(...text.split(''))
      if (!typing) startTyping()
    },
    onDone: () => {
      // 等待所有缓冲字符输出完毕
      const checkDone = setInterval(() => {
        if (typewriterBuffer.length === 0 && !typing) {
          clearInterval(checkDone)
          if (onDone) onDone()
        }
      }, 50)
    },
    onError
  })

  function startTyping() {
    typing = true
    const typeNext = () => {
      if (typewriterBuffer.length === 0) {
        typing = false
        return
      }
      if (stream.abort && typeof stream.abort === 'function') {
        // 已取消
        typing = false
        return
      }
      const char = typewriterBuffer.shift()
      accumulatedText += char
      if (onChar) onChar(char, accumulatedText)
      // 模拟打字速度（30-80ms 随机间隔）
      setTimeout(typeNext, 30 + Math.random() * 50)
    }
    typeNext()
  }

  return {
    promise: stream.promise,
    abort: stream.abort,
    get accumulatedText() { return accumulatedText },
    get receivedChars() { return stream.receivedChars }
  }
}