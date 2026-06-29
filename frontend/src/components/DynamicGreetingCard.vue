<template>
  <div class="greeting-card" :class="cardStyleClass">
    <!-- 主卡片 -->
    <div class="greeting-content">
      <div class="greeting-header">
        <span class="greeting-emoji">{{ card.icon }}</span>
        <span class="greeting-time">{{ card.greeting }}</span>
        <NTag v-if="card.type === 'reminder'" type="error" size="small" :bordered="false" class="reminder-tag">
          待完成
        </NTag>
        <NTag v-if="card.type === 'celebration'" type="success" size="small" :bordered="false" class="celebration-tag">
          已完成
        </NTag>
      </div>
      <h3 class="greeting-message">{{ card.message }}</h3>
      <p v-if="card.detail" class="greeting-detail">{{ card.detail }}</p>

      <!-- CTA 按钮 -->
      <div v-if="card.actions?.length" class="greeting-actions">
        <NButton
          v-for="action in card.actions"
          :key="action.label"
          :type="action.primary ? 'primary' : 'default'"
          size="small"
          @click="$emit('action', action)"
        >
          {{ action.label }}
        </NButton>
      </div>
    </div>

    <!-- 进度条 (如果有今日计划进度) -->
    <div v-if="card.progress != null" class="greeting-progress">
      <NProgress
        type="line"
        :percentage="card.progress"
        :color="progressColor"
        :height="6"
        :show-indicator="false"
        :rail-color="'rgba(255,255,255,0.15)'"
      />
      <span class="progress-label">{{ card.progress }}% 完成</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NTag, NButton, NProgress } from 'naive-ui'

interface GreetingAction {
  label: string
  primary?: boolean
}

interface GreetingCard {
  type: 'morning' | 'noon' | 'afternoon' | 'reminder' | 'celebration' | 'default'
  icon: string
  greeting: string
  message: string
  detail?: string
  actions?: GreetingAction[]
  progress?: number | null
}

const props = defineProps<{
  card: GreetingCard
}>()

defineEmits<{
  (e: 'action', action: GreetingAction): void
}>()

const cardStyleClass = computed(() => {
  switch (props.card.type) {
    case 'morning':
      return 'card-morning'
    case 'noon':
      return 'card-noon'
    case 'reminder':
      return 'card-reminder'
    case 'celebration':
      return 'card-celebration'
    case 'afternoon':
      return 'card-afternoon'
    default:
      return 'card-default'
  }
})

const progressColor = computed(() => {
  const p = props.card.progress || 0
  if (p >= 80) return 'var(--sport-primary)'
  if (p >= 50) return 'var(--sport-secondary)'
  return '#FFB800'
})
</script>

<style scoped>
.greeting-card {
  border-radius: var(--radius-base);
  padding: 24px;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
  border: 1px solid transparent;
  transition: transform 0.2s, box-shadow 0.2s;
}

.greeting-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.3);
}

/* 卡片主题 */
.card-morning {
  background: linear-gradient(135deg, #4ECDC4 0%, #44A3AA 50%, #4ECDC4 100%);
  background-size: 200% 200%;
  border-color: rgba(78, 205, 196, 0.3);
  animation: gradient-shift 8s ease infinite;
}

.card-noon {
  background: linear-gradient(135deg, #FF6B35 0%, #C7F464 50%, #FF6B35 100%);
  background-size: 200% 200%;
  border-color: rgba(255, 107, 53, 0.3);
  animation: gradient-shift 8s ease infinite;
}

.card-reminder {
  background: linear-gradient(135deg, #FF6B6B 0%, #ff8585 50%, #FF6B6B 100%);
  background-size: 200% 200%;
  border-color: rgba(255, 107, 107, 0.3);
  animation: gradient-shift 8s ease infinite, reminder-glow 2s ease-in-out infinite;
}

@keyframes reminder-glow {
  0%, 100% { box-shadow: 0 0 15px rgba(255, 107, 107, 0.2); }
  50% { box-shadow: 0 0 25px rgba(255, 107, 107, 0.4); }
}

@keyframes gradient-shift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.card-celebration {
  background: linear-gradient(135deg, #FF6B35 0%, #FF6B6B 50%, #FF6B35 100%);
  background-size: 200% 200%;
  border-color: rgba(255, 107, 53, 0.3);
  animation: gradient-shift 8s ease infinite;
}

.card-afternoon {
  background: linear-gradient(135deg, #A855F7 0%, #4ECDC4 50%, #A855F7 100%);
  background-size: 200% 200%;
  border-color: rgba(168, 85, 247, 0.3);
  animation: gradient-shift 8s ease infinite;
}

.card-default {
  background: linear-gradient(135deg, #4ECDC4 0%, #44A3AA 50%, #4ECDC4 100%);
  background-size: 200% 200%;
  border-color: var(--sport-border);
  animation: gradient-shift 8s ease infinite;
}

/* 内容 */
.greeting-content {
  position: relative;
  z-index: 1;
}

.greeting-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.greeting-emoji {
  font-size: 28px;
}

.greeting-time {
  font-size: 16px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.reminder-tag, .celebration-tag {
  margin-left: 8px;
}

.greeting-message {
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  margin: 0 0 6px;
  line-height: 1.5;
}

.greeting-detail {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0 0 14px;
  line-height: 1.6;
}

.greeting-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

/* 进度条 */
.greeting-progress {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  gap: 12px;
}

.greeting-progress :deep(.n-progress) {
  flex: 1;
}

.progress-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  white-space: nowrap;
}
</style>
