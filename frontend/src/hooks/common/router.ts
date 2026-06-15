import { ref } from 'vue';
import { useRouter } from 'vue-router';
import type { RouteLocationRaw } from 'vue-router';
import { router } from '@/router';
import { getRoutePath } from '@/router/elegant/transform';

export type LoginModule = 'pwd-login' | 'code-login' | 'register' | 'reset-pwd' | 'bind-wechat';

/**
 * Router push hook
 *
 * @param [isVueRouter=false] Whether to use the Vue Router instance from the current component.
 *   Set to `true` when called inside a component setup, `false` when called from a store.
 */
export function useRouterPush(isVueRouter = true) {
  const vueRouter = useRouter();
  const routerInstance = isVueRouter ? vueRouter : router;

  const loginModule = ref<LoginModule>('pwd-login');

  function toggleLoginModule(module: LoginModule) {
    loginModule.value = module;
  }

  /**
   * Router push
   *
   * @param to Route location
   * @returns Promise that resolves to `null` on success, or the error on failure
   */
  async function routerPush(to: RouteLocationRaw) {
    try {
      await routerInstance.push(to);
      return null;
    } catch (error) {
      return error;
    }
  }

  /**
   * Router push by route key
   *
   * @param key Route key
   * @param options Additional options
   */
  async function routerPushByKey(
    key: string,
    options?: { query?: Record<string, string>; params?: Record<string, string> }
  ) {
    const path = getRoutePath(key);

    if (!path) {
      console.warn(`Route key "${key}" not found in route map`);
      return null;
    }

    const to: RouteLocationRaw = { path };

    if (options?.query) {
      (to as any).query = options.query;
    }

    return routerPush(to);
  }

  /**
   * Navigate to login page
   *
   * @param [redirect=true] Whether to include redirect query param
   */
  async function toLogin(redirect = true) {
    const currentPath = routerInstance.currentRoute.value.fullPath;
    const query: Record<string, string> = {};

    if (redirect && currentPath !== '/login') {
      query.redirect = currentPath;
    }

    return routerPush({ name: 'login', query });
  }

  /**
   * Redirect from login page after successful authentication
   *
   * @param [redirect=true] Whether to redirect to the original page
   */
  async function redirectFromLogin(redirect = true) {
    const currentRoute = routerInstance.currentRoute.value;
    const redirectPath = (currentRoute.query.redirect as string) || '/';

    if (redirect && redirectPath && redirectPath !== '/login') {
      return routerPush(redirectPath);
    }

    const homeRoute = import.meta.env.VITE_ROUTE_HOME || 'dashboard';
    const homePath = getRoutePath(homeRoute) || '/dashboard';

    return routerPush(homePath);
  }

  return {
    loginModule,
    toggleLoginModule,
    routerPush,
    routerPushByKey,
    toLogin,
    redirectFromLogin
  };
}
