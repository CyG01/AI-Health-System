declare namespace Api {
  namespace Dashboard {
    interface Overview {
      latestHealth: Api.Health.HealthRecord | null;
      today: TodayStats;
      greetingCard?: GreetingCard;
    }

    interface DashboardStats {
      latestHealth: Api.Health.HealthRecord | null;
      today: TodayStats;
      greetingCard?: GreetingCard;
    }

    interface TodayStats {
      isCheckedIn?: boolean;
      streakDays?: number;
      planName?: string;
      totalTasks?: number;
      completedTasks?: number;
      exerciseCaloriesBurned?: number;
      dietCaloriesConsumed?: number;
    }

    interface GreetingCard {
      greeting: string;
      summary: string;
      actions: GreetingAction[];
    }

    interface GreetingAction {
      label: string;
      path: string;
      type: string;
    }
  }
}
