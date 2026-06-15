// NOTE: This project uses static route mode (soybean-admin).
// Routes are generated at build time by @elegant-router/vue plugin and managed
// in the route store (store/modules/route) via createStaticRoutes().
//
// The backend does NOT provide any route-related API endpoints
// (no RouteController exists). The original soybean-admin route API functions
// (getConstantRoutes, getUserRoutes, isRouteExist) have been removed since
// they would call non-existent endpoints.
//
// If dynamic route loading from the backend is needed in the future,
// implement a RouteController and re-add the corresponding service functions here.

export {};
