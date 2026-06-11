import { InputSignal } from '@angular/core';
import { NavigationDestination } from '@tch/api';

import { WidgetConfig } from '../runtime/pagemodel.types';

export type WidgetState = 'default' | 'loading' | 'empty' | 'error' | 'partial';

/**
 * Customer-facing ticket verification statuses.
 * Mirrors the backend PublicTicketVerificationResponse.status enum.
 */
export type VerificationStatus =
  | 'PENDING_RESULT'
  | 'LOST'
  | 'WINNING_PAYABLE'
  | 'WINNING_PAID'
  | 'CANCELLED'
  | 'EXPIRED'
  | 'BLOCKED'
  | 'NOT_FOUND'
  | 'SERVICE_UNAVAILABLE';

/** Mirrors the backend DrawResultStatus enum. */
export type ResultStatus = 'PROVISIONAL' | 'CONFIRMED' | 'OVERRIDDEN' | 'ERROR';

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
  readonly labelKey?: string;
  readonly label?: string;
  readonly icon?: string;
  readonly destination?: NavigationDestination;
  readonly disabled?: boolean;
  readonly reasonKey?: string | null;
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

/**
 * Optional widget data binding. A config value may be a literal, or a binding that resolves from the
 * widget's resolved dynamic payload (`dynamic.widgets[id]`, passed to the widget as its `dynamic`
 * input) by dot-path. Bindings are opt-in and backward-compatible — a widget without one keeps its
 * current behavior.
 */
export interface WidgetBinding {
  readonly source: 'dynamic';
  readonly path: string;
}

const FORBIDDEN_PATH_PARTS = new Set(['__proto__', 'prototype', 'constructor']);

/** Type guard for a `{ source: 'dynamic', path }` binding descriptor. */
export function isBinding(value: unknown): value is WidgetBinding {
  return (
    isRecord(value) && value['source'] === 'dynamic' && typeof value['path'] === 'string'
  );
}

/** Resolve a dot-path against a value (e.g. `kpis.totalSellers`). Proto-pollution safe. */
export function resolvePath(root: unknown, path: string): unknown {
  if (!path) {
    return undefined;
  }
  let cursor: unknown = root;
  for (const part of path.split('.')) {
    if (!part || FORBIDDEN_PATH_PARTS.has(part) || !isRecord(cursor)) {
      return undefined;
    }
    cursor = cursor[part];
  }
  return cursor;
}

/**
 * Resolve a config value that may be a literal or a `{ source, path }` binding.
 * Bindings read from the widget's `dynamic` payload; literals pass through unchanged.
 */
export function resolveBinding(value: unknown, dynamic: unknown): unknown {
  return isBinding(value) ? resolvePath(dynamic, value.path) : value;
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
  const destinationValue = stringValue(value['value']);
  if (kind === 'url' && destinationValue) {
    return { kind: 'url', value: destinationValue };
  }
  if (kind === 'route' && destinationValue) {
    return { kind: 'route', value: toPublicPath(destinationValue) };
  }
  if (kind === 'anchor' && destinationValue) {
    return { kind: 'url', value: destinationValue };
  }

  return undefined;
}

export function destinationHref(destination: NavigationDestination | undefined): string {
  if (!destination) {
    return '#';
  }
  if (destination.kind === 'url') {
    return destination.value;
  }
  return destination.value;
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
    labelKey: stringValue(value['labelKey']),
    label: stringValue(value['label']),
    icon: stringValue(value['icon']),
    destination: mapBackendDestination(value['destination']),
    disabled: typeof value['disabled'] === 'boolean' ? value['disabled'] : false,
    reasonKey: stringValue(value['reasonKey']) ?? null,
    style: stringValue(value['style']),
  };
}

function isWidgetAction(value: WidgetAction | undefined): value is WidgetAction {
  return !!value?.destination;
}
