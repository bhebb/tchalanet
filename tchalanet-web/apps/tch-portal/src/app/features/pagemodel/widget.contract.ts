import { InputSignal } from '@angular/core';

import { WidgetConfig } from '../../shared/types';

export type NavigationDestination =
  | { readonly type: 'path'; readonly path: string }
  | { readonly type: 'external'; readonly url: string }
  | { readonly type: 'anchor'; readonly id: string };

export type WidgetState = 'default' | 'loading' | 'empty' | 'error' | 'partial';

export type VerificationStatus =
  | 'PENDING_RESULT'
  | 'NOT_PAYABLE'
  | 'PAYABLE'
  | 'INVALID_OR_CANCELLED'
  | 'NOT_FOUND'
  | 'SERVICE_UNAVAILABLE';

export type ResultStatus = 'CONFIRMED' | 'PENDING' | 'UNAVAILABLE';

export type SimulationStatus =
  | 'NO_GAME_SELECTED'
  | 'GAME_SELECTED'
  | 'RULES_UNAVAILABLE'
  | 'INVALID_SELECTION'
  | 'INVALID_STAKE'
  | 'SIMULATION_UNAVAILABLE'
  | 'CALCULATED';

export interface WidgetAction {
  readonly id?: string;
  readonly label_key?: string;
  readonly label?: string;
  readonly icon?: string;
  readonly destination?: NavigationDestination;
  readonly disabled?: boolean;
  readonly reason_key?: string | null;
  readonly style?: 'primary' | 'secondary' | 'tertiary' | string;
}

/**
 * Inputs every renderable widget receives. A widget gets only its own config, its resolved
 * dynamic payload, and its id — never the whole page model (design: widgets must not receive the
 * full page object).
 */
export interface WidgetInputs {
  readonly config: InputSignal<WidgetConfig>;
  readonly dynamic: InputSignal<unknown>;
  readonly widgetId: InputSignal<string>;
}

/** Read a string prop from a widget config, returning undefined when absent. */
export function stringProp(config: WidgetConfig | undefined, key: string): string | undefined {
  const value = config?.props?.[key];
  return typeof value === 'string' ? value : undefined;
}

/** Derive a stable human fallback label from an i18n key (e.g. `home.hero.title` → `title`). */
export function keyFallback(key: string | undefined): string {
  if (!key) {
    return '';
  }
  const last = key.split('.').pop() ?? key;
  return last.replace(/[_-]+/g, ' ').trim();
}

export function actionsFrom(value: unknown): readonly WidgetAction[] {
  return Array.isArray(value) ? value.map(mapBackendAction).filter(isWidgetAction) : [];
}

export function actionFrom(value: unknown): WidgetAction | undefined {
  const action = mapBackendAction(value);
  return isWidgetAction(action) ? action : undefined;
}

export function mapBackendDestination(value: unknown): NavigationDestination | undefined {
  if (!isRecord(value)) {
    return undefined;
  }

  const kind = stringValue(value['kind']);
  const path = stringValue(value['path']);
  const url = stringValue(value['url']) ?? path;
  const anchorId = stringValue(value['anchor_id']) ?? stringValue(value['anchorId']);

  if (kind === 'external' && url) {
    return { type: 'external', url };
  }

  if ((kind === 'anchor' || stringValue(value['type']) === 'anchor') && (anchorId || path)) {
    const id = anchorId ?? path;
    return id ? { type: 'anchor', id: id.replace(/^#/, '') } : undefined;
  }

  if (path) {
    return { type: 'path', path: toPublicPath(path) };
  }

  return undefined;
}

export function destinationHref(destination: NavigationDestination | undefined): string {
  if (!destination) {
    return '#';
  }
  if (destination.type === 'external') {
    return destination.url;
  }
  if (destination.type === 'anchor') {
    return `#${destination.id}`;
  }
  return destination.path;
}

export function toPublicPath(path: string): string {
  const clean = path.trim();
  if (!clean || clean.startsWith('http') || clean.startsWith('#') || clean.startsWith('/public')) {
    return clean || '/public';
  }

  const routeMap: Readonly<Record<string, string>> = {
    '/': '/public',
    '/results': '/public/results',
    '/verifier': '/public/check-ticket',
    '/check-ticket': '/public/check-ticket',
    '/support': '/public/help',
    '/help': '/public/help',
    '/demo': '/public/contact',
    '/contact': '/public/contact',
    '/privacy': '/public/privacy',
    '/legal': '/public/terms',
    '/terms': '/public/terms',
    '/games': '/public/rules',
    '/pricing': '/public/contact',
    '/features': '/public',
    '/about': '/public/contact',
  };

  return routeMap[clean] ?? clean;
}

export function stringValue(value: unknown): string | undefined {
  return typeof value === 'string' && value.trim().length > 0 ? value : undefined;
}

export function isRecord(value: unknown): value is Readonly<Record<string, unknown>> {
  return !!value && typeof value === 'object' && !Array.isArray(value);
}

function mapBackendAction(value: unknown): WidgetAction | undefined {
  if (!isRecord(value)) {
    return undefined;
  }

  return {
    id: stringValue(value['id']),
    label_key: stringValue(value['label_key']),
    label: stringValue(value['label']),
    icon: stringValue(value['icon']),
    destination: mapBackendDestination(value),
    disabled: typeof value['disabled'] === 'boolean' ? value['disabled'] : false,
    reason_key: stringValue(value['reason_key']) ?? null,
    style: stringValue(value['style']),
  };
}

function isWidgetAction(value: WidgetAction | undefined): value is WidgetAction {
  return !!value?.destination;
}
