export type ActionItemKind = 'button' | 'link' | 'externalLink' | string;
export type NavigationDestinationKind = 'route' | 'url';

export interface NavigationDestination {
  readonly kind: NavigationDestinationKind;
  readonly value: string;
  readonly requiredRoles?: readonly string[];
}

export interface ActionItem {
  readonly id: string;
  readonly kind?: ActionItemKind;
  readonly labelKey?: string;
  readonly label?: string | null;
  readonly destination?: NavigationDestination;
  readonly icon?: string | null;
  readonly image?: string | null;
  readonly activeMatch?: 'exact' | 'prefix' | string | null;
  readonly disabled?: boolean;
  readonly reasonKey?: string | null;
  readonly badge?: AdminNavBadge | null;
  readonly children?: readonly ActionItem[];
  readonly requiredPermissions?: readonly string[];
  readonly featureFlag?: string | null;
}

export interface AdminNavBadge {
  readonly kind: 'count' | 'status' | string;
  readonly value: string | number;
  readonly severity?: 'info' | 'success' | 'warning' | 'danger' | string;
}

export interface NavigationSection {
  readonly id: string;
  readonly titleKey: string;
  readonly items: readonly ActionItem[];
}

export function actionText(item: ActionItem | undefined): string {
  return item?.labelKey ?? item?.label ?? '';
}

export function actionRoute(item: ActionItem | undefined): string {
  if (item?.destination?.kind !== 'route') return '';
  const value = item.destination.value;
  const qIdx = value.indexOf('?');
  return qIdx >= 0 ? value.slice(0, qIdx) : value;
}

export function actionQueryParams(item: ActionItem | undefined): Record<string, string> | null {
  if (item?.destination?.kind !== 'route') return null;
  const value = item.destination.value;
  const qIdx = value.indexOf('?');
  if (qIdx < 0) return null;
  const params: Record<string, string> = {};
  new URLSearchParams(value.slice(qIdx + 1)).forEach((v, k) => {
    params[k] = v;
  });
  return Object.keys(params).length ? params : null;
}

export function actionHref(item: ActionItem | undefined): string {
  return item?.destination?.value ?? '#';
}

export function isExternalAction(item: ActionItem | undefined): boolean {
  return item?.destination?.kind === 'url';
}

export function isRouteAction(item: ActionItem | undefined): boolean {
  return item?.destination?.kind === 'route';
}
