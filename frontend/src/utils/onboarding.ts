/**
 * 渐进式新手引导 (Progressive Onboarding)
 *
 * 基于 driver.js 实现轻量级引导，不弹窗打扰，
 * 通过高亮聚光灯柔和地指出关键功能入口。
 *
 * 触发条件：
 * - 首次进入 Dashboard 时自动启动基础引导
 * - 用户触发特定操作时启动对应功能引导
 */

import { driver } from 'driver.js';
import 'driver.js/dist/driver.css';

const ONBOARDED_KEY = 'app-onboarded';
const ONBOARDED_VERSION = '1'; // 递增以触发重新引导

/** 检查是否需要引导 */
export function needsOnboarding(): boolean {
  try {
    const stored = localStorage.getItem(ONBOARDED_KEY);
    return stored !== ONBOARDED_VERSION;
  } catch {
    return true;
  }
}

/** 标记引导完成 */
export function markOnboarded() {
  try {
    localStorage.setItem(ONBOARDED_KEY, ONBOARDED_VERSION);
  } catch { /* ignore */ }
}

/** Dashboard 首次引导 */
export function startDashboardTour() {
  const driverObj = driver({
    showProgress: true,
    animate: true,
    smoothScroll: true,
    stagePadding: 8,
    stageRadius: 12,
    allowClose: true,
    overlayOpacity: 0.6,
    popoverClass: 'onboarding-popover',
    nextBtnText: '下一步 →',
    prevBtnText: '← 上一步',
    doneBtnText: '开始使用',
    progressText: '{{current}} / {{total}}',
    onDestroyStarted: () => {
      markOnboarded();
      driverObj.destroy();
    },
    steps: [
      {
        element: '[data-onboarding="copilot-fab"]',
        popover: {
          title: 'AI 智能助手',
          description: '点击这里，随时向 AI 助手提问健康问题、记录饮食、调整计划。试试拍下你的第一顿午餐！',
          side: 'left',
          align: 'end'
        }
      },
      {
        element: '[data-onboarding="dashboard-stats"]',
        popover: {
          title: '健康数据看板',
          description: '这里展示你的核心健康指标：体重、步数、心率、睡眠等。数据每日自动更新。',
          side: 'bottom',
          align: 'start'
        }
      },
      {
        element: '[data-onboarding="nav-plan"]',
        popover: {
          title: '个性化计划',
          description: 'AI 根据你的健康数据生成专属运动和饮食计划，支持一键调整和固化。',
          side: 'right',
          align: 'start'
        }
      },
      {
        element: '[data-onboarding="nav-food"]',
        popover: {
          title: '饮食记录',
          description: '快速记录每餐饮食，AI 自动分析营养摄入并给出建议。',
          side: 'right',
          align: 'start'
        }
      }
    ]
  });

  // 延迟启动，确保页面渲染完成
  setTimeout(() => {
    // 检查引导元素是否存在，跳过不存在的步骤
    const allSteps = [
      {
        element: '[data-onboarding="copilot-fab"]',
        popover: {
          title: 'AI 智能助手',
          description: '点击这里，随时向 AI 助手提问健康问题、记录饮食、调整计划。试试拍下你的第一顿午餐！',
          side: 'left' as const,
          align: 'end' as const
        }
      },
      {
        element: '[data-onboarding="dashboard-stats"]',
        popover: {
          title: '健康数据看板',
          description: '这里展示你的核心健康指标：体重、步数、心率、睡眠等。数据每日自动更新。',
          side: 'bottom' as const,
          align: 'start' as const
        }
      },
      {
        element: '[data-onboarding="nav-plan"]',
        popover: {
          title: '个性化计划',
          description: 'AI 根据你的健康数据生成专属运动和饮食计划，支持一键调整和固化。',
          side: 'right' as const,
          align: 'start' as const
        }
      },
      {
        element: '[data-onboarding="nav-food"]',
        popover: {
          title: '饮食记录',
          description: '快速记录每餐饮食，AI 自动分析营养摄入并给出建议。',
          side: 'right' as const,
          align: 'start' as const
        }
      }
    ];

    const visibleSteps = allSteps.filter(step =>
      !step.element || document.querySelector(step.element) !== null
    );

    if (visibleSteps.length > 0) {
      driverObj.setSteps(visibleSteps);
      driverObj.drive();
    } else {
      markOnboarded();
    }
  }, 800);
}

/** 特定功能引导（用户触发特定操作时调用） */
export function startFeatureTour(feature: string) {
  const tours: Record<string, Parameters<typeof driver>[0]> = {
    'copilot-photo': {
      animate: true,
      allowClose: true,
      overlayOpacity: 0.5,
      popoverClass: 'onboarding-popover',
      nextBtnText: '知道了',
      steps: [
        {
          element: '[data-onboarding="photo-btn"]',
          popover: {
            title: '拍照识别食物',
            description: '点击相机图标，拍下你的餐食，AI 会自动识别食物并估算热量。',
            side: 'top',
            align: 'center'
          }
        }
      ]
    },
    'family-switch': {
      animate: true,
      allowClose: true,
      overlayOpacity: 0.5,
      popoverClass: 'onboarding-popover',
      nextBtnText: '知道了',
      steps: [
        {
          element: '[data-onboarding="family-switcher"]',
          popover: {
            title: '家庭组切换',
            description: '在这里切换查看不同家庭成员的健康数据，为全家人管理健康。',
            side: 'bottom',
            align: 'start'
          }
        }
      ]
    }
  };

  const config = tours[feature];
  if (!config) return;

  const driverObj = driver(config);
  setTimeout(() => driverObj.drive(), 300);
}
