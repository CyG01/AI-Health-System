import { computed, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { defineStore } from 'pinia';
import { useLoading } from '@sa/hooks';
import { fetchGetUserInfo, fetchLogin, fetchLoginByPhone, fetchLogout, fetchRefreshToken } from '@/service/api';
import { useRouterPush } from '@/hooks/common/router';
import { localStg } from '@/utils/storage';
import { SetupStoreId } from '@/enum';
import { $t } from '@/locales';
import { useRouteStore } from '../route';
import { useTabStore } from '../tab';
import { clearAuthStorage, getToken } from './shared';

export const useAuthStore = defineStore(SetupStoreId.Auth, () => {
  const route = useRoute();
  const authStore = useAuthStore();
  const routeStore = useRouteStore();
  const tabStore = useTabStore();
  const { toLogin, redirectFromLogin } = useRouterPush(false);
  const { loading: loginLoading, startLoading, endLoading } = useLoading();

  const token = ref('');

  const userInfo: Api.Auth.UserInfo = reactive({
    userId: '',
    id: 0,
    userName: '',
    username: '',
    email: '',
    phone: '',
    avatar: '',
    roles: [],
    buttons: []
  });

  // Token proactive refresh timer (from legacy user store)
  let refreshTimerId: ReturnType<typeof setTimeout> | null = null;
  // accessToken valid for 2h, refresh 5 min early = 115 min = 6900000 ms
  const REFRESH_BEFORE_MS = 115 * 60 * 1000;

  /** is super role in static route */
  const isStaticSuper = computed(() => {
    const { VITE_AUTH_ROUTE_MODE, VITE_STATIC_SUPER_ROLE } = import.meta.env;

    return VITE_AUTH_ROUTE_MODE === 'static' && userInfo.roles.includes(VITE_STATIC_SUPER_ROLE);
  });

  /** Is login */
  const isLogin = computed(() => Boolean(token.value));

  /** Schedule proactive token refresh */
  function scheduleTokenRefresh() {
    cancelTokenRefresh();
    if (!token.value) return;

    refreshTimerId = setTimeout(async () => {
      try {
        await refreshAccessToken();
      } catch {
        // Refresh failed - let the 401 response interceptor handle logout
      }
    }, REFRESH_BEFORE_MS);
  }

  /** Cancel token refresh timer */
  function cancelTokenRefresh() {
    if (refreshTimerId) {
      clearTimeout(refreshTimerId);
      refreshTimerId = null;
    }
  }

  /** Refresh access token using refresh token */
  async function refreshAccessToken() {
    const refreshTokenValue = localStg.get('refreshToken');

    if (!refreshTokenValue) return;

    try {
      const { data, error } = await fetchRefreshToken(refreshTokenValue);

      if (!error && data) {
        const newAccessToken = data.accessToken || (data as any).token;
        const newRefreshToken = data.refreshToken;

        if (newAccessToken) {
          // Update tokens in storage
          localStg.set('token', newAccessToken);
          if (newRefreshToken) {
            localStg.set('refreshToken', newRefreshToken);
          }

          // Update token in store
          token.value = newAccessToken;

          // Reschedule next refresh
          scheduleTokenRefresh();
        }
      }
    } catch {
      // Let 401 handler deal with it
    }
  }

  /**
   * Logout
   *
   * Calls the backend logout API, clears local auth state, and redirects to login page.
   */
  async function logout() {
    const refreshTokenValue = localStg.get('refreshToken');

    // Notify backend about logout (fire and forget - don't block on failure)
    try {
      await fetchLogout(refreshTokenValue || undefined);
    } catch {
      // Ignore errors - proceed with local cleanup
    }

    // Clear local state and redirect
    await resetStore();
  }

  /** Reset auth store */
  async function resetStore() {
    recordUserId();

    cancelTokenRefresh();
    clearAuthStorage();

    authStore.$reset();

    if (!route.meta.constant) {
      await toLogin();
    }

    tabStore.cacheTabs();
    routeStore.resetStore();
  }

  /** Record the user ID of the previous login session Used to compare with the current user ID on next login */
  function recordUserId() {
    if (!userInfo.userId) {
      return;
    }

    // Store current user ID locally for next login comparison
    localStg.set('lastLoginUserId', userInfo.userId);
  }

  /**
   * Check if current login user is different from previous login user If different, clear all tabs
   *
   * @returns {boolean} Whether to clear all tabs
   */
  function checkTabClear(): boolean {
    if (!userInfo.userId) {
      return false;
    }

    const lastLoginUserId = localStg.get('lastLoginUserId');

    // Clear all tabs if current user is different from previous user
    if (!lastLoginUserId || lastLoginUserId !== userInfo.userId) {
      localStg.remove('globalTabs');
      tabStore.clearTabs();

      localStg.remove('lastLoginUserId');
      return true;
    }

    localStg.remove('lastLoginUserId');
    return false;
  }

  /**
   * Login
   *
   * @param userName User name
   * @param password Password
   * @param captchaCode Captcha code (optional)
   * @param captchaId Captcha ID/uuid (optional)
   * @param [redirect=true] Whether to redirect after login. Default is `true`
   */
  async function login(
    userName: string,
    password: string,
    captchaCode?: string,
    captchaUuid?: string,
    rememberMe?: boolean,
    redirect = true
  ) {
    startLoading();

    const params: Api.Auth.LoginParams = { username: userName, password, captchaCode, captchaUuid, rememberMe };
    const { data: loginToken, error } = await fetchLogin(params);

    if (!error) {
      const pass = await loginByToken(loginToken);

      if (pass) {
        // Check if the tab needs to be cleared
        const isClear = checkTabClear();
        let needRedirect = redirect;

        if (isClear) {
          // If the tab needs to be cleared,it means we don't need to redirect.
          needRedirect = false;
        }
        await redirectFromLogin(needRedirect);

        // Start proactive token refresh after successful login
        scheduleTokenRefresh();

        window.$notification?.success({
          title: $t('page.login.common.loginSuccess'),
          content: $t('page.login.common.welcomeBack', { userName: userInfo.userName }),
          duration: 4500
        });
      }
    } else {
      resetStore();
    }

    endLoading();
  }

  /**
   * Login by phone
   *
   * @param phone Phone number
   * @param code Verification code
   * @param [redirect=true] Whether to redirect after login. Default is `true`
   */
  async function loginByPhone(phone: string, code: string, rememberMe?: boolean, redirect = true) {
    startLoading();

    const params: Api.Auth.LoginByPhoneParams = { phone, verifyCode: code, rememberMe };
    const { data: loginToken, error } = await fetchLoginByPhone(params);

    if (!error) {
      const pass = await loginByToken(loginToken);

      if (pass) {
        const isClear = checkTabClear();
        let needRedirect = redirect;

        if (isClear) {
          needRedirect = false;
        }
        await redirectFromLogin(needRedirect);

        scheduleTokenRefresh();

        window.$notification?.success({
          title: $t('page.login.common.loginSuccess'),
          content: $t('page.login.common.welcomeBack', { userName: userInfo.userName }),
          duration: 4500
        });
      }
    } else {
      resetStore();
    }

    endLoading();
  }

  async function loginByToken(loginToken: Api.Auth.LoginToken) {
    // 1. stored in the localStorage, the later requests need it in headers
    const accessToken = loginToken.accessToken || (loginToken as any).token;
    localStg.set('token', accessToken);
    localStg.set('refreshToken', loginToken.refreshToken);

    // 2. get user info
    const pass = await getUserInfo();

    if (pass) {
      token.value = accessToken;

      return true;
    }

    return false;
  }

  async function getUserInfo() {
    const { data: info, error } = await fetchGetUserInfo();

    if (!error) {
      // The backend returns a single `role` field (e.g., 'admin' or 'user')
      // Map it to roles array for soybean-admin compatibility
      if (info && typeof (info as any).role === 'string' && !(info as any).roles) {
        (info as any).roles = [(info as any).role];
        delete (info as any).role;
      }

      // update store
      Object.assign(userInfo, info);

      return true;
    }

    return false;
  }

  async function initUserInfo() {
    const maybeToken = getToken();

    if (maybeToken) {
      token.value = maybeToken;
      const pass = await getUserInfo();

      if (pass) {
        // Start proactive token refresh after restoring session
        scheduleTokenRefresh();
      } else {
        // Clear auth state without triggering navigation — the router guard
        // will detect isLogin=false and redirect to login. Calling resetStore()
        // here would trigger toLogin() during beforeEach, causing competing
        // navigations and potential white screen.
        cancelTokenRefresh();
        token.value = '';
        clearAuthStorage();
      }
    }
  }

  return {
    token,
    userInfo,
    isStaticSuper,
    isLogin,
    loginLoading,
    resetStore,
    login,
    loginByPhone,
    logout,
    initUserInfo,
    scheduleTokenRefresh,
    cancelTokenRefresh
  };
});
