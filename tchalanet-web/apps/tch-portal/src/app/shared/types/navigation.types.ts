export type ActionItemKind = 'button' | 'link' | 'external-link';

export interface ActionItem {
  readonly id: string;
  readonly kind: ActionItemKind;
  readonly labelKey: string;
  readonly icon?: string;
  readonly destination?: NavigationDestination;
  readonly disabled?: boolean;
}

export type NavigationDestinationKind = 'route' | 'url';

export interface NavigationDestination {
  readonly kind: NavigationDestinationKind;
  readonly value: string;
  readonly requiredRoles?: readonly string[];
}
