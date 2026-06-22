declare namespace Api {
  namespace Enterprise {
    interface Plan {
      id: number;
      name: string;
      price: number;
      features: string[];
      maxUsers: number;
    }

    /** Matches backend Subscription entity returned by enterprise endpoints */
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

    interface ActivateParams {
      teamSize: number;
      customTokenQuotaM: number;
      customPrice: number;
      months: number;
      orderNo: string;
      channel: string;
    }

    interface UpdateConfigParams {
      teamSize?: number;
      customTokenQuotaM?: number;
      customPrice?: number;
    }
  }
}
