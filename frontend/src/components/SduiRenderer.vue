<template>
  <div
    class="sdui-widget"
    :class="`sdui-${widget.type}`"
    :aria-label="widgetAriaLabel"
    role="figure"
  >
    <!-- stat_card -->
    <template v-if="widget.type === 'stat_card'">
      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-icon">{{ iconEmoji(w('stat_card').icon) }}</span>
          <span class="stat-name">{{ w('stat_card').metricName }}</span>
        </div>
        <div class="stat-value">
          <span class="stat-num">{{ w('stat_card').value }}</span>
          <span class="stat-unit">{{ w('stat_card').unit }}</span>
        </div>
        <div v-if="w('stat_card').trend" class="stat-trend" :class="trendClass(w('stat_card').trendDirection)">
          <span class="trend-arrow">{{ trendArrow(w('stat_card').trendDirection) }}</span>
          {{ w('stat_card').trend }}
        </div>
      </div>
    </template>

    <!-- meal_plan -->
    <template v-else-if="widget.type === 'meal_plan'">
      <div class="meal-plan">
        <div class="widget-head">
          <span class="head-icon">&#127860;</span>
          <span>{{ w('meal_plan').mealType || '今日餐食' }}</span>
          <span v-if="w('meal_plan').totalCalories" class="head-badge">{{ w('meal_plan').totalCalories }} kcal</span>
        </div>
        <div v-if="w('meal_plan').items?.length" class="meal-items">
          <div v-for="(item, i) in w('meal_plan').items" :key="i" class="meal-item">
            <span class="meal-name">{{ item.name }}</span>
            <span v-if="item.calories" class="meal-cal">{{ item.calories }}kcal</span>
          </div>
        </div>
        <div v-if="w('meal_plan').nutrition" class="nutrition-row">
          <span v-if="w('meal_plan').nutrition?.protein" class="nut-chip">蛋白 {{ w('meal_plan').nutrition!.protein }}g</span>
          <span v-if="w('meal_plan').nutrition?.carbs" class="nut-chip">碳水 {{ w('meal_plan').nutrition!.carbs }}g</span>
          <span v-if="w('meal_plan').nutrition?.fat" class="nut-chip">脂肪 {{ w('meal_plan').nutrition!.fat }}g</span>
          <span v-if="w('meal_plan').nutrition?.fiber" class="nut-chip">纤维 {{ w('meal_plan').nutrition!.fiber }}g</span>
        </div>
        <p v-if="w('meal_plan').cookingTip" class="cooking-tip">{{ w('meal_plan').cookingTip }}</p>
      </div>
    </template>

    <!-- meal_chart -->
    <template v-else-if="widget.type === 'meal_chart'">
      <div class="meal-chart">
        <div class="widget-head">
          <span class="head-icon">&#128200;</span>
          <span>营养摄入</span>
        </div>
        <div class="macro-bars">
          <div class="macro-row" v-for="m in macroItems('meal_chart')" :key="m.label">
            <span class="macro-label">{{ m.label }}</span>
            <div class="macro-track">
              <div class="macro-fill" :style="{ width: m.pct + '%', background: m.color }" />
            </div>
            <span class="macro-val">{{ m.value }}g</span>
          </div>
        </div>
        <div class="cal-summary">
          <span>已摄入 <b>{{ w('meal_chart').totalCalories || 0 }}</b> kcal</span>
          <span v-if="w('meal_chart').remainingCalories != null">剩余 <b>{{ w('meal_chart').remainingCalories }}</b> kcal</span>
        </div>
        <p v-if="w('meal_chart').mealSuggestion" class="cooking-tip">{{ w('meal_chart').mealSuggestion }}</p>
      </div>
    </template>

    <!-- exercise_card -->
    <template v-else-if="widget.type === 'exercise_card'">
      <div class="exercise-card" :class="{ done: w('exercise_card').completed }">
        <div class="widget-head">
          <span class="head-icon">{{ w('exercise_card').completed ? '&#9989;' : '&#127947;' }}</span>
          <span>{{ w('exercise_card').exerciseName }}</span>
          <span v-if="w('exercise_card').scenarioTag" class="head-badge tag">{{ w('exercise_card').scenarioTag }}</span>
        </div>
        <div class="exercise-meta">
          <span v-if="w('exercise_card').duration">{{ w('exercise_card').duration }}分钟</span>
          <span v-if="w('exercise_card').intensity" class="intensity-tag" :class="intensityClass(w('exercise_card').intensity)">
            {{ w('exercise_card').intensity }}
          </span>
        </div>
        <p v-if="w('exercise_card').instruction" class="exercise-instruction">{{ w('exercise_card').instruction }}</p>
        <div v-if="w('exercise_card').phases?.length" class="phase-list">
          <div v-for="(p, i) in w('exercise_card').phases" :key="i" class="phase-item" :class="{ done: p.completed }">
            <span class="phase-dot" />
            <span class="phase-name">{{ p.name }}</span>
            <span v-if="p.durationMinutes" class="phase-dur">{{ p.durationMinutes }}min</span>
          </div>
        </div>
      </div>
    </template>

    <!-- exercise_phase -->
    <template v-else-if="widget.type === 'exercise_phase'">
      <div class="exercise-phase-widget">
        <div class="widget-head">
          <span class="head-icon">&#128170;</span>
          <span>{{ w('exercise_phase').exerciseName }}</span>
          <span v-if="w('exercise_phase').totalDuration" class="head-badge">{{ w('exercise_phase').totalDuration }}min</span>
        </div>
        <div class="phase-timeline">
          <div v-for="(p, i) in w('exercise_phase').phases" :key="i" class="phase-step" :class="[p.type, { done: p.completed }]">
            <div class="step-marker">
              <span class="step-num">{{ i + 1 }}</span>
            </div>
            <div class="step-body">
              <div class="step-title">{{ p.name }}</div>
              <div class="step-meta">
                <span v-if="p.durationMinutes">{{ p.durationMinutes }}min</span>
                <span v-if="p.heartRateZone" class="hr-zone">{{ p.heartRateZone }}</span>
              </div>
              <p v-if="p.instruction" class="step-instruction">{{ p.instruction }}</p>
            </div>
          </div>
        </div>
        <div v-if="w('exercise_phase').completedPhases != null" class="phase-progress">
          已完成 {{ w('exercise_phase').completedPhases }}/{{ w('exercise_phase').phases?.length || 0 }} 阶段
        </div>
      </div>
    </template>

    <!-- comparison -->
    <template v-else-if="widget.type === 'comparison'">
      <div class="comparison-widget">
        <div class="widget-head">
          <span class="head-icon">&#128202;</span>
          <span>{{ widget.title || '数据对比' }}</span>
        </div>
        <div class="compare-row">
          <div class="compare-side before">
            <span class="compare-label">{{ w('comparison').beforeLabel || '之前' }}</span>
            <span class="compare-value">{{ w('comparison').beforeValue }}</span>
          </div>
          <div class="compare-arrow" :class="trendClass(w('comparison').changeDirection)">
            {{ trendArrow(w('comparison').changeDirection) }}
          </div>
          <div class="compare-side after">
            <span class="compare-label">{{ w('comparison').afterLabel || '之后' }}</span>
            <span class="compare-value">{{ w('comparison').afterValue }}</span>
          </div>
        </div>
        <div v-if="w('comparison').changePercentage" class="change-pct" :class="trendClass(w('comparison').changeDirection)">
          {{ w('comparison').changePercentage }}
        </div>
      </div>
    </template>

    <!-- progress_ring -->
    <template v-else-if="widget.type === 'progress_ring'">
      <div class="progress-ring-widget">
        <svg class="ring-svg" viewBox="0 0 80 80">
          <circle class="ring-track" cx="40" cy="40" r="34" />
          <circle
            class="ring-fill"
            cx="40" cy="40" r="34"
            :style="{
              strokeDasharray: `${(w('progress_ring').percentage || 0) * 2.136} 213.6`,
              stroke: w('progress_ring').color || 'var(--chart-sky)'
            }"
          />
        </svg>
        <div class="ring-center">
          <span class="ring-pct">{{ Math.round(w('progress_ring').percentage || 0) }}%</span>
          <span v-if="w('progress_ring').label" class="ring-label">{{ w('progress_ring').label }}</span>
        </div>
        <p v-if="w('progress_ring').subText" class="ring-sub">{{ w('progress_ring').subText }}</p>
      </div>
    </template>

    <!-- sleep_chart -->
    <template v-else-if="widget.type === 'sleep_chart'">
      <div class="sleep-chart">
        <div class="widget-head">
          <span class="head-icon">&#127769;</span>
          <span>睡眠分析</span>
          <span v-if="w('sleep_chart').sleepScore" class="head-badge">{{ w('sleep_chart').sleepScore }}分</span>
        </div>
        <div v-if="w('sleep_chart').totalHours" class="sleep-total">
          总时长 <b>{{ w('sleep_chart').totalHours }}h</b>
        </div>
        <div v-if="w('sleep_chart').phases?.length" class="sleep-bars">
          <div v-for="(p, i) in w('sleep_chart').phases" :key="i" class="sleep-bar-row">
            <span class="sleep-phase-name">{{ p.name }}</span>
            <div class="sleep-bar-track">
              <div
                class="sleep-bar-fill"
                :style="{
                  width: ((p.hours || 0) / (w('sleep_chart').totalHours || 1) * 100) + '%',
                  background: p.color || `hsl(${210 + i * 30}, 70%, 60%)`
                }"
              />
            </div>
            <span class="sleep-phase-hours">{{ p.hours }}h</span>
          </div>
        </div>
        <p v-if="w('sleep_chart').suggestion" class="cooking-tip">{{ w('sleep_chart').suggestion }}</p>
      </div>
    </template>

    <!-- notification -->
    <template v-else-if="widget.type === 'notification'">
      <div class="notification-widget" :class="w('notification').severity || 'info'">
        <div class="notif-icon">{{ severityIcon(w('notification').severity) }}</div>
        <div class="notif-body">
          <p class="notif-msg">{{ w('notification').message }}</p>
          <a v-if="w('notification').actionUrl" class="notif-action" :href="w('notification').actionUrl">
            {{ w('notification').actionLabel || '查看详情' }}
          </a>
        </div>
      </div>
    </template>

    <!-- quiz -->
    <template v-else-if="widget.type === 'quiz'">
      <div class="quiz-widget">
        <div class="widget-head">
          <span class="head-icon">&#10067;</span>
          <span>健康知识</span>
        </div>
        <p class="quiz-question">{{ w('quiz').question }}</p>
        <div class="quiz-options">
          <button
            v-for="(opt, i) in w('quiz').options"
            :key="i"
            class="quiz-opt"
            :class="{
              selected: selectedOption === i,
              correct: w('quiz').showResult && opt === w('quiz').correctAnswer,
              wrong: w('quiz').showResult && selectedOption === i && opt !== w('quiz').correctAnswer
            }"
            @click="handleQuizSelect(i)"
          >{{ opt }}</button>
        </div>
        <p v-if="w('quiz').showResult && w('quiz').explanation" class="quiz-explanation">{{ w('quiz').explanation }}</p>
      </div>
    </template>

    <!-- timer -->
    <template v-else-if="widget.type === 'timer'">
      <div class="timer-widget">
        <div class="timer-display">
          {{ formatTimer(w('timer').totalSeconds || 0) }}
        </div>
        <div v-if="widget.title" class="timer-label">{{ widget.title }}</div>
        <div class="timer-type-tag">{{ w('timer').timerType === 'countdown' ? '倒计时' : '正计时' }}</div>
      </div>
    </template>

    <!-- tip -->
    <template v-else-if="widget.type === 'tip'">
      <div class="tip-widget">
        <span class="tip-icon">{{ w('tip').icon || '&#128161;' }}</span>
        <span class="tip-content">{{ w('tip').content }}</span>
        <span v-if="w('tip').category" class="tip-cat">{{ w('tip').category }}</span>
      </div>
    </template>

    <!-- text_block -->
    <template v-else-if="widget.type === 'text_block'">
      <div
        class="text-block"
        :class="[`size-${w('text_block').textSize || 'medium'}`, { bold: w('text_block').bold }]"
      >{{ w('text_block').content }}</div>
    </template>

    <!-- fallback: unknown type -->
    <template v-else>
      <div class="text-block size-medium">
        <span v-if="(widget as Sdui.WidgetBase).title" class="fallback-title">{{ (widget as Sdui.WidgetBase).title }}</span>
        <span v-if="(widget as Sdui.WidgetBase).props?.content">{{ (widget as Sdui.WidgetBase).props!.content }}</span>
        <span v-else class="fallback-hint">[{{ (widget as Sdui.WidgetBase).type }}]</span>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';

