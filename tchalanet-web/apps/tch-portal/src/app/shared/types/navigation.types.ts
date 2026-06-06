export type ActionItemKind = 'button' | 'link' | 'externalLink' | 'languageSwitcher' | 'themeSwitcher';

export interface ActionItem {
  readonly id: string;
  readonly kind?: ActionItemKind;
  readonly labelKey?: string;
  readonly label?: string | null;
  readonly destination?: NavigationDestination;
  readonly icon?: string | null;
  readonly image?: string | null;
  readonly activeMatch?: 'exact' | 'prefix';
  readonly disabled?: boolean;
  readonly reasonKey?: string | null;
  readonly badge?: unknown;
  readonly children?: readonly ActionItem[];
}

export type NavigationDestinationKind = 'route' | 'url';

export interface NavigationDestination {
  readonly kind: NavigationDestinationKind;
  readonly value: string;
  readonly requiredRoles?: readonly string[];
}
