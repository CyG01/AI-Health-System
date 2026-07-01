declare namespace Api {
  namespace Family {
    interface Family {
      id: number;
      familyName: string;
      familyAvatar?: string;
      creatorId: number;
      maxMembers: number;
      shareHealthData: number;
      shareReports: number;
      status: number;
    }

    interface Member {
      id: number;
      familyId: number;
      userId: number;
      memberRole: 'OWNER' | 'ADMIN' | 'MEMBER' | 'CHILD' | 'ELDER';
      nicknameInFamily?: string;
      dataVisibility: 'PRIVATE' | 'FAMILY' | 'REPORT_ONLY';
      joinTime?: string;
      status: number;
      /** Display fields from join query */
      username?: string;
      avatar?: string;
      phone?: string;
    }
  }
}
