/** Default theme settings — "活力运动风" Sport Theme */
export const themeSettings: App.Theme.ThemeSetting = {
  themeScheme: 'light',
  grayscale: false,
  colourWeakness: false,
  recommendColor: false,
  themeColor: '#FF6B35',
  themeRadius: 16,
  otherColor: {
    info: '#4ECDC4',
    success: '#C7F464',
    warning: '#FFB800',
    error: '#FF6B6B'
  },
  isInfoFollowPrimary: false,
  layout: {
    mode: 'vertical',
    scrollMode: 'content'
  },
  page: {
    animate: true,
    animateMode: 'fade-slide'
  },
  header: {
    height: 60,
    breadcrumb: {
      visible: true,
      showIcon: true
    },
    multilingual: {
      visible: true
    },
    globalSearch: {
      visible: true
    }
  },
  tab: {
    visible: true,
    cache: true,
    height: 44,
    mode: 'chrome',
    closeTabByMiddleClick: false
  },
  fixedHeaderAndTab: true,
  sider: {
    inverted: false,
    width: 220,
    collapsedWidth: 64,
    mixWidth: 90,
    mixCollapsedWidth: 64,
    mixChildMenuWidth: 200,
    autoSelectFirstMenu: false
  },
  footer: {
    visible: true,
    fixed: false,
    height: 48,
    right: true
  },
  watermark: {
    visible: false,
    text: 'AI Health System',
    enableUserName: false,
    enableTime: false,
    timeFormat: 'YYYY-MM-DD HH:mm'
  },
  tokens: {
    light: {
      colors: {
        container: 'rgb(255, 255, 255)',
        layout: 'rgb(250, 250, 250)',
        inverted: 'rgb(255, 107, 53)',
        'base-text': 'rgb(26, 29, 39)'
      },
      boxShadow: {
        header: '0 1px 3px rgb(0, 0, 0, 0.05)',
        sider: '2px 0 8px 0 rgb(0, 0, 0, 0.04)',
        tab: '0 1px 2px rgb(0, 0, 0, 0.06)'
      }
    },
    dark: {
      colors: {
        container: 'rgb(15, 17, 23)',
        layout: 'rgb(26, 29, 39)',
        inverted: 'rgb(255, 140, 90)',
        'base-text': 'rgb(240, 242, 245)'
      },
      boxShadow: {
        header: '0 1px 3px rgb(0, 0, 0, 0.2)',
        sider: '2px 0 8px 0 rgb(0, 0, 0, 0.15)',
        tab: '0 1px 2px rgb(0, 0, 0, 0.2)'
      }
    }
  }
};

/**
 * Override theme settings
 *
 * If publish new version, use `overrideThemeSettings` to override certain theme settings
 */
export const overrideThemeSettings: Partial<App.Theme.ThemeSetting> = {};
