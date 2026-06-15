import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import { SetupStoreId } from '@/enum';

export const usePlanStore = defineStore(SetupStoreId.Plan, () => {
  const currentPlan = ref<Api.Plan.AiPlan | null>(null);
  const currentPlanDays = ref<Api.Plan.PlanDay[]>([]);
  const planVersion = ref(0);

  // AI plan streaming state
  const isStreaming = ref(false);
  const streamingContent = ref('');
  const streamingPlanType = ref<string>('');

  /**
   * Set plan data
   *
   * @param plan Plan object
   * @param days Optional days array (some callers pass days separately)
   */
  function setPlan(plan: Api.Plan.AiPlan, days?: any[]) {
    currentPlan.value = plan;
    currentPlanDays.value = days || plan.days || [];
    planVersion.value++;
  }

  function updateDayItem(dayIndex: number, updates: Partial<Api.Plan.PlanDay>) {
    if (currentPlanDays.value[dayIndex]) {
      Object.assign(currentPlanDays.value[dayIndex], updates);
      planVersion.value++;
    }
  }

  function replaceDayItems(dayIndex: number, day: Api.Plan.PlanDay) {
    currentPlanDays.value[dayIndex] = day;
    planVersion.value++;
  }

  function clearPlan() {
    currentPlan.value = null;
    currentPlanDays.value = [];
    planVersion.value++;
  }

  /** Start streaming mode for a new AI plan generation */
  function startStreaming(planType?: string) {
    isStreaming.value = true;
    streamingContent.value = '';
    streamingPlanType.value = planType || '';
  }

  /** Append a chunk of content to the streaming buffer */
  function appendStreamingContent(chunk: string) {
    streamingContent.value += chunk;
  }

  /** Finish streaming and mark as complete */
  function finishStreaming() {
    isStreaming.value = false;
  }

  /** Reset streaming state (e.g. on error or cancel) */
  function resetStreaming() {
    isStreaming.value = false;
    streamingContent.value = '';
    streamingPlanType.value = '';
  }

  return {
    currentPlan,
    currentPlanDays,
    planVersion,
    isStreaming,
    streamingContent,
    streamingPlanType,
    setPlan,
    updateDayItem,
    replaceDayItems,
    clearPlan,
    startStreaming,
    appendStreamingContent,
    finishStreaming,
    resetStreaming
  };
});
