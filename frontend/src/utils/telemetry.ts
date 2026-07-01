/**
 * 前端遥测与异常捕获模块
 *
 * 轻量级错误追踪，替代 Sentry 等重型方案。
 * 捕获：JS 运行时错误、Promise 未处理异常、SDUI 解析错误、ECharts 渲染错误。
 * 错误存储到 IndexedDB，定期批量上报（或手动触发上报）。
 */

import { put, getAll, remove, openDB } from './telemetryDb';

export interface TelemetryError {
  id?: string;
  timestamp: number;
  type: 'runtime' | 'promise' | 'sdui_parse' | 'echarts_render' | 'api_error' | 'custom';
  message: string;
  stack?: string;
  component?: string;
  widgetType?: string;
  route?: string;
  userAgent?: string;
  reported: boolean;
}

const DB_NAME = 'ai-health-telemetry';
const STORE_NAME = 'errors';
const MAX_STORED_ERRORS = 200;

let initialized = false;
const errorQueue: TelemetryError[] = [];

/** 初始化全局错误捕获 */
export function initTelemetry() {
  if (initialized) return;
  initialized = true;

  // 1. 捕获未处理的 JS 运行时错误
  window.addEventListener('error', (event) => {
    captureError({
      type: 'runtime',
      message: event.message,
      stack: event.error?.stack,
      component: extractComponentFromStack(event.error?.stack)
    });
  });

  // 2. 捕获未处理的 Promise 异常
  window.addEventListener('unhandledrejection', (event) => {
    const reason = event.reason;
    captureError({
      type: 'promise',
      message: reason?.message || String(reason),
      stack: reason?.stack,
      component: extractComponentFromStack(reason?.stack)
    });
  });

  // 3. 定期清理旧错误（保留最近 MAX_STORED_ERRORS 条）
  setInterval(() => pruneOldErrors(), 60_000);

  console.info('[Telemetry] 前端遥测已初始化');
}

/** 手动捕获错误 */
export function captureError(error: Omit<TelemetryError, 'id' | 'timestamp' | 'userAgent' | 'reported'>) {
  const entry: TelemetryError = {
    ...error,
    timestamp: Date.now(),
    id: `err_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    userAgent: navigator.userAgent,
    route: window.location.pathname,
    reported: false
  };

  // 同时存入内存队列和 IndexedDB
  errorQueue.push(entry);
  if (errorQueue.length > 50) errorQueue.shift();

  // 异步写入 IndexedDB（不阻塞主线程）
  put(STORE_NAME, entry as unknown as Record<string, unknown>).catch((err) => {
    console.warn('[Telemetry] 写入错误失败', err);
  });

  // 开发环境打印
  if (import.meta.env.DEV) {
    console.warn(`[Telemetry][${error.type}] ${error.message}`, error.component || '');
  }
}

/** 捕获 SDUI 解析错误 */
export function captureSduiError(widgetType: string, rawJson: string, error: Error) {
  captureError({
    type: 'sdui_parse',
    message: `SDUI 解析失败: ${error.message}`,
    stack: error.stack,
    widgetType,
    component: 'SduiRenderer'
  });
}

/** 捕获 ECharts 渲染错误 */
export function captureEchartsError(chartType: string, error: Error) {
  captureError({
    type: 'echarts_render',
    message: `ECharts 渲染异常: ${error.message}`,
    stack: error.stack,
    component: 'ECharts'
  });
}

/** 获取所有未上报的错误 */
export async function getUnreportedErrors(): Promise<TelemetryError[]> {
  try {
    const all = await getAll(STORE_NAME) as unknown as TelemetryError[];
    return all.filter(e => !e.reported).sort((a, b) => a.timestamp - b.timestamp);
  } catch {
    return [];
  }
}

/** 标记错误为已上报 */
export async function markAsReported(errorIds: string[]) {
  for (const id of errorIds) {
    await remove(STORE_NAME, id).catch(() => {});
  }
}

/** 获取内存队列中的最近错误（用于开发调试） */
export function getRecentErrors(count = 10): TelemetryError[] {
  return errorQueue.slice(-count).reverse();
}

/** 清理旧错误，保留最近 MAX_STORED_ERRORS 条 */
async function pruneOldErrors() {
  try {
    const all = await getAll(STORE_NAME) as unknown as TelemetryError[];
    if (all.length > MAX_STORED_ERRORS) {
      const sorted = all.sort((a, b) => a.timestamp - b.timestamp);
      const toRemove = sorted.slice(0, all.length - MAX_STORED_ERRORS);
      for (const err of toRemove) {
        if (err.id) await remove(STORE_NAME, err.id).catch(() => {});
      }
    }
  } catch {
    // Silently fail
  }
}

/** 从堆栈信息中提取组件名 */
function extractComponentFromStack(stack?: string): string | undefined {
  if (!stack) return undefined;
  // 匹配 Vue 组件名（如 SduiRenderer.vue, GlobalCopilotDrawer.vue）
  const match = stack.match(/(\w+\.vue)/);
  return match?.[1];
}
