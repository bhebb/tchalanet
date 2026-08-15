import { PageModelRef, RuntimeBootstrapNotice } from '@tch/api';

export type PrivateSpace = 'PLATFORM' | 'ADMIN' | 'CASHIER';

export interface RuntimeBootstrapResponse {
  readonly space: PrivateSpace;
  readonly user: AuthenticatedUserView;
  readonly tenantContext: TenantContextView | null;
  readonly settings?: RuntimeSettingsView | null;
  readonly theme?: RuntimeThemeView | null;
  readonly i18n?: RuntimeI18nBundle | null;
  readonly entitlements: EntitlementsView;
  readonly readiness: RuntimeReadinessView;
  readonly notifications: RuntimeNotificationSummary;
  readonly navigationDrawer?: RuntimeNavigationDrawer | null;
  readonly pageModelRef: PageModelRef;
  readonly entryRoute?: string | null;
  readonly portalConfig?: RuntimePortalConfigView | null;
  readonly notices?: readonly RuntimeBootstrapNotice[] | null;
}

export interface RuntimePortalConfigView {
  readonly portalBaseUrls?: Readonly<Partial<Record<'admin-portal' | 'platform-portal', string>>> | null;
}

/**
 * The resolver that actually serves this payload (`PrivateShellNavigationResolver`, backend)
 * passes the JSON fragment through untyped — it does not go through the `NavigationDrawer` Java
 * record that names these fields `topDestinations`/`footerDestinations`. The fragment files on
 * disk use `primary`/`secondary` instead. Both names are declared here and read defensively by
 * `runtime-navigation.mapper.ts`, so this keeps working whether or not the backend is ever wired
 * to the record's naming.
 */
export interface RuntimeNavigationDrawer {
  readonly brand?: unknown;
  readonly primary?: readonly RuntimeNavigationEntry[];
  readonly topDestinations?: readonly RuntimeNavigationEntry[];
  readonly sections?: readonly RuntimeNavigationSection[];
  readonly secondary?: readonly RuntimeNavigationEntry[];
  readonly footerDestinations?: readonly RuntimeNavigationEntry[];
  readonly actions?: readonly RuntimeNavigationEntry[];
}

export interface RuntimeNavigationSection {
  readonly id?: string | null;
  readonly label_key?: string | null;
  readonly labelKey?: string | null;
  readonly titleKey?: string | null;
  readonly label?: string | null;
  readonly items?: readonly RuntimeNavigationEntry[] | null;
}

export interface RuntimeNavigationEntry {
  readonly id?: string | null;
  readonly type?: string | null;
  readonly label_key?: string | null;
  readonly labelKey?: string | null;
  readonly label?: string | null;
  readonly destination?: RuntimeNavigationDestination | null;
  readonly path?: string | null;
  readonly kind?: string | null;
  readonly icon?: string | null;
  readonly active_routes?: readonly string[] | null;
  readonly activeRoutes?: readonly string[] | null;
  readonly active_match?: string | null;
  readonly activeMatch?: string | null;
  readonly disabled?: boolean | null;
  readonly reason_key?: string | null;
  readonly reasonKey?: string | null;
  readonly badge?: unknown;
  readonly children?: readonly RuntimeNavigationEntry[] | null;
}

export interface RuntimeNavigationDestination {
  readonly kind?: string | null;
  readonly value?: string | null;
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
  readonly mustChangePassword?: boolean;
  readonly mustCompleteProfile?: boolean;
  readonly firstLoginCompletedAt?: string | null;
  readonly temporaryCredentialIssuedAt?: string | null;
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