defineOptions({ name: 'SduiRenderer' });

const props = defineProps<{
  widget: Sdui.Widget;
}>();

/** Accessibility: generate descriptive aria-label for screen readers */
const widgetAriaLabel = computed(() => {
  const w = props.widget;
  switch (w.type) {
    case 'stat_card':
      return `${w.metricName || '指标'}: ${w.value || ''} ${w.unit || ''}${w.trend ? `, 趋势${w.trend}` : ''}`;
    case 'sleep_chart':
      return `睡眠分析: 评分${w.sleepScore || 0}分, 总时长${w.totalHours || 0}小时`;
    case 'progress_ring':
      return `进度: ${Math.round(w.percentage || 0)}%${w.label ? `, ${w.label}` : ''}`;
    case 'meal_chart':
      return `营养摄入: 已摄入${w.totalCalories || 0}千卡`;
    case 'comparison':
      return `数据对比: ${w.beforeLabel || '之前'} ${w.beforeValue} → ${w.afterLabel || '之后'} ${w.afterValue}`;
    case 'exercise_card':
      return `运动: ${w.exerciseName || ''}, ${w.duration || 0}分钟, ${w.intensity || ''}强度`;
    case 'tip':
      return `健康提示: ${w.content || ''}`;
    case 'notification':
      return `${w.severity || '通知'}: ${w.message || ''}`;
    default:
      return w.title || w.type;
  }
});

