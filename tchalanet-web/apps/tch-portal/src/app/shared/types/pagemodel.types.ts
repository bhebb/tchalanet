/**
 * Web mirror of the backend `PageModelDoc` contract
 * (`com.tchalanet.server.core.pagemodel.api.model.PageModelDoc`).
 *
 * Field names follow the backend JSON wire format verbatim (snake_case for keys such as
 * `label_key`, `schema_version`, `default_lang`). The renderer types the real payload directly;
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
  readonly header?: ShellSectionConfig;
  readonly sidenav?: ShellSectionConfig;
  readonly footer?: ShellSectionConfig;
}

export interface ShellSectionConfig {
  readonly component?: string;
  readonly binding?: WidgetBinding;
  readonly nav?: ShellNav;
  readonly props?: Readonly<Record<string, unknown>>;
}

export interface ShellNav {
  readonly primary?: readonly NavItem[];
  readonly secondary?: readonly NavItem[];
}

export interface NavItem {
  readonly label_key?: string;
  readonly path?: string;
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
  readonly label_key?: string;
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
