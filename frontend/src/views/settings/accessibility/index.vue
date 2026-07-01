<template>
  <div class="accessibility-settings">
    <div class="settings-header">
      <h2>无障碍设置</h2>
      <p class="settings-desc">调整显示设置，让应用更适合您的需求</p>
    </div>

    <!-- 字体缩放 -->
    <div class="setting-section">
      <div class="section-title">
        <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
          <path d="M9.93 12.6h4.14L12 7.74 9.93 12.6zM12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z"/>
        </svg>
        <span>字体大小</span>
      </div>
      <div class="font-scale-options">
        <button
          v-for="level in fontScaleLevels"
          :key="level.value"
          class="scale-btn"
          :class="{ active: currentLevel === level.value }"
          @click="setFontScaleLevel(level.value)"
        >
          <span :style="{ fontSize: level.previewSize }">Aa</span>
          <span class="scale-label">{{ level.label }}</span>
        </button>
      </div>
      <div class="preview-box" :style="{ fontSize: `calc(14px * ${fontScale})` }">
        <p>预览效果：今天的步数已达到目标的 80%，继续保持！</p>
      </div>
    </div>

    <!-- 高对比度 -->
    <div class="setting-section">
      <div class="section-title">
        <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18V4c4.41 0 8 3.59 8 8s-3.59 8-8 8z"/>
        </svg>
        <span>高对比度模式</span>
      </div>
      <p class="section-hint">增强文字与背景的对比度，让内容更清晰</p>
      <div class="toggle-row">
        <span>{{ highContrast ? '已开启' : '已关闭' }}</span>
        <button
          class="toggle-switch"
          :class="{ on: highContrast }"
          @click="toggleHighContrast"
          :aria-label="highContrast ? '关闭高对比度' : '开启高对比度'"
        >
          <span class="toggle-knob" />
        </button>
      </div>
    </div>

    <!-- 动画减弱 -->
    <div class="setting-section">
      <div class="section-title">
        <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 14H9V8h2v8zm4 0h-2V8h2v8z"/>
        </svg>
        <span>减弱动画</span>
      </div>
      <p class="section-hint">减少界面动画效果，适合对运动敏感的用户</p>
      <div class="toggle-row">
        <span>{{ reduceMotion ? '已开启' : '已关闭' }}</span>
        <button
          class="toggle-switch"
          :class="{ on: reduceMotion }"
          @click="toggleReduceMotion"
          :aria-label="reduceMotion ? '关闭减弱动画' : '开启减弱动画'"
        >
          <span class="toggle-knob" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useAccessibility, type FontScaleLevel } from '@/composables/useAccessibility';

defineOptions({ name: 'AccessibilitySettings' });

const { fontScale, highContrast, setFontScaleLevel, getFontScaleLevel, toggleHighContrast } = useAccessibility();

const currentLevel = computed<FontScaleLevel>(() => getFontScaleLevel());

const fontScaleLevels: Array<{ value: FontScaleLevel; label: string; previewSize: string }> = [
  { value: 'normal', label: '标准', previewSize: '16px' },
  { value: 'large', label: '大字号', previewSize: '19px' },
  { value: 'xlarge', label: '超大字号', previewSize: '22px' }
];

// Reduce motion setting
const REDUCE_MOTION_KEY = 'a11y-reduce-motion';
const reduceMotion = ref<boolean>(
  (() => {
    try {
      return localStorage.getItem(REDUCE_MOTION_KEY) === '1';
    } catch {
      return false;
    }
  })()
);

function toggleReduceMotion() {
  reduceMotion.value = !reduceMotion.value;
  try {
    localStorage.setItem(REDUCE_MOTION_KEY, reduceMotion.value ? '1' : '0');
  } catch { /* ignore */ }

  if (reduceMotion.value) {
    document.documentElement.classList.add('a11y-reduce-motion');
  } else {
    document.documentElement.classList.remove('a11y-reduce-motion');
  }
}

// Apply reduce motion on mount
if (reduceMotion.value) {
  document.documentElement.classList.add('a11y-reduce-motion');
}
</script>

<style scoped>
.accessibility-settings {
  max-width: 560px;
  padding: 24px;
}

.settings-header {
  margin-bottom: 24px;
}

.settings-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: var(--sport-text-primary, #e6edf3);
  margin: 0 0 8px;
}

.settings-desc {
  font-size: 14px;
  color: var(--sport-text-secondary, #8b949e);
  margin: 0;
}

.setting-section {
  margin-bottom: 28px;
  padding: 16px;
  border-radius: 12px;
  background: var(--sport-bg-surface, #161b22);
  border: 1px solid var(--sport-bg-elevated, #21262d);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--sport-text-primary, #e6edf3);
  margin-bottom: 8px;
}

.section-hint {
  font-size: 13px;
  color: var(--sport-text-secondary, #8b949e);
  margin: 0 0 12px;
  line-height: 1.5;
}

.font-scale-options {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.scale-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid var(--sport-bg-elevated, #21262d);
  background: transparent;
  color: var(--sport-text-primary, #e6edf3);
  cursor: pointer;
  transition: all 0.2s;
}

.scale-btn:hover {
  border-color: var(--chart-sky, #58a6ff);
  background: rgba(88, 166, 255, 0.04);
}

.scale-btn.active {
  border-color: var(--chart-sky, #58a6ff);
  background: rgba(88, 166, 255, 0.1);
}

.scale-label {
  font-size: 12px;
  color: var(--sport-text-secondary, #8b949e);
}

.preview-box {
  padding: 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px dashed var(--sport-bg-elevated, #21262d);
}

.preview-box p {
  margin: 0;
  color: var(--sport-text-primary, #e6edf3);
  line-height: 1.6;
}

.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.toggle-switch {
  width: 44px;
  height: 24px;
  border-radius: 12px;
  border: none;
  background: var(--sport-bg-elevated, #21262d);
  cursor: pointer;
  position: relative;
  transition: background 0.2s;
  padding: 0;
}

.toggle-switch.on {
  background: var(--chart-sky, #58a6ff);
}

.toggle-knob {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: white;
  transition: transform 0.2s;
}

.toggle-switch.on .toggle-knob {
  transform: translateX(20px);
}
</style>
