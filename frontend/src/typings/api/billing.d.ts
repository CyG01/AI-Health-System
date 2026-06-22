declare namespace Api {
  namespace Billing {
    interface Balance {
      userId: number;
      tier: string;
      inputTokens: number;
      outputTokens: number;
      planGenCount: number;
      foodRecogCount: number;
      chatCount: number;
      apiCallCount: number;
      dailyCost: number;
      monthlyCost: number;
      limit?: {
        dailyCallLimit: number;
        dailyPlanGenLimit: number;
        dailyFoodRecogLimit: number;
        dailyChatLimit: number;
        monthlyTokenLimitM: number;
      };
      exceeded: boolean;
      usagePercent: number;
      quotaLevel: string;
      monthlyTokensUsed: number;
    }

    interface Invoice {
      id: number;
      amount: number;
      date: string;
      status: string;
      downloadUrl?: string;
    }

    interface Subscription {
      id: number;
      userId: number;
      tier: string;
      status: string;
      startTime: string;
      endTime: string;
      autoRenew: boolean;
      orderNo: string;
      paymentChannel: string;
      teamSize: number;
      customTokenQuotaM: number;
      customPrice: number;
      refundStatus: string;
      refundAmount: number;
      refundReason: string;
      refundTime: string;
      createdAt: string;
      updatedAt: string;
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
