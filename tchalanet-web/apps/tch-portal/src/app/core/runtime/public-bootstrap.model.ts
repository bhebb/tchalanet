import { PageModelRef, RuntimeBootstrapNotice } from './private-bootstrap.model';

/**
 * Public runtime startup payload returned by `GET /public/runtime/bootstrap`.
 * Mirrors the backend `PublicBootstrapResponse`. No private/user data.
 */
export interface PublicBootstrapResponse {
  readonly settings: PublicSettingsView;
  readonly theme: PublicThemeView;
  readonly i18n: PublicI18nBundle;
  readonly navigation: PublicNavigationModel;
  readonly readiness: PublicReadinessView;
  readonly pageModelRef: PageModelRef;
  readonly notices?: readonly RuntimeBootstrapNotice[] | null;
}

export interface PublicSettingsView {
  readonly locale: string;
  readonly timezone: string;
  readonly supportedLocales: readonly string[];
  readonly defaultCurrency: string;
  readonly features: Readonly<Record<string, boolean>>;
}

export interface PublicThemeView {
  readonly scope: string;
  readonly mode: string;
  readonly primaryColor: string;
  readonly secondaryColor: string;
  readonly logoUrl: string | null;
  readonly faviconUrl: string | null;
}

export interface PublicI18nBundle {
  readonly lang: string;
  readonly messages: Readonly<Record<string, string>>;
  readonly loadedAt: string;
}

export interface PublicNavigationModel {
  readonly items: readonly PublicNavigationItem[];
  readonly footerItems?: readonly PublicNavigationItem[] | null;
}

export interface PublicNavigationItem {
  readonly id: string;
  readonly labelKey: string;
  readonly route: string;
  readonly external?: boolean | null;
}

export type PublicReadinessStatus = 'READY' | 'PARTIAL';
export type PublicReadinessCheckStatus = 'READY' | 'WARNING';

export interface PublicReadinessView {
  readonly status: PublicReadinessStatus;
  readonly checks: readonly PublicReadinessCheck[];
}

export interface PublicReadinessCheck {
  readonly code: string;
  readonly status: PublicReadinessCheckStatus;
  readonly message?: string | null;
}
