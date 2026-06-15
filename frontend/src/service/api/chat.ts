import { request } from '../request';

/** Create a new chat session */
export function fetchCreateSession() {
  return request<Api.Chat.Session>({
    url: '/chat/session/create',
    method: 'post'
  });
}

/** Get chat session list */
export function fetchGetSessionList() {
  return request<Api.Chat.Session[]>({
    url: '/chat/session/list',
    method: 'get'
  });
}

/** Get messages for a session */
export function fetchGetMessages(sessionId: string) {
  return request<Api.Chat.Message[]>({
    url: `/chat/session/${sessionId}/messages`,
    method: 'get'
  });
}

/** Delete a chat session */
export function fetchDeleteSession(sessionId: string) {
  return request<void>({
    url: `/chat/session/${sessionId}`,
    method: 'delete'
  });
}

// Note: sendMessage, sendMessageWithContext, and sendMessageTypewriter
// use SSE streams via sseClient.ts and are not included here as standard request functions.
// Import createSSEStream / createTypewriterStream from '@/utils/sseClient' for streaming.

// =============================================================================
// SSE Endpoint: POST /chat/send-with-context
// =============================================================================
// Sends a chat message with page context for AI awareness.
// Accepts the same ChatSendDTO as /chat/send, plus a `context` field:
//   { sessionId, content, context: { page: string, entityId?: string | number } }
// The backend prepends "[用户当前在「{page}」页面，实体ID={entityId}]" to the content.
// Returns SSE stream — use with sseClient (createSSEStream / createTypewriterStream).
// =============================================================================
