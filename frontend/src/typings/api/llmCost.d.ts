declare namespace Api {
  namespace LlmCost {
    /** Tier-level cost breakdown */
    interface TierCost {
      tier: string;
      cost: number;
      requestCount: number;
    }

    /** Global daily cost overview */
    interface GlobalDailyCost {
      totalCost: number;
      tierCosts: TierCost[];
      date: string;
    }

    /** Intent-level cost breakdown for a user */
    interface IntentCost {
      intent: string;
      cost: number;
      requestCount: number;
    }

    /** Model-level cost breakdown for a user */
    interface ModelCost {
      model: string;
      cost: number;
      requestCount: number;
    }

    /** User daily cost detail */
    interface UserDailyCost {
      userId: number;
      totalCost: number;
      costByIntent: IntentCost[];
      costByModel: ModelCost[];
      isPaused: boolean;
      date: string;
    }

    /** Over-budget user entry */
    interface OverBudgetUser {
      userId: number;
      username?: string;
      totalCost: number;
      budget: number;
      [key: string]: unknown;
    }

    /** Model routing status */
    interface ModelStatus {
      [key: string]: unknown;
    }

    /** Tier circuit breaker status */
    interface TierCircuitBreakerStatus {
      [key: string]: unknown;
    }
  }
}
