/**
 * 无障碍 (Accessibility) 设置 composable
 *
 * 提供全局字体缩放和高对比度模式，通过 CSS 变量驱动。
 * 设置持久化到 localStorage，刷新后保留。
 */
import { ref, watch } from 'vue';

const FONT_SCALE_KEY = 'a11y-font-scale';
const HIGH_CONTRAST_KEY = 'a11y-high-contrast';

/** 字体缩放档位 */
export type FontScaleLevel = 'normal' | 'large' | 'xlarge';

const SCALE_MAP: Record<FontScaleLevel, number> = {
  normal: 1,
  large: 1.15,
  xlarge: 1.3
};

function readStoredNumber(key: string, fallback: number): number {
  try {
    const v = localStorage.getItem(key);
    return v ? Number(v) : fallback;
  } catch {
    return fallback;
  }
}

function readStoredBool(key: string, fallback: boolean): boolean {
  try {
    const v = localStorage.getItem(key);
    return v === null ? fallback : v === '1';
  } catch {
    return fallback;
  }
}

// Singleton reactive state (shared across all components)
const fontScale = ref<number>(readStoredNumber(FONT_SCALE_KEY, 1));
const highContrast = ref<boolean>(readStoredBool(HIGH_CONTRAST_KEY, false));

function applyCssVars() {
  const root = document.documentElement;
  root.style.setProperty('--a11y-font-scale', String(fontScale.value));
  if (highContrast.value) {
    root.classList.add('a11y-high-contrast');
  } else {
    root.classList.remove('a11y-high-contrast');
  }
}

// Persist on change
watch(fontScale, (v) => {
  try { localStorage.setItem(FONT_SCALE_KEY, String(v)); } catch { /* ignore */ }
  applyCssVars();
}, { immediate: true });

watch(highContrast, (v) => {
  try { localStorage.setItem(HIGH_CONTRAST_KEY, v ? '1' : '0'); } catch { /* ignore */ }
  applyCssVars();
}, { immediate: true });

export function useAccessibility() {
  function setFontScaleLevel(level: FontScaleLevel) {
    fontScale.value = SCALE_MAP[level];
  }

  function toggleHighContrast() {
    highContrast.value = !highContrast.value;
  }

  function getFontScaleLevel(): FontScaleLevel {
    if (fontScale.value >= 1.3) return 'xlarge';
    if (fontScale.value >= 1.1) return 'large';
    return 'normal';
  }

  return {
    fontScale,
    highContrast,
    setFontScaleLevel,
    getFontScaleLevel,
    toggleHighContrast
  };
}
