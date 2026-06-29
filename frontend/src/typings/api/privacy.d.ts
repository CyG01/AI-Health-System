declare namespace Api {
  namespace Privacy {
    /** User data consent status */
    interface ConsentStatus {
      userId: number;
      /** 0 = not consented, 1 = consented */
      dataConsentForModel: number;
      /** 0 = not consented, 1 = consented */
      dataConsentForRecommend: number;
    }

    /** Params for updating consent */
    interface ConsentUpdateParams {
      /** 0 = not consented, 1 = consented */
      dataConsentForModel?: number;
      /** 0 = not consented, 1 = consented */
      dataConsentForRecommend?: number;
    }
  }
}