/** Type-safe cast helper */
function w<T extends Sdui.Widget['type']>(type: T): Extract<Sdui.Widget, { type: T }> {
  return props.widget as Extract<Sdui.Widget, { type: T }>;
}

// --- Quiz state ---
const selectedOption = ref<number | null>(null);
function handleQuizSelect(i: number) {
  if (selectedOption.value === i) selectedOption.value = null;
  else selectedOption.value = i;
}

// --- Helpers ---
function iconEmoji(icon?: string): string {
  const map: Record<string, string> = {
    weight: '\u2696\uFE0F', steps: '\uD83D\uDEB6', heart: '\u2764\uFE0F',
    water: '\uD83D\uDCA7', sleep: '\uD83C\uDF19', fire: '\uD83D\uDD25',
    run: '\uD83C\uDFC3', food: '\uD83C\uDF5C', clock: '\u23F0'
  };
  return icon ? (map[icon] || '\uD83D\uDCCA') : '\uD83D\uDCCA';
}

function trendClass(dir?: string): string {
  if (dir === 'up') return 'trend-up';
  if (dir === 'down') return 'trend-down';
  return 'trend-flat';
}

function trendArrow(dir?: string): string {
  if (dir === 'up') return '\u2191';
  if (dir === 'down') return '\u2193';
  return '\u2192';
}

