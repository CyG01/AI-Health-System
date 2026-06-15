import { localStg } from '@/utils/storage';

const BASE_URL = '/proxy-default';

/** 断点续传重试间隔（毫秒） */
const RESUME_RETRY_DELAY = 2000;

/** 最大续传重试次数 */
const MAX_RESUME_RETRIES = 3;

interface SSECallbacks {
  onMessage?: (text: string) => void;
  onProgress?: (receivedChars: number) => void;
  onDone?: () => void;
  onError?: (error: Error) => void;
  onResume?: (cursor: number) => void;
}

interface SSEStreamControl {
  promise: Promise<void>;
  readonly receivedChars: number;
  readonly resumeRetries: number;
  abort: () => void;
}

interface SSEStreamData {
  [key: string]: any;
  cursor?: number;
}

/**
 * SSE 流式请求客户端 — TypeScript 增强版。
 *
 * 特性：
 * - cursor 断点续传：断网后自动从上次位置恢复
 * - 打字机动画回调：支持逐字渲染进度回调
 * - 进度提示：每收到数据块后触发 onProgress
 * - AbortController 取消支持
 */
export function createSSEStream(url: string, data: SSEStreamData, callbacks: SSECallbacks = {}): SSEStreamControl {
  const {
    onMessage = () => {},
    onProgress = () => {},
    onDone = () => {},
    onError = () => {},
    onResume = () => {}
  } = callbacks;

  const controller = new AbortController();
  let resolvePromise: (() => void) | null = null;
  let rejectPromise: ((reason?: any) => void) | null = null;
  let receivedChars = 0;
  let resumeRetries = 0;

  const promise = new Promise<void>((resolve, reject) => {
    resolvePromise = resolve;
    rejectPromise = reject;
  });

  function getAccessToken(): string {
    return localStg.get('token') || '';
  }

  function getRefreshToken(): string {
    return localStg.get('refreshToken') || '';
  }

  function doFetch(accessToken: string) {
    const requestData: SSEStreamData = {
      ...data,
      cursor: receivedChars || undefined
    };

    if (receivedChars > 0) {
      onResume(receivedChars);
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
        return handleTokenRefresh(accessToken);
      }
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      if (!response.body) {
        throw new Error('Response body is empty');
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      function processText() {
        reader.read().then(({ done, value }) => {
          if (done) {
            resolvePromise?.();
            return;
          }

          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n');
          buffer = lines.pop() || '';

          for (const line of lines) {
            if (line.startsWith('data:')) {
              const text = line.substring(5).trim();

              if (!text) continue;

              if (text === '[DONE]') {
                onMessage('[DONE]');
                onDone();
                resolvePromise?.();
                return;
              }

              if (text === '[ERROR]') {
                onMessage('[ERROR]');
                const error = new Error('AI 服务错误');
                onError(error);
                rejectPromise?.(error);
                return;
              }

              receivedChars += text.length;
              onMessage(text);
              onProgress(receivedChars);
            }
          }
          processText();
        }).catch(err => {
          handleStreamError(err);
        });
      }

      processText();
    }).catch(err => {
      handleStreamError(err);
    });
  }

  function handleTokenRefresh(_oldToken: string) {
    const refreshToken = getRefreshToken();
    if (refreshToken) {
      return fetch(`${BASE_URL}/auth/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Refresh-Token': `Bearer ${refreshToken}`
        }
      }).then(refreshRes => {
        if (!refreshRes.ok) throw new Error('Token expired, please login again');
        return refreshRes.json();
      }).then(res => {
        if (res.code === 200 && res.data) {
          localStg.set('token', res.data.accessToken);
          if (res.data.refreshToken) {
            localStg.set('refreshToken', res.data.refreshToken);
          }
          return doFetch(res.data.accessToken);
        }
        throw new Error('Token refresh failed');
      });
    }
    throw new Error('Login expired');
  }

  function handleStreamError(err: Error) {
    if (controller.signal.aborted) {
      rejectPromise?.(new Error('请求已取消'));
      return;
    }

    // 网络错误 + 已有部分数据 → 断点续传
    if (!controller.signal.aborted && receivedChars > 0 && resumeRetries < MAX_RESUME_RETRIES) {
      resumeRetries++;
      console.warn(`[SSE] 连接中断，${RESUME_RETRY_DELAY}ms 后从 cursor=${receivedChars} 续传 (第${resumeRetries}次)`);
      setTimeout(() => {
        if (!controller.signal.aborted) {
          doFetch(getAccessToken());
        }
      }, RESUME_RETRY_DELAY);
      return;
    }

    rejectPromise?.(err);
    onError(err);
  }

  // 立即启动请求
  setTimeout(() => {
    doFetch(getAccessToken());
  }, 0);

  return {
    promise,
    get receivedChars() { return receivedChars; },
    get resumeRetries() { return resumeRetries; },
    abort: () => {
      controller.abort();
    }
  };
}

interface TypewriterStreamControl {
  promise: Promise<void>;
  abort: () => void;
  readonly accumulatedText: string;
  readonly receivedChars: number;
}

/**
 * 创建打字机动画版本的 SSE 流。
 * 会将每个增量文本进一步拆分为单个字符发送，实现逐字打印效果。
 */
export function createTypewriterStream(
  url: string,
  data: SSEStreamData,
  onChar?: (char: string, accumulated: string) => void,
  onDone?: () => void,
  onError?: (error: Error) => void
): TypewriterStreamControl {
  let accumulatedText = '';
  const typewriterBuffer: string[] = [];
  let typing = false;

  const stream = createSSEStream(url, data, {
    onMessage: (text: string) => {
      typewriterBuffer.push(...text.split(''));
      if (!typing) startTyping();
    },
    onDone: () => {
      const checkDone = setInterval(() => {
        if (typewriterBuffer.length === 0 && !typing) {
          clearInterval(checkDone);
          if (onDone) onDone();
        }
      }, 50);
    },
    onError
  });

  function startTyping() {
    typing = true;
    const typeNext = () => {
      if (typewriterBuffer.length === 0) {
        typing = false;
        return;
      }
      const char = typewriterBuffer.shift();
      if (char) {
        accumulatedText += char;
        if (onChar) onChar(char, accumulatedText);
      }
      // 模拟打字速度（30-80ms 随机间隔）
      setTimeout(typeNext, 30 + Math.random() * 50);
    };
    typeNext();
  }

  return {
    promise: stream.promise,
    abort: () => {
      stream.abort();
    },
    get accumulatedText() { return accumulatedText; },
    get receivedChars() { return stream.receivedChars; }
  };
}
