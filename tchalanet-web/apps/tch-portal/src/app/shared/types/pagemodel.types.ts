import { ActionItem } from './navigation.types';

/**
 * Web mirror of the backend `PageModelDoc` contract
 * (`com.tchalanet.server.core.pagemodel.api.model.PageModelDoc`).
 *
 * Field names follow the backend JSON wire format. PageModel metadata keeps historical
 * snake_case keys such as `schema_version` and `default_lang`; UI/action contracts use camelCase.
 * The renderer types the real payload directly;
 * there is no abstract widget vocabulary or legacy-mapping layer.
 */

export interface PageModelDoc {
  readonly meta: PageModelMeta;
  readonly theme?: PageTheme;
  readonly shell?: PageShell;
  readonly content?: PageContent;
}

export interface PageModelMeta {
  readonly id: string;
  readonly scope?: string;
  readonly slug?: string;
  readonly context?: string;
  readonly schema_version: number;
  readonly langs?: readonly string[];
  readonly default_lang?: string;
}

export interface PageTheme {
  readonly presetId?: string;
  readonly mode?: string;
  readonly density?: number;
  readonly overrides?: Readonly<Record<string, string>>;
}

export interface PageShell {
  readonly brand?: ActionItem;
  readonly primary?: readonly ActionItem[];
  readonly actions?: readonly ActionItem[];
  readonly mobile?: readonly ActionItem[];
  readonly header?: ShellSectionConfig;
  readonly sidenav?: ShellSectionConfig;
  readonly footer?: ShellSectionConfig;
}

export interface ShellSectionConfig {
  readonly component?: string;
  readonly binding?: WidgetBinding;
  readonly brand?: TchBrand;
  readonly nav?: ShellNav;
  readonly props?: Readonly<Record<string, unknown>>;
}

export interface ShellNav {
  readonly primary?: readonly ActionItem[];
  readonly secondary?: readonly ActionItem[];
}
export interface TchBrand {
  readonly image?: string;
  readonly name?: string;
  readonly secondary?: readonly ActionItem[];
}

export interface PageContent {
  readonly layout?: PageLayout;
  readonly widgets?: Readonly<Record<string, WidgetConfig>>;
}

export interface PageLayout {
  readonly component?: string;
  readonly rows?: readonly LayoutRow[];
}

export interface LayoutRow {
  readonly id?: string;
  readonly labelKey?: string;
  readonly columns?: readonly LayoutColumn[];
}

export interface LayoutColumn {
  readonly span: number;
  readonly widgets?: readonly string[];
}

export interface WidgetConfig {
  readonly type: string;
  readonly binding?: WidgetBinding;
  readonly props?: Readonly<Record<string, unknown>>;
}

export interface WidgetBinding {
  readonly mode?: string;
  readonly source?: string;
}

/** Mirror of backend `PageDynamicPayload`: resolved widget payloads keyed by widget id + errors. */
export interface PageDynamicPayload {
  readonly widgets?: Readonly<Record<string, unknown>>;
  readonly errors?: readonly WidgetDynamicError[];
}

/** Mirror of backend `WidgetDynamicError`: a contained, widget-local provider failure. */
export interface WidgetDynamicError {
  readonly widgetId: string;
  readonly provider?: string;
  readonly code?: string;
  readonly message?: string;
}

/** Mirror of backend `PublicPageModelResponse`. */
export interface PublicPageModelResponse {
  readonly currentLang?: string;
  readonly langs?: readonly string[];
  readonly pageModel: PageModelDoc;
  readonly dynamic?: PageDynamicPayload;
}

/** Mirror of backend `DashboardPageModelResponse` (adds notification summary). */
export interface DashboardPageModelResponse {
  readonly currentLang?: string;
  readonly langs?: readonly string[];
  readonly pageModel: PageModelDoc;
  readonly dynamic?: PageDynamicPayload;
  readonly notifications?: unknown;
}
