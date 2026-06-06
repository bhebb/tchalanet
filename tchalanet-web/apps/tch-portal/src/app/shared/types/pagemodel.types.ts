import { ActionItem, NavigationSection } from './navigation.types';

/** Resolved PageModel BFF contract. It contains no storage bindings or fragment keys. */
export interface PageRuntimeResponse {
  readonly meta: PageMeta;
  readonly theme?: PageThemeHint;
  readonly shell: PageShellRuntime;
  readonly content: PageContentRuntime;
  readonly dynamic: PageDynamicPayload;
}

export interface PageMeta {
  readonly logicalId: string;
  readonly scope: string;
  readonly slug: string;
  readonly schemaVersion: number;
}

export interface PageThemeHint {
  readonly presetId?: string;
  readonly mode?: string;
  readonly density?: number;
}

export type PageShellRuntime = PublicShellRuntime | PrivateShellRuntime;

export interface PublicShellRuntime {
  readonly type: 'public';
  readonly header: PublicHeaderRuntime;
  readonly footer: PublicFooterRuntime;
}

export interface PublicHeaderRuntime {
  readonly brand?: ActionItem;
  readonly primary?: readonly ActionItem[];
  readonly utilities?: readonly ActionItem[];
  readonly secondary?: readonly ActionItem[];
  readonly actions?: readonly ActionItem[];
}

export interface PublicFooterRuntime {
  readonly brand?: ActionItem;
  readonly descriptionKey?: string;
  readonly statusKey?: string;
  readonly copyrightKey?: string;
  readonly columns?: readonly PublicFooterColumn[];
  readonly social?: readonly ActionItem[];
}

export interface PublicFooterColumn {
  readonly id?: string;
  readonly titleKey: string;
  readonly links: readonly ActionItem[];
}

export interface PrivateShellRuntime {
  readonly type: 'private';
  readonly topAppBar: PrivateTopAppBarRuntime;
  readonly navigationDrawer: NavigationDrawerRuntime;
}

export interface PrivateTopAppBarRuntime {
  readonly titleKey?: string;
  readonly utilities?: readonly ActionItem[];
  readonly actions?: readonly ActionItem[];
}

export interface NavigationDrawerRuntime {
  readonly brand?: ActionItem;
  readonly primary?: readonly ActionItem[];
  readonly sections?: readonly NavigationSection[];
  readonly secondary?: readonly ActionItem[];
}

export interface PageContentRuntime {
  readonly layout: PageLayout;
  readonly widgets: Readonly<Record<string, WidgetConfig>>;
}

export interface PageLayout {
  readonly rows: readonly LayoutRow[];
}

export interface LayoutRow {
  readonly id?: string;
  readonly labelKey?: string;
  readonly columns: readonly LayoutColumn[];
}

export interface LayoutColumn {
  readonly span: number;
  readonly widgets: readonly string[];
}

export interface WidgetConfig {
  readonly type: string;
  readonly props?: Readonly<Record<string, unknown>>;
}

export interface PageDynamicPayload {
  readonly widgets: Readonly<Record<string, unknown>>;
  readonly errors: readonly WidgetDynamicError[];
}

export interface WidgetDynamicError {
  readonly widgetId: string;
  readonly code?: string;
}
