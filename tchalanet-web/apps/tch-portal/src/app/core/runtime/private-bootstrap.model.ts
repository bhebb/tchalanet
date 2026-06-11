export type PrivateSpace = 'PLATFORM' | 'ADMIN' | 'CASHIER';

export type BootstrapStatus = 'idle' | 'loading' | 'ready' | 'partial' | 'error';

export interface RuntimeBootstrapResponse {
  readonly space: PrivateSpace;
  readonly user: AuthenticatedUserView;
  readonly tenantContext: TenantContextView | null;
  readonly settings: RuntimeSettingsView;
  readonly theme: RuntimeThemeView;
  readonly i18n: RuntimeI18nBundle;
  readonly entitlements: EntitlementsView;
  readonly readiness: RuntimeReadinessView;
  readonly notifications: RuntimeNotificationSummary;
  readonly pageModelRef: PageModelRef;
  readonly notices?: readonly RuntimeBootstrapNotice[] | null;
}

export interface AuthenticatedUserView {
  readonly userId: string | null;
  readonly username: string | null;
  readonly displayName: string | null;
  readonly email: string | null;
  readonly roles: readonly string[];
  readonly defaultSpace: PrivateSpace;
  readonly preferredLocale: string | null;
  readonly preferredTimezone: string | null;
}

export interface TenantContextView {
  readonly tenantId: string;
  readonly tenantCode: string | null;
  readonly tenantName: string | null;
}

export interface RuntimeSettingsView {
  readonly locale: string;
  readonly timezone: string;
  readonly currency: string;
  readonly features: Readonly<Record<string, boolean>>;
}

export interface RuntimeThemeView {
  readonly presetCode: string;
  readonly mode: string;
  readonly tokens: Readonly<Record<string, string>>;
  readonly isDefault: boolean;
  readonly version: number;
}

export interface RuntimeI18nBundle {
  readonly locale: string;
  readonly messages: Readonly<Record<string, string>>;
}

export interface EntitlementsView {
  readonly roles: readonly string[];
  readonly permissions: readonly string[];
}

export type ReadinessStatus = 'READY' | 'PARTIAL' | 'BLOCKED';
export type CheckStatus = 'READY' | 'MISSING' | 'BLOCKED' | 'WARNING';

export interface RuntimeReadinessView {
  readonly status: ReadinessStatus;
  readonly checks: readonly RuntimeReadinessCheck[];
}

export interface RuntimeReadinessCheck {
  readonly code: string;
  readonly labelKey: string;
  readonly status: CheckStatus;
}

export interface RuntimeNotificationSummary {
  readonly unreadCount: number;
  readonly criticalCount: number;
}

export interface PageModelRef {
  readonly route: string;
  readonly endpoint: string;
}

export interface RuntimeBootstrapNotice {
  readonly code: string;
  readonly message: string;
  readonly level: 'INFO' | 'WARNING' | 'ERROR';
}
