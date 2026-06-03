import { ActionItem } from './navigation.types';

export type PageWidgetType = 'hero' | 'text' | 'notice' | 'action-list' | 'custom';

export interface PageModel {
  readonly pageKey: string;
  readonly titleKey?: string;
  readonly sections: readonly PageSection[];
  readonly version?: string;
}

export interface PageSection {
  readonly id: string;
  readonly titleKey?: string;
  readonly widgets: readonly PageWidget[];
}

export interface PageWidget {
  readonly id: string;
  readonly type: PageWidgetType;
  readonly titleKey?: string;
  readonly textKey?: string;
  readonly actions?: readonly ActionItem[];
  readonly data?: Readonly<Record<string, unknown>>;
}
