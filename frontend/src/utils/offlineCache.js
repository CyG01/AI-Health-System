/**
 * 前端离线缓存管理器 — 基于 IndexedDB。
 * 断网时展示本地缓存数据，联网后自动同步。
 *
 * 数据库结构：
 *   ai-health-cache / chat_cache     — 聊天消息缓存
 *   ai-health-cache / session_cache  — 会话列表缓存
 *   ai-health-cache / sdui_cache     — SDUI 响应缓存
 *   ai-health-cache / api_cache      — 通用 API 响应缓存
 */

const DB_NAME = 'ai-health-cache'
const DB_VERSION = 1

/** 缓存有效期（毫秒） */
const TTL = {
  chat: 7 * 24 * 60 * 60 * 1000,    // 聊天记录 7 天
  session: 30 * 24 * 60 * 60 * 1000, // 会话列表 30 天
  sdui: 24 * 60 * 60 * 1000,         // SDUI 响应 1 天
  api: 5 * 60 * 1000                 // 通用 API 5 分钟
}

let db = null

/**
 * 打开/初始化数据库。
 */
function openDB() {
  return new Promise((resolve, reject) => {
    if (db) return resolve(db)

    const request = indexedDB.open(DB_NAME, DB_VERSION)

    request.onupgradeneeded = (event) => {
      const database = event.target.result

      // 聊天消息缓存
      if (!database.objectStoreNames.contains('chat_cache')) {
        const store = database.createObjectStore('chat_cache', { keyPath: 'id' })
        store.createIndex('sessionId', 'sessionId', { unique: false })
        store.createIndex('timestamp', 'timestamp', { unique: false })
      }

      // 会话列表缓存
      if (!database.objectStoreNames.contains('session_cache')) {
        database.createObjectStore('session_cache', { keyPath: 'id' })
      }

      // SDUI 响应缓存
      if (!database.objectStoreNames.contains('sdui_cache')) {
        const store = database.createObjectStore('sdui_cache', { keyPath: 'key' })
        store.createIndex('timestamp', 'timestamp', { unique: false })
      }

      // 通用 API 响应缓存
      if (!database.objectStoreNames.contains('api_cache')) {
        const store = database.createObjectStore('api_cache', { keyPath: 'key' })
        store.createIndex('timestamp', 'timestamp', { unique: false })
      }
    }

    request.onsuccess = (event) => {
      db = event.target.result
      resolve(db)
    }

    request.onerror = (event) => {
      console.error('[OfflineCache] 数据库打开失败', event.target.error)
      reject(event.target.error)
    }
  })
}

/**
 * 通用存储操作。
 */
async function put(storeName, data) {
  const database = await openDB()
  return new Promise((resolve, reject) => {
    const tx = database.transaction(storeName, 'readwrite')
    const store = tx.objectStore(storeName)
    const request = store.put(data)
    request.onsuccess = () => resolve()
    request.onerror = () => reject(request.error)
  })
}

