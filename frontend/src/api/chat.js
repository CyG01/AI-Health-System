import request from '@/utils/request'
import { createSSEStream, createTypewriterStream } from '@/utils/sseClient'

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

/**
 * 发送消息（SSE 流式）— 增强版。
 *
 * @param {object} data - { sessionId, content, cursor?, regenerate? }
 * @param {object} callbacks
 * @param {function} callbacks.onMessage - 增量文本回调
 * @param {function} [callbacks.onProgress] - 进度回调 (receivedChars)
 * @param {function} [callbacks.onDone] - 完成回调
 * @param {function} [callbacks.onError] - 错误回调
 * @param {function} [callbacks.onResume] - 断点续传回调 (cursor)
 * @returns {{ promise: Promise<void>, abort: () => void }}
 */
export function sendMessage(data, callbacks = {}) {
  return createSSEStream('/chat/send', data, callbacks)
}

/**
 * 发送消息（打字机动画模式）。
 * onChar 回调会逐字触发，实现逐字渲染效果。
 *
 * @param {object} data
 * @param {function} onChar - (char: string, accumulated: string) => void
 * @param {function} [onDone]
 * @param {function} [onError]
 * @returns {{ promise: Promise<void>, abort: () => void, accumulatedText: string }}
 */
export function sendMessageTypewriter(data, onChar, onDone, onError) {
  return createTypewriterStream('/chat/send', data, onChar, onDone, onError)
}