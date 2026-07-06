import { computed, Injectable, signal } from '@angular/core';

import { FeatureFlag, RuntimeSettings } from '../contracts/runtime.types';
import { defaultRuntimeSettings } from './settings.mapper';

export const TENANT_DASHBOARD_SETTINGS_WIDGET_KEY = 'tenant.settings';

/** Settings carried inside the runtime bootstrap response (no separate settings HTTP call). */
export interface BootstrapSettingsInput {
  readonly locale: string;
  readonly timezone: string;
  readonly currency: string;
  readonly features: Readonly<Record<string, boolean>>;
}

export interface TenantDashboardSettingsInput {
  readonly tenantCode?: string | null;
  readonly displayName?: string | null;
  readonly timezone?: string | null;
  readonly currency?: string | null;
  readonly defaultLanguage?: string | null;
  readonly defaultLocale?: string | null;
  readonly supportedLanguages?: readonly string[] | null;
  readonly supportedCurrencies?: readonly string[] | null;
  readonly fallbackLanguage?: string | null;
  readonly settings?: unknown;
}

export interface TenantRuntimeOption {
  readonly code: string;
  readonly label: string;
}

type SettingsLoadState = 'idle' | 'loading' | 'ready' | 'fallback';

const LANGUAGE_LABELS: Readonly<Record<string, string>> = {
  fr: 'Français',
  ht: 'Kreyòl Ayisyen',
  en: 'English',
};

const CURRENCY_LABELS: Readonly<Record<string, string>> = {
  HTG: 'HTG',
  USD: 'USD',
  CAD: 'CAD',
};

@Injectable({ providedIn: 'root' })
export class RuntimeSettingsStore {
  private readonly settingsState = signal<RuntimeSettings>(defaultRuntimeSettings);
  private readonly loadStateState = signal<SettingsLoadState>('idle');

  readonly settings = this.settingsState.asReadonly();
  readonly loadState = this.loadStateState.asReadonly();
  readonly ready = computed(() => this.loadState() === 'ready' || this.loadState() === 'fallback');
  readonly tenantSettings = computed(() => this.settings().values[TENANT_DASHBOARD_SETTINGS_WIDGET_KEY]);
  readonly tenantCurrency = computed(() => stringValue(this.settings().values['tenant.currency'])
    ?? stringValue(this.settings().values['app.currency'])
    ?? 'HTG');
  readonly tenantSupportedLanguages = computed(() => {
    const values = stringArrayValue(this.settings().values['tenant.supportedLanguages']);
    return values.length ? values : ['fr'];
  });
  readonly tenantSupportedCurrencies = computed(() => {
    const values = stringArrayValue(this.settings().values['tenant.supportedCurrencies']);
    if (values.length) return values;
    const currency = this.tenantCurrency();
    return currency ? [currency] : ['HTG'];
  });
  readonly tenantLanguageOptions = computed(() => optionsFor(this.tenantSupportedLanguages(), LANGUAGE_LABELS));
  readonly tenantCurrencyOptions = computed(() => optionsFor(this.tenantSupportedCurrencies(), CURRENCY_LABELS));

  /** Apply settings delivered inside the runtime bootstrap — no extra settings HTTP call. */
  applyBootstrapSettings(input: BootstrapSettingsInput): void {
    const featureFlags: Record<string, FeatureFlag> = {};
    const values: Record<string, unknown> = {
      'app.locale': input.locale,
      'app.timezone': input.timezone,
      'app.currency': input.currency,
    };
    for (const [key, enabled] of Object.entries(input.features ?? {})) {
      featureFlags[key] = { key, enabled };
      values[key] = enabled;
    }
    this.settingsState.set({ featureFlags, values, loadedAt: new Date().toISOString() });
    this.loadStateState.set('ready');
  }

  applyTenantDashboardSettings(input: TenantDashboardSettingsInput | null | undefined): void {
    if (!input) {
      return;
    }

    const current = this.settingsState();
    const values: Record<string, unknown> = { ...current.values };
    setIfDefined(values, 'tenant.code', input.tenantCode);
    setIfDefined(values, 'tenant.displayName', input.displayName);
    setIfDefined(values, 'tenant.timezone', input.timezone);
    setIfDefined(values, 'tenant.currency', input.currency);
    setIfDefined(values, 'tenant.defaultLanguage', input.defaultLanguage);
    setIfDefined(values, 'tenant.defaultLocale', input.defaultLocale);
    setIfDefined(values, 'tenant.supportedLanguages', input.supportedLanguages);
    setIfDefined(values, 'tenant.supportedCurrencies', input.supportedCurrencies);
    setIfDefined(values, 'tenant.fallbackLanguage', input.fallbackLanguage);
    setIfDefined(values, TENANT_DASHBOARD_SETTINGS_WIDGET_KEY, input.settings);

    this.settingsState.set({
      ...current,
      values,
      loadedAt: new Date().toISOString(),
    });
    if (this.loadStateState() === 'idle' || this.loadStateState() === 'loading') {
      this.loadStateState.set('ready');
    }
  }

  isFeatureEnabled(key: string, defaultValue = false): boolean {
    return this.settings().featureFlags[key]?.enabled ?? defaultValue;
  }
}

function setIfDefined(values: Record<string, unknown>, key: string, value: unknown): void {
  if (value !== undefined) {
    values[key] = value;
  }
}

function stringValue(value: unknown): string | null {
  return typeof value === 'string' && value.trim() ? value.trim() : null;
}

function stringArrayValue(value: unknown): readonly string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
    .map(item => item.trim());
}

function optionsFor(
  codes: readonly string[],
  labels: Readonly<Record<string, string>>,
): readonly TenantRuntimeOption[] {
  return codes.map(code => ({ code, label: labels[code] ?? code }));
}
