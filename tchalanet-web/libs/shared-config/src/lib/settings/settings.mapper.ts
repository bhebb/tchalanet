import { RuntimeSettings } from '../contracts/runtime.types';

/**
 * Default runtime settings used before the runtime bootstrap response is applied.
 * Settings are delivered inside the bootstrap payload (see
 * `RuntimeSettingsStore.applyBootstrapSettings`), so there is no separate settings HTTP client.
 */
export const defaultRuntimeSettings: RuntimeSettings = {
  featureFlags: {},
  values: {},
};