function intensityClass(intensity?: string): string {
  const map: Record<string, string> = { '低': 'low', '中': 'mid', '高': 'high', low: 'low', medium: 'mid', high: 'high' };
  return intensity ? (map[intensity] || 'mid') : 'mid';
}

function severityIcon(sev?: string): string {
  const map: Record<string, string> = { info: '\u2139\uFE0F', warning: '\u26A0\uFE0F', error: '\u274C', success: '\u2705' };
  return sev ? (map[sev] || '\u2139\uFE0F') : '\u2139\uFE0F';
}

function formatTimer(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

function macroItems(type: 'meal_chart') {
  const d = w(type);
  const max = Math.max(d.protein || 0, d.carbs || 0, d.fat || 0, 1);
  return [
    { label: '蛋白质', value: d.protein || 0, pct: ((d.protein || 0) / max) * 100, color: 'var(--chart-sky)' },
    { label: '碳水', value: d.carbs || 0, pct: ((d.carbs || 0) / max) * 100, color: 'var(--sport-accent)' },
    { label: '脂肪', value: d.fat || 0, pct: ((d.fat || 0) / max) * 100, color: 'var(--chart-orange, #f0a020)' }
  ];
}
</script>

<style scoped>
.sdui-widget {
  margin-top: 8px;
  border-radius: 10px;
  background: var(--sport-bg-surface);
  border: 1px solid var(--sport-bg-elevated);
  overflow: hidden;
  /* Micro-interaction: smooth hover float + shadow */
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1),
              box-shadow 0.25s ease;
  will-change: transform;
}

