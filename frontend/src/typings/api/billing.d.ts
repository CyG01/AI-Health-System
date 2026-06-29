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
      invoiceNo: string;
      invoiceType: string;
      invoiceTitle: string;
      taxNumber?: string;
      createTime?: string;
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

    /** Monthly summary returned by GET /billing/monthly */
    interface MonthlySummary {
      month: number;
      year: number;
      monthlyTokens: number;
      monthlyCost: number;
      monthlyTokenLimitM: number;
      monthlyTokenLimitBytes: number;
      monthlyLimitExceeded: boolean;
      daysUntilExpiry: number;
    }

    /** Quota warning returned by GET /billing/quota-warning */
    interface QuotaWarning {
      warningLevel: string;
      warningMessage: string;
      suggestion: string;
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
