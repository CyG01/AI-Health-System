declare namespace Api {
  namespace Dashboard {
    /** DashboardTodayVO - today's dashboard stats (flat structure) */
    interface DashboardToday {
      isCheckedIn: boolean;
      /** 连续打卡天数 */
      streakDays: number;
      planId: number;
      planName: string;
      tasks: TaskItem[];
      completedTasks: number;
      totalTasks: number;
      exerciseCaloriesBurned: number;
      exerciseRecordsCount: number;
      dietCaloriesConsumed: number;
      dietRecordsCount: number;
      /** 最新体重 */
      weight: number;
      /** 最新 BMI */
      bmi: number;
    }

    /** TaskItem nested in DashboardTodayVO */
    interface TaskItem {
      detailId: number;
      itemType: string;
      itemName: string;
      targetAmount: string;
      status: number;
    }

    /** DashboardWeekVO - weekly dashboard overview */
    interface DashboardWeek {
      /** 周起始日期 */
      weekStart: string;
      /** 周结束日期 */
      weekEnd: string;
      /** 本周打卡天数 */
      checkinDays: number;
      /** 本周运动消耗总热量 */
      exerciseCalories: number;
      /** 本周饮食摄入总热量 */
      dietCalories: number;
      /** 本周运动记录数 */
      exerciseRecordsCount: number;
      /** 本周饮食记录数 */
      dietRecordsCount: number;
      /** 每日明细 */
      dailySummary: DaySummary[];
    }

    /** DaySummary nested in DashboardWeekVO */
    interface DaySummary {
      date: string;
      checkedIn: boolean;
      exerciseCalories: number;
      dietCalories: number;
      exerciseCount: number;
      dietCount: number;
    }

    /** DashboardMonthVO - monthly dashboard overview */
    interface DashboardMonth {
      /** 月份，格式 yyyy-MM */
      month: string;
      /** 本月打卡天数 */
      checkinDays: number;
      /** 本月总天数 */
      totalDays: number;
      /** 打卡率 */
      checkinRate: number;
      /** 本月运动消耗总热量 */
      exerciseCalories: number;
      /** 本月饮食摄入总热量 */
      dietCalories: number;
      /** 本月运动记录数 */
      exerciseRecordsCount: number;
      /** 本月饮食记录数 */
      dietRecordsCount: number;
      /** 按周汇总 */
      weeklySummary: WeekSummary[];
    }

    /** WeekSummary nested in DashboardMonthVO */
    interface WeekSummary {
      weekLabel: string;
      checkinDays: number;
      exerciseCalories: number;
      dietCalories: number;
    }

    /** DashboardGreetingVO - AI predictive greeting card */
    interface DashboardGreeting {
      /** 卡片类型：morning / noon / afternoon / reminder / celebration */
      type: string;
      /** 图标 emoji */
      icon: string;
      /** 问候语 */
      greeting: string;
      /** 主消息 */
      message: string;
      /** 详细描述 */
      detail: string;
      /** CTA 操作按钮 */
      actions: CardAction[];
      /** 今日计划完成进度（0-100），null 表示不显示进度条 */
      progress: number | null;
    }

    /** CardAction nested in DashboardGreetingVO */
    interface CardAction {
      /** 按钮文字 */
      label: string;
      /** 路由地址 */
      url: string;
      /** 是否为主按钮 */
      primary: boolean;
      /** 特殊动作（如 open_copilot） */
      action: string;
    }
  }
}