.sdui-widget:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15),
              0 2px 6px rgba(88, 166, 255, 0.08);
}

/* Accessibility: font scaling via CSS variable (--a11y-font-scale, default 1) */
.sdui-widget {
  font-size: calc(1em * var(--a11y-font-scale, 1));
}

/* Mobile responsive: compact layout on small screens */
@media (max-width: 640px) {
  .stat-num { font-size: 22px; }
  .macro-label { width: 36px; font-size: 10px; }
  .macro-val { width: 30px; font-size: 10px; }
  .sleep-phase-name { width: 40px; font-size: 10px; }
  .compare-value { font-size: 16px; }
  .timer-display { font-size: 28px; }
  .widget-head { font-size: 12px; padding: 8px 10px 6px; }
}

/* ---- Shared header ---- */
.widget-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--sport-text-primary);
}
.head-icon { font-size: 16px; }
.head-badge {
  margin-left: auto;
  font-size: 12px;
  font-weight: 500;
  padding: 1px 8px;
  border-radius: 10px;
  background: rgba(88, 166, 255, 0.12);
  color: var(--chart-sky);
}
.head-badge.tag {
  background: rgba(160, 120, 255, 0.12);
  color: #a078ff;
}

/* ---- Stat card ---- */
.stat-card { padding: 12px; }
.stat-header { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--sport-text-secondary); }
.stat-icon { font-size: 16px; }
.stat-value { display: flex; align-items: baseline; gap: 3px; margin-top: 6px; }
.stat-num { font-size: 28px; font-weight: 700; color: var(--sport-text-primary); line-height: 1; }
.stat-unit { font-size: 13px; color: var(--sport-text-secondary); }
.stat-trend { font-size: 12px; margin-top: 4px; display: flex; align-items: center; gap: 2px; }
.trend-up { color: var(--color-success, #3fb950); }
.trend-down { color: var(--color-danger, #f85149); }
.trend-flat { color: var(--sport-text-tertiary); }

/* ---- Meal plan ---- */
.meal-plan { padding: 0 0 10px; }
.meal-items { padding: 0 12px; display: flex; flex-direction: column; gap: 4px; }
.meal-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 6px 10px; border-radius: 6px; background: rgba(255,255,255,0.03);
  font-size: 13px;
}
.meal-name { color: var(--sport-text-primary); }
.meal-cal { color: var(--sport-text-secondary); font-size: 12px; }
.nutrition-row { display: flex; flex-wrap: wrap; gap: 6px; padding: 8px 12px 0; }
.nut-chip {
  font-size: 11px; padding: 2px 8px; border-radius: 8px;
  background: rgba(88, 166, 255, 0.08); color: var(--chart-sky);
}
.cooking-tip {
  margin: 8px 12px 0; padding: 6px 10px; font-size: 12px;
  border-radius: 6px; background: rgba(255,255,255,0.03);
  color: var(--sport-text-secondary); line-height: 1.5;
}

/* ---- Meal chart (macro bars) ---- */
.meal-chart { padding: 0 0 10px; }
.macro-bars { padding: 0 12px; display: flex; flex-direction: column; gap: 6px; }
.macro-row { display: flex; align-items: center; gap: 8px; }
.macro-label { width: 42px; font-size: 11px; color: var(--sport-text-secondary); text-align: right; flex-shrink: 0; }
.macro-track { flex: 1; height: 8px; border-radius: 4px; background: rgba(255,255,255,0.06); overflow: hidden; }
.macro-fill { height: 100%; border-radius: 4px; transition: width 0.4s ease; }
.macro-val { width: 36px; font-size: 11px; color: var(--sport-text-secondary); text-align: right; }
.cal-summary {
  display: flex; justify-content: space-between; padding: 8px 12px 0;
  font-size: 12px; color: var(--sport-text-secondary);
}
.cal-summary b { color: var(--sport-text-primary); }

/* ---- Exercise card ---- */
.exercise-card { padding: 0 0 10px; }
.exercise-card.done { opacity: 0.7; }
.exercise-meta { display: flex; gap: 8px; padding: 0 12px; font-size: 12px; color: var(--sport-text-secondary); }
.intensity-tag {
  padding: 1px 6px; border-radius: 4px; font-size: 11px; font-weight: 500;
}
.intensity-tag.low { background: rgba(63,185,80,0.12); color: #3fb950; }
.intensity-tag.mid { background: rgba(210,153,34,0.12); color: #d29922; }
.intensity-tag.high { background: rgba(248,81,73,0.12); color: #f85149; }
.exercise-instruction { margin: 6px 12px 0; font-size: 12px; color: var(--sport-text-secondary); line-height: 1.5; }
.phase-list { padding: 8px 12px 0; display: flex; flex-direction: column; gap: 4px; }
.phase-item {
  display: flex; align-items: center; gap: 6px; font-size: 12px;
  color: var(--sport-text-secondary); padding: 3px 0;
}
.phase-item.done { color: var(--color-success, #3fb950); }
.phase-dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--sport-text-tertiary); flex-shrink: 0;
}
.phase-item.done .phase-dot { background: var(--color-success, #3fb950); }
.phase-name { flex: 1; }
.phase-dur { font-size: 11px; color: var(--sport-text-tertiary); }

/* ---- Exercise phase timeline ---- */
.exercise-phase-widget { padding: 0 0 10px; }
.phase-timeline { padding: 0 12px; }
.phase-step {
  display: flex; gap: 10px; padding-bottom: 12px; position: relative;
}
.phase-step:not(:last-child)::before {
  content: ''; position: absolute; left: 11px; top: 24px; bottom: 0;
  width: 2px; background: var(--sport-bg-elevated);
}
.phase-step.done:not(:last-child)::before { background: var(--color-success, #3fb950); }
.step-marker {
  width: 24px; height: 24px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: var(--sport-bg-elevated); font-size: 11px; font-weight: 600;
  color: var(--sport-text-secondary);
}
.phase-step.done .step-marker { background: var(--color-success, #3fb950); color: #fff; }
.phase-step.warmup .step-marker { border: 2px solid #d29922; }
.phase-step.core .step-marker { border: 2px solid var(--chart-sky); }
.phase-step.cooldown .step-marker { border: 2px solid #3fb950; }
.step-body { flex: 1; min-width: 0; }
.step-title { font-size: 13px; font-weight: 500; color: var(--sport-text-primary); }
.step-meta { display: flex; gap: 8px; font-size: 11px; color: var(--sport-text-secondary); margin-top: 2px; }
.hr-zone { padding: 0 4px; border-radius: 3px; background: rgba(248,81,73,0.1); color: #f85149; }
.step-instruction { font-size: 12px; color: var(--sport-text-secondary); margin-top: 4px; line-height: 1.4; }
.phase-progress { padding: 4px 12px 0; font-size: 12px; color: var(--sport-text-secondary); text-align: center; }

/* ---- Comparison ---- */
.comparison-widget { padding: 0 0 10px; }
.compare-row { display: flex; align-items: center; justify-content: center; gap: 16px; padding: 8px 12px; }
.compare-side { text-align: center; flex: 1; }
.compare-label { display: block; font-size: 11px; color: var(--sport-text-tertiary); margin-bottom: 4px; }
.compare-value { font-size: 20px; font-weight: 700; color: var(--sport-text-primary); }
.compare-arrow { font-size: 24px; font-weight: 700; flex-shrink: 0; }
.change-pct { text-align: center; font-size: 13px; font-weight: 600; padding-bottom: 4px; }

/* ---- Progress ring ---- */
.progress-ring-widget { padding: 12px; text-align: center; position: relative; }
.ring-svg { width: 80px; height: 80px; transform: rotate(-90deg); }
.ring-track { fill: none; stroke: rgba(255,255,255,0.06); stroke-width: 6; }
.ring-fill { fill: none; stroke-width: 6; stroke-linecap: round; transition: stroke-dasharray 0.5s ease; }
.ring-center { position: absolute; top: 12px; left: 12px; width: 80px; height: 80px; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.ring-pct { font-size: 18px; font-weight: 700; color: var(--sport-text-primary); }
.ring-label { font-size: 10px; color: var(--sport-text-secondary); }
.ring-sub { font-size: 12px; color: var(--sport-text-secondary); margin-top: 4px; }

/* ---- Sleep chart ---- */
.sleep-chart { padding: 0 0 10px; }
.sleep-total { padding: 0 12px; font-size: 13px; color: var(--sport-text-secondary); }
.sleep-total b { color: var(--sport-text-primary); }
.sleep-bars { padding: 8px 12px 0; display: flex; flex-direction: column; gap: 6px; }
.sleep-bar-row { display: flex; align-items: center; gap: 8px; }
.sleep-phase-name { width: 48px; font-size: 11px; color: var(--sport-text-secondary); text-align: right; flex-shrink: 0; }
.sleep-bar-track { flex: 1; height: 10px; border-radius: 5px; background: rgba(255,255,255,0.06); overflow: hidden; }
.sleep-bar-fill { height: 100%; border-radius: 5px; transition: width 0.4s ease; }
.sleep-phase-hours { width: 32px; font-size: 11px; color: var(--sport-text-secondary); }

/* ---- Notification ---- */
.notification-widget {
  display: flex; gap: 10px; padding: 10px 12px; align-items: flex-start;
}
.notification-widget.info { border-left: 3px solid var(--chart-sky); }
.notification-widget.warning { border-left: 3px solid #d29922; }
.notification-widget.error { border-left: 3px solid var(--color-danger, #f85149); }
.notification-widget.success { border-left: 3px solid var(--color-success, #3fb950); }
.notif-icon { font-size: 18px; flex-shrink: 0; }
.notif-body { flex: 1; }
.notif-msg { font-size: 13px; color: var(--sport-text-primary); line-height: 1.5; margin: 0; }
.notif-action { font-size: 12px; color: var(--chart-sky); text-decoration: none; margin-top: 4px; display: inline-block; }
.notif-action:hover { text-decoration: underline; }

/* ---- Quiz ---- */
.quiz-widget { padding: 0 0 10px; }
.quiz-question { padding: 0 12px; font-size: 14px; font-weight: 500; color: var(--sport-text-primary); line-height: 1.5; margin: 0 0 8px; }
.quiz-options { padding: 0 12px; display: flex; flex-direction: column; gap: 6px; }
.quiz-opt {
  display: block; width: 100%; text-align: left; padding: 8px 12px;
  border-radius: 8px; border: 1px solid var(--sport-bg-elevated);
  background: transparent; color: var(--sport-text-primary); font-size: 13px;
  cursor: pointer; transition: all 0.15s;
}
.quiz-opt:hover { background: rgba(255,255,255,0.04); }
.quiz-opt.selected { border-color: var(--chart-sky); background: rgba(88, 166, 255, 0.08); }
.quiz-opt.correct { border-color: var(--color-success, #3fb950); background: rgba(63,185,80,0.1); }
.quiz-opt.wrong { border-color: var(--color-danger, #f85149); background: rgba(248,81,73,0.1); }
.quiz-explanation { margin: 8px 12px 0; padding: 8px 10px; font-size: 12px; color: var(--sport-text-secondary); background: rgba(255,255,255,0.03); border-radius: 6px; line-height: 1.5; }

/* ---- Timer ---- */
.timer-widget { padding: 16px; text-align: center; }
.timer-display { font-size: 36px; font-weight: 700; font-variant-numeric: tabular-nums; color: var(--sport-text-primary); letter-spacing: 2px; }
.timer-label { font-size: 13px; color: var(--sport-text-secondary); margin-top: 4px; }
.timer-type-tag { font-size: 11px; color: var(--sport-text-tertiary); margin-top: 2px; }

/* ---- Tip ---- */
.tip-widget {
  display: flex; align-items: center; gap: 8px; padding: 10px 12px;
}
.tip-icon { font-size: 18px; flex-shrink: 0; }
.tip-content { flex: 1; font-size: 13px; color: var(--sport-text-primary); line-height: 1.5; }
.tip-cat {
  font-size: 11px; padding: 1px 6px; border-radius: 6px;
  background: rgba(88, 166, 255, 0.08); color: var(--chart-sky); flex-shrink: 0;
}

/* ---- Text block / fallback ---- */
.text-block { padding: 10px 12px; color: var(--sport-text-primary); line-height: 1.6; }
.text-block.size-small { font-size: 12px; }
.text-block.size-medium { font-size: 14px; }
.text-block.size-large { font-size: 16px; }
.text-block.bold { font-weight: 600; }
.fallback-title { font-weight: 600; display: block; margin-bottom: 4px; }
.fallback-hint { color: var(--sport-text-tertiary); font-size: 12px; }
</style>
