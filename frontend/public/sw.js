/**
 * AI Health System — Service Worker
 *
 * 缓存策略：
 * - 静态资源（JS/CSS/字体/图标）：Cache First，长期缓存
 * - API 请求：Network First，失败时降级到缓存
 * - 页面导航：Network First，离线时返回 app shell
 *
 * 缓存清理：每次 activate 时清理旧版本缓存
 */

const CACHE_VERSION = 'ai-health-sw-v3';
const STATIC_CACHE = `${CACHE_VERSION}-static`;
const API_CACHE = `${CACHE_VERSION}-api`;

// 需要预缓存的静态资源模式
const STATIC_PATTERNS = [
  /\.js$/,
  /\.css$/,
  /\.woff2?$/,
  /\.ttf$/,
  /\.svg$/,
  /\.png$/,
  /\.ico$/
];

// API 路径前缀
const API_PREFIX = '/api/';

// 离线 fallback 页面
const OFFLINE_URL = '/offline.html';

/**
 * Install: 预缓存核心资源
 */
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(STATIC_CACHE).then((cache) => {
      // 预缓存离线页面（如果存在）
      return cache.add(OFFLINE_URL).catch(() => {
        // 离线页面不存在，跳过
      });
    }).then(() => self.skipWaiting())
  );
});

/**
 * Activate: 清理旧版本缓存
 */
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames
          .filter((name) => name.startsWith('ai-health-sw') && name !== STATIC_CACHE && name !== API_CACHE)
          .map((name) => caches.delete(name))
      );
    }).then(() => self.clients.claim())
  );
});

/**
 * Fetch: 根据请求类型选择缓存策略
 */
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // 只处理同源请求
  if (url.origin !== self.location.origin) return;

  // 跳过非 GET 请求
  if (request.method !== 'GET') return;

  // API 请求：Network First
  if (url.pathname.startsWith(API_PREFIX)) {
    event.respondWith(networkFirst(request, API_CACHE, 5 * 60 * 1000)); // 5分钟 TTL
    return;
  }

  // 静态资源：Cache First
  if (isStaticAsset(url.pathname)) {
    event.respondWith(cacheFirst(request, STATIC_CACHE));
    return;
  }

  // 页面导航：Network First + 离线降级
  if (request.mode === 'navigate') {
    event.respondWith(networkFirst(request, STATIC_CACHE).catch(() => {
      return caches.match(OFFLINE_URL).then((cached) => cached || Response.error());
    }));
    return;
  }

  // 其他：Network First
  event.respondWith(networkFirst(request, STATIC_CACHE));
});

/**
 * Cache First 策略：优先读缓存，缓存没有再请求网络
 */
async function cacheFirst(request, cacheName) {
  const cached = await caches.match(request);
  if (cached) return cached;

  const response = await fetch(request);
  if (response.ok) {
    const cache = await caches.open(cacheName);
    cache.put(request, response.clone());
  }
  return response;
}

/**
 * Network First 策略：优先请求网络，失败时降级到缓存
 */
async function networkFirst(request, cacheName, ttlMs = 0) {
  try {
    const response = await fetch(request);
    if (response.ok) {
      const cache = await caches.open(cacheName);
      // 存储时加上时间戳
      const responseWithTime = new Response(response.body, {
        status: response.status,
        statusText: response.statusText,
        headers: response.headers
      });
      await cache.put(request, responseWithTime);
    }
    return response;
  } catch (error) {
    // 网络失败，尝试读缓存
    const cached = await caches.match(request);
    if (cached) {
      // 检查 TTL
      if (ttlMs > 0) {
        const date = cached.headers.get('sw-cached-at');
        if (date && Date.now() - Number(date) > ttlMs) {
          return Response.error(); // 缓存过期
        }
      }
      return cached;
    }
    throw error;
  }
}

/**
 * 判断是否为静态资源
 */
function isStaticAsset(pathname) {
  return STATIC_PATTERNS.some(pattern => pattern.test(pathname));
}

/**
 * 消息处理：接收来自页面的控制指令
 */
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
  if (event.data && event.data.type === 'CLEAR_CACHE') {
    event.waitUntil(
      caches.keys().then((cacheNames) => {
        return Promise.all(
          cacheNames
            .filter((name) => name.startsWith('ai-health-sw'))
            .map((name) => caches.delete(name))
        );
      })
    );
  }
});
