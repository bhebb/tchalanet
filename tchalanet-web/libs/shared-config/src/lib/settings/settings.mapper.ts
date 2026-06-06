import { FeatureFlag, RuntimeSettings } from '../contracts/runtime.types';
import { SettingsApiSetting } from './settings-api.service';

export type RuntimeSettingsSource = RuntimeSettings;

export const defaultRuntimeSettings: RuntimeSettings = {
  featureFlags: {},
  values: {},
};

export function toRuntimeSettings(settings: readonly SettingsApiSetting[]): RuntimeSettings {
  return settings.reduce<RuntimeSettings>(
    (runtime, setting) => {
      const key = fullSettingKey(setting);
      const value = parseSettingValue(setting);
      const values = {
        ...runtime.values,
        [key]: value,
      };
      const featureFlags =
        typeof value === 'boolean' && isFeatureFlagSetting(setting)
          ? {
              ...runtime.featureFlags,
              [key]: toFeatureFlag(key, value),
            }
          : runtime.featureFlags;

      return {
        featureFlags,
        values,
        loadedAt: runtime.loadedAt,
      };
    },
    {
      ...defaultRuntimeSettings,
      loadedAt: new Date().toISOString(),
    },
  );
}

function fullSettingKey(setting: SettingsApiSetting): string {
  return `${setting.namespace}.${setting.settingKey}`;
}

function isFeatureFlagSetting(setting: SettingsApiSetting): boolean {
  const namespace = setting.namespace.toLowerCase();
  return (
    namespace === 'feature' ||
    namespace.startsWith('feature.') ||
    namespace === 'features' ||
    namespace.startsWith('features.') ||
    namespace === 'feature_flags' ||
    namespace.startsWith('feature_flags.')
  );
}

function toFeatureFlag(key: string, enabled: boolean): FeatureFlag {
  return {
    key,
    enabled,
  };
}

function parseSettingValue(setting: SettingsApiSetting): unknown {
  switch (setting.valueType) {
    case 'BOOLEAN':
      return setting.settingValue.toLowerCase() === 'true';
    case 'INT':
    case 'LONG':
    case 'DECIMAL':
      return Number(setting.settingValue);
    case 'JSON':
      return parseJsonSafely(setting.settingValue);
    case 'STRING':
      return setting.settingValue;
  }
}

function parseJsonSafely(value: string): unknown {
  try {
    return JSON.parse(value) as unknown;
  } catch {
    return value;
  }
}
