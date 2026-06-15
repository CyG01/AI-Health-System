declare namespace Api {
  namespace Enterprise {
    interface Plan {
      id: number;
      name: string;
      price: number;
      features: string[];
      maxUsers: number;
    }

    interface Subscription {
      id: number;
      planId: number;
      planName: string;
      status: string;
      startDate: string;
      endDate: string;
      seats: number;
      usedSeats: number;
    }

    interface ActivateParams {
      planId: number;
      seats: number;
      paymentMethod: string;
    }
  }
}