async function getAll(storeName) {
  const database = await openDB()
  return new Promise((resolve, reject) => {
    const tx = database.transaction(storeName, 'readonly')
    const store = tx.objectStore(storeName)
    const request = store.getAll()
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

async function getByIndex(storeName, indexName, value) {
  const database = await openDB()
  return new Promise((resolve, reject) => {
    const tx = database.transaction(storeName, 'readonly')
    const store = tx.objectStore(storeName)
    const index = store.index(indexName)
    const request = index.getAll(value)
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

async function remove(storeName, key) {
  const database = await openDB()
  return new Promise((resolve, reject) => {
    const tx = database.transaction(storeName, 'readwrite')
    const store = tx.objectStore(storeName)
    const request = store.delete(key)
    request.onsuccess = () => resolve()
    request.onerror = () => reject(request.error)
  })
}

async function clearStore(storeName) {
  const database = await openDB()
  return new Promise((resolve, reject) => {
    const tx = database.transaction(storeName, 'readwrite')
    const store = tx.objectStore(storeName)
    const request = store.clear()
    request.onsuccess = () => resolve()
    request.onerror = () => reject(request.error)
  })
}

// ===================== 公开 API =====================

/**
 * 缓存聊天消息（按会话分组）。
 */
export async function cacheChatMessages(sessionId, messages) {
  const timestamp = Date.now()
  const items = messages.map((msg, idx) => ({
    ...msg,
    id: `chat_${sessionId}_${idx}`,
    sessionId,
    timestamp: msg.timestamp || timestamp
  }))
  for (const item of items) {
    await put('chat_cache', item).catch(() => {})
  }
}

/**
 * 获取本地缓存的聊天消息。
 */
export async function getCachedChatMessages(sessionId) {
  try {
    const messages = await getByIndex('chat_cache', 'sessionId', sessionId)
    return messages.sort((a, b) => (a.timestamp || 0) - (b.timestamp || 0))
  } catch {
    return []
  }
}

/**
 * 缓存会话列表。
 */
export async function cacheSessionList(sessions) {
  for (const session of sessions) {
    await put('session_cache', { ...session, timestamp: Date.now() }).catch(() => {})
  }
}

/**
 * 获取本地缓存的会话列表。
 */
export async function getCachedSessionList() {
  try {
    const sessions = await getAll('session_cache')
    return sessions.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0))
  } catch {
    return []
  }
}

/**
 * 缓存 SDUI 响应（按请求 key）。
 */
export async function cacheSduiResponse(key, response) {
  await put('sdui_cache', {
    key,
    response,
    timestamp: Date.now()
  })
}

/**
 * 获取本地缓存的 SDUI 响应。
 */
export async function getCachedSduiResponse(key) {
  try {
    const database = await openDB()
    return new Promise((resolve) => {
      const tx = database.transaction('sdui_cache', 'readonly')
      const store = tx.objectStore('sdui_cache')
      const request = store.get(key)
      request.onsuccess = () => {
        const cached = request.result
        if (cached && (Date.now() - cached.timestamp) < TTL.sdui) {
          resolve(cached.response)
        } else {
          resolve(null)
        }
      }
      request.onerror = () => resolve(null)
    })
  } catch {
    return null
  }
}

/**
 * 通用 API 缓存（GET 请求）。
 */
export async function cacheApiResponse(key, response) {
  await put('api_cache', {
    key,
    response,
    timestamp: Date.now()
  })
}

/**
 * 获取通用 API 缓存。
 */
export async function getCachedApiResponse(key, ttlMs = TTL.api) {
  try {
    const database = await openDB()
    return new Promise((resolve) => {
      const tx = database.transaction('api_cache', 'readonly')
      const store = tx.objectStore('api_cache')
      const request = store.get(key)
      request.onsuccess = () => {
        const cached = request.result
        if (cached && (Date.now() - cached.timestamp) < ttlMs) {
          resolve(cached.response)
        } else {
          resolve(null)
        }
      }
      request.onerror = () => resolve(null)
    })
  } catch {
    return null
  }
}

/**
 * 删除指定会话的缓存。
 */
export async function removeSessionCache(sessionId) {
  const messages = await getByIndex('chat_cache', 'sessionId', sessionId)
  for (const msg of messages) {
    await remove('chat_cache', msg.id).catch(() => {})
  }
  await remove('session_cache', sessionId).catch(() => {})
}

/**
 * 清理过期缓存。
 */
export async function cleanExpiredCache() {
  try {
    const database = await openDB()
    const now = Date.now()

    const cleanStore = (storeName, ttl) => {
      return new Promise((resolve) => {
        const tx = database.transaction(storeName, 'readwrite')
        const store = tx.objectStore(storeName)
        const request = store.getAll()
        request.onsuccess = () => {
          const items = request.result
          for (const item of items) {
            if (now - (item.timestamp || 0) > ttl) {
              store.delete(item.id || item.key)
            }
          }
          resolve()
        }
        request.onerror = () => resolve()
      })
    }

    await cleanStore('chat_cache', TTL.chat)
    await cleanStore('sdui_cache', TTL.sdui)
    await cleanStore('api_cache', TTL.api)
  } catch (e) {
    console.warn('[OfflineCache] 清理过期缓存失败', e)
  }
}

/**
 * 检测网络状态，决定是否使用缓存。
 */
export function isOnline() {
  return navigator.onLine
}

/**
 * 带离线降级的请求包装器。
 * 在线 → 正常请求，缓存结果
 * 离线 → 返回缓存数据
 */
export async function fetchWithCache(key, fetchFn, options = {}) {
  const { ttlMs = TTL.api, cacheType = 'api' } = options

  if (isOnline()) {
    try {
      const response = await fetchFn()
      // 缓存成功响应
      const cacheFn = cacheType === 'sdui' ? cacheSduiResponse : cacheApiResponse
      await cacheFn(key, response)
      return response
    } catch (e) {
      // 网络错误，尝试降级到缓存
      console.warn('[OfflineCache] 请求失败，降级到缓存', e)
      const getFn = cacheType === 'sdui' ? getCachedSduiResponse : getCachedApiResponse
      const cached = await getFn(key, ttlMs)
      if (cached) return cached
      throw e
    }
  } else {
    // 离线：直接返回缓存
    const getFn = cacheType === 'sdui' ? getCachedSduiResponse : getCachedApiResponse
    const cached = await getFn(key, ttlMs)
    if (cached) return cached
    throw new Error('离线模式，且无本地缓存数据')
  }
}

export default {
  cacheChatMessages,
  getCachedChatMessages,
  cacheSessionList,
  getCachedSessionList,
  cacheSduiResponse,
  getCachedSduiResponse,
  cacheApiResponse,
  getCachedApiResponse,
  removeSessionCache,
  cleanExpiredCache,
  fetchWithCache,
  isOnline
}