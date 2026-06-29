/**
 * Sport Theme Chart Palette
 * Unified ECharts color palette for all chart instances.
 * Import this composable instead of hardcoding chart colors.
 */

/** Primary sport chart color palette — use as `color` option in ECharts */
export const SPORTS_CHART_PALETTE = [
  '#FF6B35', // orange — primary
  '#4ECDC4', // teal — secondary
  '#A855F7', // purple — accent
  '#FF6B6B', // coral — warm accent
  '#C7F464', // lime — success
  '#FFB800', // amber — warning
  '#38BDF8'  // sky — info
] as const;

/** Dark-mode adjusted palette (lighter variants for dark backgrounds) */
export const SPORTS_CHART_PALETTE_DARK = [
  '#FF8C5A', // lighter orange
  '#6FE0D8', // lighter teal
  '#C084FC', // lighter purple
  '#FCA5A5', // lighter coral
  '#D9F99D', // lighter lime
  '#FFD54F', // lighter amber
  '#7DD3FC'  // lighter sky
] as const;

interface ChartThemeConfig {
  color: readonly string[];
  textStyle: { color: string };
  title: { textStyle: { color: string }; subtextStyle: { color: string } };
  categoryAxis: { axisLine: { lineStyle: { color: string } }; axisTick: { lineStyle: { color: string } }; axisLabel: { textStyle: { color: string } } };
  valueAxis: { axisLine: { lineStyle: { color: string } }; splitLine: { lineStyle: { color: string } }; axisLabel: { textStyle: { color: string } } };
  tooltip: { backgroundColor: string; borderColor: string; textStyle: { color: string } };
  legend: { textStyle: { color: string } };
}

/**
 * Get a complete ECharts theme config that responds to dark/light mode.
 *
 * @param isDark - Whether dark mode is active
 * @returns ECharts option partial object with sport-themed colors
 *
 * @example
 * ```ts
 * const { getChartTheme } = useChartTheme();
 * const chart = echarts.init(el);
 * chart.setOption({
 *   ...getChartTheme(isDark.value),
 *   series: [{ type: 'bar', data: [...] }]
 * });
 * ```
 */
export function useChartTheme() {
  function getChartTheme(isDark: boolean): ChartThemeConfig {
    const palette = isDark ? SPORTS_CHART_PALETTE_DARK : SPORTS_CHART_PALETTE;
    const textColor = isDark ? '#F0F2F5' : '#1A1D27';
    const textSecondary = isDark ? '#9CA3AF' : '#6B7280';
    const borderColor = isDark ? '#2D3348' : '#E5E7EB';
    const splitLineColor = isDark ? 'rgba(45, 51, 72, 0.5)' : 'rgba(229, 231, 235, 0.6)';
    const tooltipBg = isDark ? 'rgba(26, 29, 39, 0.95)' : 'rgba(255, 255, 255, 0.95)';

    return {
      color: palette,
      textStyle: { color: textColor },
      title: {
        textStyle: { color: textColor, fontWeight: 700 },
        subtextStyle: { color: textSecondary }
      },
      categoryAxis: {
        axisLine: { lineStyle: { color: borderColor } },
        axisTick: { lineStyle: { color: borderColor } },
        axisLabel: { textStyle: { color: textSecondary } }
      },
      valueAxis: {
        axisLine: { lineStyle: { color: borderColor } },
        splitLine: { lineStyle: { color: splitLineColor } },
        axisLabel: { textStyle: { color: textSecondary } }
      },
      tooltip: {
        backgroundColor: tooltipBg,
        borderColor: borderColor,
        textStyle: { color: textColor }
      },
      legend: {
        textStyle: { color: textSecondary }
      }
    };
  }

  return { getChartTheme, palette: SPORTS_CHART_PALETTE };
}
