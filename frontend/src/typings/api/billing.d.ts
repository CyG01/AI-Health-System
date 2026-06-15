declare namespace Api {
  namespace Billing {
    interface Transaction {
      id: number;
      userId: number;
      type: string;
      amount: number;
      description: string;
      status: string;
      createdAt: string;
    }

    interface Balance {
      credits: number;
      totalSpent: number;
      plan: string;
      expiresAt?: string;
    }

    interface Invoice {
      id: number;
      amount: number;
      date: string;
      status: string;
      downloadUrl?: string;
    }

    interface RefundRequest {
      id: number;
      transactionId: number;
      amount: number;
      reason: string;
      status: string;
      createdAt: string;
    }

    /** Daily usage record returned by GET /billing/history */
    interface UserUsage {
      id: number;
      userId: number;
      usageDate: string;
      inputTokens: number;
      outputTokens: number;
      apiCallCount: number;
      planGenCount: number;
      foodRecogCount: number;
      chatCount: number;
      dailyCost: number;
      createdAt: string;
    }
  }
}
