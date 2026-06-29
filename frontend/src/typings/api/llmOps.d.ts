declare namespace Api {
  namespace LlmOps {
    /** Active prompt version info */
    interface ActiveVersion {
      templateKey: string;
      activeVersion: number;
      content: string;
    }

    /** Circuit breaker status */
    interface CircuitStatus {
      state: string;
      avgSafetyScore: number;
      summary: string;
    }

    /** Canary deployment status */
    interface CanaryStatus {
      templateKey: string;
      version: number;
      currentPercentage: number;
      status: string;
      [key: string]: unknown;
    }

    /** Prompt version entry in history */
    interface PromptVersion {
      version: number;
      content: string;
      createdAt?: string;
      isActive?: boolean;
      [key: string]: unknown;
    }

    /** Alert entry */
    interface Alert {
      id?: number;
      type?: string;
      message?: string;
      severity?: string;
      timestamp?: string;
      [key: string]: unknown;
    }
  }
}
