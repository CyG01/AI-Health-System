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
      /** Alternative field name returned by some backend endpoints */
      token?: string;
      refreshToken: string;
      userInfo: UserInfo;
    }

    interface UserInfo {
      id: number;
      username: string;
      phone: string;
      avatar: string;
      nickname: string;
      gender: number;
      age: number;
      role: string;
      status?: number;
      createTime?: string;
    }

    interface CaptchaResponse {
      base64: string;
      uuid: string;
    }

    /** Alias used by soybean-admin auth store */
    type RawUserInfo = UserInfo;
  }
}
