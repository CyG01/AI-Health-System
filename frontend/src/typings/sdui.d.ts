declare namespace Sdui {
  /** Widget base — all widgets share these fields */
  interface WidgetBase {
    type: string;
    title?: string;
    props?: Record<string, unknown>;
  }

  // ---- 13 widget types matching backend com.example.sdui ----

  interface StatCard extends WidgetBase {
    type: 'stat_card';
    metricName?: string;
    value?: string;
    unit?: string;
    trend?: string;
    trendDirection?: 'up' | 'down' | 'flat';
    icon?: string;
  }

  interface MealPlan extends WidgetBase {
    type: 'meal_plan';
    mealType?: string;
    totalCalories?: number;
    items?: Array<{ name: string; calories?: number; protein?: number; imageUrl?: string }>;
    nutrition?: { protein?: number; carbs?: number; fat?: number; fiber?: number };
    cookingTip?: string;
  }

  interface MealChart extends WidgetBase {
    type: 'meal_chart';
    totalCalories?: number;
    protein?: number;
    carbs?: number;
    fat?: number;
    remainingCalories?: number;
    mealSuggestion?: string;
  }

  interface ExerciseCard extends WidgetBase {
    type: 'exercise_card';
    exerciseName?: string;
    duration?: number;
    intensity?: string;
    videoUrl?: string;
    instruction?: string;
    completed?: boolean;
    checkinAction?: string;
    scenarioTag?: string;
    phases?: ExercisePhase[];
    completedPhases?: number;
  }

  interface ExercisePhaseWidget extends WidgetBase {
    type: 'exercise_phase';
    exerciseName?: string;
    totalDuration?: number;
    intensity?: string;
    scenarioTag?: string;
    phases?: ExercisePhase[];
    completedPhases?: number;
    videoUrl?: string;
  }

  interface ExercisePhase {
    name: string;
    type: 'warmup' | 'core' | 'cooldown';
    durationMinutes?: number;
    instruction?: string;
    heartRateZone?: string;
    completed?: boolean;
  }

  interface Comparison extends WidgetBase {
    type: 'comparison';
    beforeLabel?: string;
    beforeValue?: string;
    afterLabel?: string;
    afterValue?: string;
    changePercentage?: string;
    changeDirection?: 'up' | 'down' | 'flat';
  }

  interface ProgressRing extends WidgetBase {
    type: 'progress_ring';
    percentage?: number;
    label?: string;
    color?: string;
    subText?: string;
  }

  interface SleepChart extends WidgetBase {
    type: 'sleep_chart';
    sleepScore?: number;
    totalHours?: number;
    deepSleepHours?: number;
    lightSleepHours?: number;
    remHours?: number;
    phases?: Array<{ name: string; hours?: number; color?: string }>;
    suggestion?: string;
  }

  interface Notification extends WidgetBase {
    type: 'notification';
    message?: string;
    severity?: 'info' | 'warning' | 'error' | 'success';
    actionUrl?: string;
    actionLabel?: string;
    dismissible?: boolean;
  }

  interface Quiz extends WidgetBase {
    type: 'quiz';
    question?: string;
    options?: string[];
    correctAnswer?: string;
    explanation?: string;
    showResult?: boolean;
  }

  interface Timer extends WidgetBase {
    type: 'timer';
    totalSeconds?: number;
    timerType?: 'countdown' | 'stopwatch';
    startAction?: string;
    pauseAction?: string;
    resetAction?: string;
  }

  interface Tip extends WidgetBase {
    type: 'tip';
    content?: string;
    icon?: string;
    category?: string;
  }

  interface TextBlock extends WidgetBase {
    type: 'text_block';
    content?: string;
    textSize?: 'small' | 'medium' | 'large';
    bold?: boolean;
  }

  /** Union of all known widget types */
  type Widget =
    | StatCard
    | MealPlan
    | MealChart
    | ExerciseCard
    | ExercisePhaseWidget
    | Comparison
    | ProgressRing
    | SleepChart
    | Notification
    | Quiz
    | Timer
    | Tip
    | TextBlock;
}
