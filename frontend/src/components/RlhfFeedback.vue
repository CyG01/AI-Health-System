<template>
  <div class="rlhf-feedback" :class="{ submitted: submitted }">
    <span class="feedback-label">AI 回复质量：</span>
    <button
      class="fb-btn"
      :class="{ active: vote === 'up' }"
      :disabled="submitted"
      @click="handleVote('up')"
      :aria-label="'有用'"
      title="有用 / 采纳"
    >
      <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
      </svg>
      <span class="fb-text">有用</span>
    </button>
    <button
      class="fb-btn negative"
      :class="{ active: vote === 'down' }"
      :disabled="submitted"
      @click="handleVote('down')"
      :aria-label="'不准/有误'"
      title="不准 / 有误"
    >
      <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
        <path d="M12 2C6.47 2 2 6.47 2 12s4.47 10 10 10 10-4.47 10-10S17.53 2 12 2zm5 13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z"/>
      </svg>
      <span class="fb-text">不准</span>
    </button>
    <transition name="fade">
      <span v-if="submitted" class="fb-thanks">感谢反馈！</span>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

defineOptions({ name: 'RlhfFeedback' });

const props = defineProps<{
  messageId: string | number;
  /** Optional: the AI content text for richer feedback payload */
  aiContent?: string;
}>();

const emit = defineEmits<{
  (e: 'feedback', payload: { messageId: string | number; vote: 'up' | 'down'; aiContent?: string }): void;
}>();

const vote = ref<'up' | 'down' | null>(null);
const submitted = ref(false);

function handleVote(v: 'up' | 'down') {
  if (submitted.value) return;
  vote.value = v;
  submitted.value = true;
  emit('feedback', {
    messageId: props.messageId,
    vote: v,
    aiContent: props.aiContent
  });
}
</script>

<style scoped>
.rlhf-feedback {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  padding: 4px 0;
  opacity: 0;
  transition: opacity 0.25s;
}

.message-row.assistant:hover .rlhf-feedback,
.rlhf-feedback.submitted {
  opacity: 1;
}

.feedback-label {
  font-size: 11px;
  color: var(--sport-text-tertiary);
  flex-shrink: 0;
}

.fb-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 8px;
  border-radius: 12px;
  border: 1px solid var(--sport-bg-elevated);
  background: transparent;
  color: var(--sport-text-secondary);
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s;
}

.fb-btn:hover:not(:disabled) {
  border-color: var(--chart-sky);
  color: var(--chart-sky);
  background: rgba(88, 166, 255, 0.06);
}

.fb-btn.active {
  border-color: var(--color-success, #3fb950);
  color: var(--color-success, #3fb950);
  background: rgba(63, 185, 80, 0.1);
}

.fb-btn.negative.active {
  border-color: var(--color-danger, #f85149);
  color: var(--color-danger, #f85149);
  background: rgba(248, 81, 73, 0.1);
}

.fb-btn:disabled {
  cursor: default;
  opacity: 0.6;
}

.fb-text { line-height: 1; }

.fb-thanks {
  font-size: 11px;
  color: var(--color-success, #3fb950);
  margin-left: 4px;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
