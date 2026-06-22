declare namespace Api {
  /** Auth namespace types */
  namespace Auth {
    interface LoginParams {
      username: string;
      password: string;
      captchaCode?: string;
      captchaUuid?: string;
      rememberMe?: boolean;
    }

    interface LoginByPhoneParams {
      phone: string;
      verifyCode: string;
      rememberMe?: boolean;
    }

    interface RegisterRequest {
      username: string;
      password: string;
      confirmPassword: string;
      phone: string;
      verifyCode: string;
      disclaimerAccepted: boolean;
    }

    interface SendCodeParams {
      phone: string;
    }

    interface ResetPasswordRequest {
      phone: string;
      verifyCode: string;
      newPassword: string;
      confirmPassword: string;
    }

    interface LoginToken {
      accessToken: string;
      refreshToken: string;
      userInfo: UserInfo;
    }

    interface UserInfo {
      /** User ID (backend returns 'id', store uses 'userId') */
      userId: string;
      id: number;
      /** Username (backend returns 'username', store uses 'userName') */
      userName: string;
      username: string;
      email: string;
      phone: string;
      avatar: string;
      /** User roles array (mapped from backend's single 'role' field) */
      roles: string[];
      /** Button-level permissions */
      buttons: string[];
      nickname?: string;
      gender?: string;
      birthday?: string;
      /** Original role string from backend */
      role?: string;
    }

    interface CaptchaResponse {
      base64: string;
      uuid: string;
    }

    /** Alias used by soybean-admin auth store */
    type RawUserInfo = UserInfo;
  }
}
