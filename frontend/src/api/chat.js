import request from '@/utils/request'
import { createSSEStream } from '@/utils/sseClient'

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
  const promise = createSSEStream('/chat/send', data,
    (text) => { if (onMessage) onMessage(text) },
    (err) => { if (onError) onError(err) }
  )

  return {
    then: (fn) => promise.then(fn),
    catch: (fn) => promise.catch(fn),
    onMessage: (fn) => { onMessage = fn; return { then: (fn2) => promise.then(fn2), catch: (fn2) => promise.catch(fn2), onError: (fn2) => { onError = fn2; return { then: (fn3) => promise.then(fn3), catch: (fn3) => promise.catch(fn3) } } } },
    onError: (fn) => { onError = fn; return { then: (fn2) => promise.then(fn2), catch: (fn2) => promise.catch(fn2) } }
  }
}