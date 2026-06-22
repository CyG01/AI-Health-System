import type { LocationQueryRaw, RouteLocationNormalized, RouteLocationRaw, Router } from 'vue-router';
import { useAuthStore } from '@/store/modules/auth';
import { clearAuthStorage } from '@/store/modules/auth/shared';
import { useRouteStore } from '@/store/modules/route';
import { getRouteName } from '@/router/elegant/transform';

/**
 * create route guard
 *
 * @param router router instance
 */
export function createRouteGuard(router: Router) {
  router.beforeEach(async (to, from) => {
    try {
      const location = await initRoute(to);

      if (location) {
        return location;
      }

      const authStore = useAuthStore();

      const rootRoute = 'root';
      const loginRoute = 'login';
      const noAuthorizationRoute = '403';

      const isLogin = authStore.isLogin;
      const needLogin = !to.meta.constant;
      const routeRoles = (to.meta.roles as string[]) || [];

      const hasRole = authStore.userInfo.roles.some(role => routeRoles.includes(role));
      const hasAuth = authStore.isStaticSuper || !routeRoles.length || hasRole;

      // if it is login route when logged in, then switch to the root page
      if (to.name === loginRoute && isLogin) {
        return { name: rootRoute };
      }

      // if the route does not need login, then it is allowed to access directly
      if (!needLogin) {
        return handleRouteSwitch(to, from);
      }

      // the route need login but the user is not logged in, then switch to the login page
      if (!isLogin) {
        return { name: loginRoute, query: { redirect: to.fullPath } };
      }

      // if the user is logged in but does not have authorization, then switch to the 403 page
      if (!hasAuth) {
        return { name: noAuthorizationRoute };
      }

      // Schedule proactive token refresh on protected page visit
      authStore.scheduleTokenRefresh();

      // switch route normally
      return handleRouteSwitch(to, from);
    } catch (error) {
      console.error('[RouteGuard] Unexpected error in beforeEach:', error);
      // Safety net: always allow navigation to proceed (redirect to login if needed)
      // This prevents the app from being stuck on a white screen
      return { name: 'login', query: { redirect: to.fullPath } };
    }
  });
}

/**
 * initialize route
 *
 * @param to to route
 */
async function initRoute(to: RouteLocationNormalized): Promise<RouteLocationRaw | null> {
  const routeStore = useRouteStore();
  const authStore = useAuthStore();

  const notFoundRoute = 'not-found';
  const isNotFoundRoute = to.name === notFoundRoute;

  // if the constant route is not initialized, then initialize the constant route
  if (!routeStore.isInitConstantRoute) {
    try {
      await routeStore.initConstantRoute();
    } catch (error) {
      console.error('[RouteGuard] initConstantRoute failed:', error);
      // Constant routes are essential — if they fail, redirect to login as a fallback
      return { name: 'login' };
    }

    // the route is captured by the "not-found" route because the constant route is not initialized
    // after the constant route is initialized, redirect to the original route
    const path = to.fullPath;
    const location: RouteLocationRaw = {
      path,
      replace: true,
      query: to.query,
      hash: to.hash
    };

    return location;
  }

  const isLogin = authStore.isLogin;

  if (!isLogin) {
    // if the user is not logged in and the route is a constant route but not the "not-found" route, then it is allowed to access.
    if (to.meta.constant && !isNotFoundRoute) {
      routeStore.onRouteSwitchWhenNotLoggedIn();

      return null;
    }

    // if the user is not logged in, then switch to the login page
    const loginRoute = 'login';
    const query = getRouteQueryOfLoginRoute(to, routeStore.routeHome);

    const location: RouteLocationRaw = {
      name: loginRoute,
      query
    };

    return location;
  }

  if (!routeStore.isInitAuthRoute) {
    // initialize the auth route (fetches user info, starts token refresh timer)
    try {
      await routeStore.initAuthRoute();
    } catch (error) {
      console.error('[RouteGuard] initAuthRoute failed:', error);
      // Clear token on failure so the guard will redirect to login below
      clearAuthStorage();
      authStore.$reset();
      // Do NOT call toLogin() here — let the guard's own redirect logic handle it
      // to avoid competing navigations during beforeEach execution
    }

    // Re-check login state after initAuthRoute (token may have been cleared)
    if (!authStore.isLogin) {
      const loginRoute = 'login';
      const query = getRouteQueryOfLoginRoute(to, routeStore.routeHome);
      return { name: loginRoute, query };
    }

    // the route is captured by the "not-found" route because the auth route is not initialized
    // after the auth route is initialized, redirect to the original route
    if (isNotFoundRoute) {
      const rootRoute = 'root';
      const path = to.redirectedFrom?.name === rootRoute ? '/' : to.fullPath;

      const location: RouteLocationRaw = {
        path,
        replace: true,
        query: to.query,
        hash: to.hash
      };

      return location;
    }
  }

  routeStore.onRouteSwitchWhenLoggedIn();

  // the auth route is initialized
  // it is not the "not-found" route, then it is allowed to access
  if (!isNotFoundRoute) {
    return null;
  }

  // it is captured by the "not-found" route, then check whether the route exists
  const exist = await routeStore.getIsAuthRouteExist(to.path as string);
  const noPermissionRoute = '403';

  if (exist) {
    const location: RouteLocationRaw = {
      name: noPermissionRoute
    };

    return location;
  }

  return null;
}

function handleRouteSwitch(to: RouteLocationNormalized, from: RouteLocationNormalized) {
  // route with href
  if (to.meta.href) {
    window.open(to.meta.href as string, '_blank');

    return { path: from.fullPath, replace: true, query: from.query, hash: to.hash };
  }
}

function getRouteQueryOfLoginRoute(to: RouteLocationNormalized, routeHome: string) {
  const loginRoute = 'login';
  const redirect = to.fullPath;
  const [redirectPath, redirectQuery] = redirect.split('?');
  const redirectName = getRouteName(redirectPath);

  const isRedirectHome = routeHome === redirectName;

  const query: LocationQueryRaw = to.name !== loginRoute && !isRedirectHome ? { redirect } : {};

  if (isRedirectHome && redirectQuery) {
    query.redirect = `/?${redirectQuery}`;
  }

  return query;
}
