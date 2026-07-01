/**
 * 增量 JSON 解析器 — 用于流式 SDUI 渐进渲染。
 *
 * 当大模型正在吐出 JSON 时，每收到一段 delta 就尝试提取已完整的字段，
 * 让前端可以边接收边渲染图表，而不是等全部完成才显示。
 *
 * 策略：
 * 1. 累积所有收到的文本片段
 * 2. 每次新增后尝试找到最外层 {} 的平衡点
 * 3. 如果找到完整 JSON 对象则解析返回
 * 4. 如果 JSON 不完整但 data 数组已有元素，提取已有部分用于渐进渲染
 */

export interface ProgressiveParseResult {
  /** 已成功解析的完整 JSON（如果已完整） */
  complete: Sdui.Widget | null;
  /** 部分解析的数据（用于渐进渲染） */
  partial: Partial<Record<string, unknown>> | null;
  /** 是否已完成（收到 DONE 信号） */
  done: boolean;
}

export class ProgressiveJsonParser {
  private buffer = '';
  private lastParsed: Partial<Record<string, unknown>> | null = null;

  /** 喂入新的文本片段，返回当前解析状态 */
  feed(delta: string): ProgressiveParseResult {
    this.buffer += delta;
    return this.tryParse();
  }

  /** 标记流结束 */
  finish(): ProgressiveParseResult {
    return this.tryParse();
  }

  /** 重置解析器 */
  reset() {
    this.buffer = '';
    this.lastParsed = null;
  }

  private tryParse(): ProgressiveParseResult {
    const text = this.buffer.trim();
    if (!text) return { complete: null, partial: null, done: false };

    // 1. 尝试完整解析
    try {
      const parsed = JSON.parse(text);
      if (parsed && typeof parsed === 'object' && parsed.type) {
        return { complete: parsed as Sdui.Widget, partial: parsed, done: true };
      }
    } catch {
      // Not complete JSON yet — try partial extraction
    }

    // 2. 尝试提取最外层 {} 的平衡部分
    const jsonStart = text.indexOf('{');
    if (jsonStart === -1) {
      return { complete: null, partial: this.lastParsed, done: false };
    }

    // 找到 JSON 开始位置
    const jsonText = text.slice(jsonStart);
    let depth = 0;
    let inString = false;
    let escape = false;
    let lastValidPos = -1;

    for (let i = 0; i < jsonText.length; i++) {
      const ch = jsonText[i];

      if (escape) {
        escape = false;
        continue;
      }

      if (ch === '\\') {
        escape = true;
        continue;
      }

      if (ch === '"') {
        inString = !inString;
        continue;
      }

      if (inString) continue;

      if (ch === '{' || ch === '[') depth++;
      else if (ch === '}' || ch === ']') {
        depth--;
        if (depth === 0) {
          lastValidPos = i;
          break;
        }
      }
    }

    // 如果找到了完整的对象边界
    if (lastValidPos > 0) {
      try {
        const completeJson = jsonText.slice(0, lastValidPos + 1);
        const parsed = JSON.parse(completeJson);
        if (parsed && parsed.type) {
          this.lastParsed = parsed;
          return { complete: parsed as Sdui.Widget, partial: parsed, done: true };
        }
      } catch {
        // Fall through to partial extraction
      }
    }

    // 3. 尝试部分提取：截断到最后一个完整的 key-value 对
    // 找到最后一个逗号或闭合括号前的位置
    const partialText = this.extractPartialObject(jsonText);
    if (partialText) {
      try {
        const parsed = JSON.parse(partialText);
        if (parsed && typeof parsed === 'object') {
          this.lastParsed = parsed;
          return { complete: null, partial: parsed, done: false };
        }
      } catch {
        // Partial parse failed, keep last known state
      }
    }

    return { complete: null, partial: this.lastParsed, done: false };
  }

  /**
   * 尝试从不完整的 JSON 文本中提取部分对象。
   * 策略：从末尾逐步截断，尝试补全为合法 JSON。
   */
  private extractPartialObject(jsonText: string): string | null {
    // 找到 type 字段（必须有）
    const typeMatch = jsonText.match(/"type"\s*:\s*"(\w+)"/);
    if (!typeMatch) return null;

    // 找到最后一个完整的 "key": value 对
    // 从末尾向前找最后一个逗号
    let lastComma = -1;
    let inStr = false;
    let esc = false;
    let depth = 0;

    for (let i = 1; i < jsonText.length; i++) {
      const ch = jsonText[i];
      if (esc) { esc = false; continue; }
      if (ch === '\\') { esc = true; continue; }
      if (ch === '"') { inStr = !inStr; continue; }
      if (inStr) continue;
      if (ch === '{' || ch === '[') depth++;
      else if (ch === '}' || ch === ']') depth--;
      else if (ch === ',' && depth === 1) lastComma = i;
    }

    if (lastComma > 0) {
      // 截断到最后一个完整字段，补上闭合括号
      const truncated = jsonText.slice(0, lastComma).trimEnd();
      // 计算需要补几个 }
      let openCount = 0;
      for (const ch of truncated) {
        if (ch === '{') openCount++;
        else if (ch === '}') openCount--;
      }
      return truncated + '}'.repeat(Math.max(0, openCount));
    }

    return null;
  }
}
