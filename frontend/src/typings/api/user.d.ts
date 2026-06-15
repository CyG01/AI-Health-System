declare namespace Api {
  /**
   * namespace User
   *
   * backend api module: "user"
   */
  namespace User {
    /** User profile */
    interface UserProfile {
      userId: number;
      userName: string;
      nickname: string;
      email: string;
      phone: string;
      gender: Common.Gender;
      avatar: string;
      role: string;
      createdAt: string;
    }

    /** Profile update request */
    interface ProfileUpdateRequest {
      nickname?: string;
      email?: string;
      phone?: string;
      gender?: Common.Gender;
    }

    /** Password update request */
    interface PasswordUpdateRequest {
      oldPassword: string;
      newPassword: string;
      confirmPassword: string;
    }
  }
}
