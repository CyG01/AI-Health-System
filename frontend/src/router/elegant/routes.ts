import type { ElegantConstRoute } from '@elegant-router/types';

export const generatedRoutes: ElegantConstRoute[] = [
  {
    name: '403',
    path: '/403',
    component: 'layout.blank$view.403',
    meta: {
      title: '403',
      i18nKey: 'route.403',
      constant: true
    }
  },
  {
    name: '404',
    path: '/404',
    component: 'layout.blank$view.404',
    meta: {
      title: '404',
      i18nKey: 'route.404',
      constant: true
    }
  },
  {
    name: '500',
    path: '/500',
    component: 'layout.blank$view.500',
    meta: {
      title: '500',
      i18nKey: 'route.500',
      constant: true
    }
  },
  {
    name: 'admin',
    path: '/admin',
    component: 'layout.base',
    meta: {
      title: 'admin',
      i18nKey: 'route.admin',
      roles: ['admin']
    },
    children: [
      {
        name: 'admin_ai-feedback',
        path: '/admin/ai-feedback',
        component: 'view.admin_ai-feedback',
        meta: {
          title: 'admin_ai-feedback',
          i18nKey: 'route.admin_ai-feedback'
        }
      },
      {
        name: 'admin_announcement-manage',
        path: '/admin/announcement-manage',
        component: 'view.admin_announcement-manage',
        meta: {
          title: 'admin_announcement-manage',
          i18nKey: 'route.admin_announcement-manage'
        }
      },
      {
        name: 'admin_approval-manage',
        path: '/admin/approval-manage',
        component: 'view.admin_approval-manage',
        meta: {
          title: 'admin_approval-manage',
          i18nKey: 'route.admin_approval-manage'
        }
      },
      {
        name: 'admin_audit-log',
        path: '/admin/audit-log',
        component: 'view.admin_audit-log',
        meta: {
          title: 'admin_audit-log',
          i18nKey: 'route.admin_audit-log'
        }
      },
      {
        name: 'admin_dashboard',
        path: '/admin/dashboard',
        component: 'view.admin_dashboard',
        meta: {
          title: '数据概览',
          i18nKey: 'route.admin_dashboard',
          roles: ['admin']
        }
      },
      {
        name: 'admin_exercise-manage',
        path: '/admin/exercise-manage',
        component: 'view.admin_exercise-manage',
        meta: {
          title: 'admin_exercise-manage',
          i18nKey: 'route.admin_exercise-manage'
        }
      },
      {
        name: 'admin_food-manage',
        path: '/admin/food-manage',
        component: 'view.admin_food-manage',
        meta: {
          title: 'admin_food-manage',
          i18nKey: 'route.admin_food-manage'
        }
      },
      {
        name: 'admin_llm-cost-monitor',
        path: '/admin/llm-cost-monitor',
        component: 'view.admin_llm-cost-monitor',
        meta: {
          title: 'admin_llm-cost-monitor',
          i18nKey: 'route.admin_llm-cost-monitor'
        }
      },
      {
        name: 'admin_llm-ops',
        path: '/admin/llm-ops',
        component: 'view.admin_llm-ops',
        meta: {
          title: 'admin_llm-ops',
          i18nKey: 'route.admin_llm-ops'
        }
      },
      {
        name: 'admin_notification-send',
        path: '/admin/notification-send',
        component: 'view.admin_notification-send',
        meta: {
          title: 'admin_notification-send',
          i18nKey: 'route.admin_notification-send'
        }
      },
      {
        name: 'admin_plan-feedback',
        path: '/admin/plan-feedback',
        component: 'view.admin_plan-feedback',
        meta: {
          title: 'admin_plan-feedback',
          i18nKey: 'route.admin_plan-feedback'
        }
      },
      {
        name: 'admin_rule-suggestion',
        path: '/admin/rule-suggestion',
        component: 'view.admin_rule-suggestion',
        meta: {
          title: 'admin_rule-suggestion',
          i18nKey: 'route.admin_rule-suggestion'
        }
      },
      {
        name: 'admin_user-manage',
        path: '/admin/user-manage',
        component: 'view.admin_user-manage',
        meta: {
          title: 'admin_user-manage',
          i18nKey: 'route.admin_user-manage'
        }
      }
    ]
  },
  {
    name: 'billing',
    path: '/billing',
    component: 'layout.base',
    meta: {
      title: 'billing',
      i18nKey: 'route.billing'
    },
    children: [
      {
        name: 'billing_billing',
        path: '/billing/billing',
        component: 'view.billing_billing',
        meta: {
          title: 'billing_billing',
          i18nKey: 'route.billing_billing'
        }
      },
      {
        name: 'billing_refund-invoice',
        path: '/billing/refund-invoice',
        component: 'view.billing_refund-invoice',
        meta: {
          title: 'billing_refund-invoice',
          i18nKey: 'route.billing_refund-invoice'
        }
      }
    ]
  },
  {
    name: 'body',
    path: '/body',
    component: 'layout.base',
    meta: {
      title: 'body',
      i18nKey: 'route.body'
    },
    children: [
      {
        name: 'body_measurement',
        path: '/body/measurement',
        component: 'view.body_measurement',
        meta: {
          title: 'body_measurement',
          i18nKey: 'route.body_measurement'
        }
      }
    ]
  },
  {
    name: 'chat',
    path: '/chat',
    component: 'layout.base',
    meta: {
      title: 'chat',
      i18nKey: 'route.chat'
    },
    children: [
      {
        name: 'chat_chat-bot',
        path: '/chat/chat-bot',
        component: 'view.chat_chat-bot',
        meta: {
          title: 'chat_chat-bot',
          i18nKey: 'route.chat_chat-bot'
        }
      }
    ]
  },
  {
    name: 'checkin',
    path: '/checkin',
    component: 'layout.base',
    meta: {
      title: 'checkin',
      i18nKey: 'route.checkin'
    },
    children: [
      {
        name: 'checkin_calendar',
        path: '/checkin/calendar',
        component: 'view.checkin_calendar',
        meta: {
          title: 'checkin_calendar',
          i18nKey: 'route.checkin_calendar'
        }
      }
    ]
  },
  {
    name: 'community',
    path: '/community',
    component: 'layout.base',
    meta: {
      title: 'community',
      i18nKey: 'route.community'
    },
    children: [
      {
        name: 'community_feed',
        path: '/community/feed',
        component: 'view.community_feed',
        meta: {
          title: 'community_feed',
          i18nKey: 'route.community_feed'
        }
      }
    ]
  },
  {
    name: 'dashboard',
    path: '/dashboard',
    component: 'layout.base$view.dashboard',
    meta: {
      title: '仪表盘',
      i18nKey: 'route.dashboard',
      icon: 'mdi:view-dashboard',
      order: 1,
      keepAlive: true
    }
  },
  {
    name: 'enterprise',
    path: '/enterprise',
    component: 'layout.base',
    meta: {
      title: 'enterprise',
      i18nKey: 'route.enterprise'
    },
    children: [
      {
        name: 'enterprise_activate',
        path: '/enterprise/activate',
        component: 'view.enterprise_activate',
        meta: {
          title: 'enterprise_activate',
          i18nKey: 'route.enterprise_activate'
        }
      }
    ]
  },
  {
    name: 'exercise',
    path: '/exercise',
    component: 'layout.base',
    meta: {
      title: 'exercise',
      i18nKey: 'route.exercise'
    },
    children: [
      {
        name: 'exercise_record',
        path: '/exercise/record',
        component: 'view.exercise_record',
        meta: {
          title: 'exercise_record',
          i18nKey: 'route.exercise_record'
        }
      }
    ]
  },
  {
    name: 'export',
    path: '/export',
    component: 'layout.base',
    meta: {
      title: 'export',
      i18nKey: 'route.export'
    },
    children: [
      {
        name: 'export_export',
        path: '/export/export',
        component: 'view.export_export',
        meta: {
          title: 'export_export',
          i18nKey: 'route.export_export'
        }
      }
    ]
  },
  {
    name: 'food',
    path: '/food',
    component: 'layout.base',
    meta: {
      title: 'food',
      i18nKey: 'route.food'
    },
    children: [
      {
        name: 'food_record',
        path: '/food/record',
        component: 'view.food_record',
        meta: {
          title: 'food_record',
          i18nKey: 'route.food_record'
        }
      }
    ]
  },
  {
    name: 'goal',
    path: '/goal',
    component: 'layout.base',
    meta: {
      title: 'goal',
      i18nKey: 'route.goal'
    },
    children: [
      {
        name: 'goal_milestones',
        path: '/goal/milestones',
        component: 'view.goal_milestones',
        meta: {
          title: 'goal_milestones',
          i18nKey: 'route.goal_milestones'
        }
      }
    ]
  },
  {
    name: 'health',
    path: '/health',
    component: 'layout.base',
    meta: {
      title: 'health',
      i18nKey: 'route.health'
    },
    children: [
      {
        name: 'health_blood-sugar',
        path: '/health/blood-sugar',
        component: 'view.health_blood-sugar',
        meta: {
          title: 'health_blood-sugar',
          i18nKey: 'route.health_blood-sugar'
        }
      },
      {
        name: 'health_create',
        path: '/health/create',
        component: 'view.health_create',
        meta: {
          title: 'health_create',
          i18nKey: 'route.health_create'
        }
      },
      {
        name: 'health_form',
        path: '/health/form',
        component: 'view.health_form',
        meta: {
          title: 'health_form',
          i18nKey: 'route.health_form'
        }
      },
      {
        name: 'health_report',
        path: '/health/report',
        component: 'view.health_report',
        meta: {
          title: 'health_report',
          i18nKey: 'route.health_report'
        }
      },
      {
        name: 'health_view',
        path: '/health/view',
        component: 'view.health_view',
        meta: {
          title: 'health_view',
          i18nKey: 'route.health_view'
        }
      }
    ]
  },
  {
    name: 'iframe-page',
    path: '/iframe-page/:url',
    component: 'layout.base$view.iframe-page',
    props: true,
    meta: {
      title: 'iframe-page',
      i18nKey: 'route.iframe-page'
    }
  },
  {
    name: 'login',
    path: '/login/:module(pwd-login|code-login|register|reset-pwd|bind-wechat)?',
    component: 'layout.blank$view.login',
    meta: {
      title: '登录',
      i18nKey: 'route.login',
      constant: true
    },
    props: true
  },
  {
    name: 'notification',
    path: '/notification',
    component: 'layout.base',
    meta: {
      title: 'notification',
      i18nKey: 'route.notification'
    },
    children: [
      {
        name: 'notification_notification-list',
        path: '/notification/notification-list',
        component: 'view.notification_notification-list',
        meta: {
          title: 'notification_notification-list',
          i18nKey: 'route.notification_notification-list'
        }
      }
    ]
  },
  {
    name: 'plan',
    path: '/plan',
    component: 'layout.base',
    meta: {
      title: 'plan',
      i18nKey: 'route.plan'
    },
    children: [
      {
        name: 'plan_detail',
        path: '/plan/detail',
        component: 'view.plan_detail',
        meta: {
          title: 'plan_detail',
          i18nKey: 'route.plan_detail'
        }
      },
      {
        name: 'plan_generate',
        path: '/plan/generate',
        component: 'view.plan_generate',
        meta: {
          title: 'plan_generate',
          i18nKey: 'route.plan_generate'
        }
      },
      {
        name: 'plan_list',
        path: '/plan/list',
        component: 'view.plan_list',
        meta: {
          title: 'plan_list',
          i18nKey: 'route.plan_list'
        }
      }
    ]
  },
  {
    name: 'settings',
    path: '/settings',
    component: 'layout.base',
    meta: {
      title: 'settings',
      i18nKey: 'route.settings'
    },
    children: [
      {
        name: 'settings_accessibility',
        path: '/settings/accessibility',
        component: 'view.settings_accessibility',
        meta: {
          title: 'settings_accessibility',
          i18nKey: 'route.settings_accessibility'
        }
      },
      {
        name: 'settings_notification-preference',
        path: '/settings/notification-preference',
        component: 'view.settings_notification-preference',
        meta: {
          title: 'settings_notification-preference',
          i18nKey: 'route.settings_notification-preference'
        }
      },
      {
        name: 'settings_privacy',
        path: '/settings/privacy',
        component: 'view.settings_privacy',
        meta: {
          title: 'settings_privacy',
          i18nKey: 'route.settings_privacy'
        }
      }
    ]
  },
  {
    name: 'sleep',
    path: '/sleep',
    component: 'layout.base',
    meta: {
      title: 'sleep',
      i18nKey: 'route.sleep'
    },
    children: [
      {
        name: 'sleep_record',
        path: '/sleep/record',
        component: 'view.sleep_record',
        meta: {
          title: 'sleep_record',
          i18nKey: 'route.sleep_record'
        }
      }
    ]
  },
  {
    name: 'statistics',
    path: '/statistics',
    component: 'layout.base',
    meta: {
      title: 'statistics',
      i18nKey: 'route.statistics'
    },
    children: [
      {
        name: 'statistics_dashboard',
        path: '/statistics/dashboard',
        component: 'view.statistics_dashboard',
        meta: {
          title: 'statistics_dashboard',
          i18nKey: 'route.statistics_dashboard'
        }
      }
    ]
  },
  {
    name: 'water',
    path: '/water',
    component: 'layout.base',
    meta: {
      title: 'water',
      i18nKey: 'route.water'
    },
    children: [
      {
        name: 'water_record',
        path: '/water/record',
        component: 'view.water_record',
        meta: {
          title: 'water_record',
          i18nKey: 'route.water_record'
        }
      }
    ]
  }
];
