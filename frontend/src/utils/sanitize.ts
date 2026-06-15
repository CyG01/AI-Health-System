import DOMPurify from 'dompurify';

/**
 * 净化 HTML 内容，防止 XSS 攻击。
 * 仅允许安全的格式化标签（换行、加粗等）。
 */
export function sanitizeHtml(dirty: string | null | undefined): string {
  if (!dirty) return '';
  return DOMPurify.sanitize(dirty, {
    ALLOWED_TAGS: ['br', 'b', 'i', 'em', 'strong', 'p', 'ul', 'ol', 'li', 'span'],
    ALLOWED_ATTR: ['class']
  });
}
