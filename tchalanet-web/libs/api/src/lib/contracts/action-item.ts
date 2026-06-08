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
  readonly badge?: unknown;
  readonly children?: readonly ActionItem[];
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
  return item?.destination?.kind === 'route' ? item.destination.value : '';
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
