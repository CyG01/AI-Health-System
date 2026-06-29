import type { PiniaPluginContext } from 'pinia';

/**
 * Reset store plugin
 *
 * This plugin adds a $reset method to setup stores, which is not available by default in Pinia setup stores.
 */
export function resetSetupStore({ store }: PiniaPluginContext) {
  const initialState = JSON.parse(JSON.stringify(store.$state));

  store.$reset = () => {
    const keys = Object.keys(initialState);

    keys.forEach(key => {
      const value = initialState[key];

      (store.$state as Record<string, any>)[key] = value;
    });
  };
}
