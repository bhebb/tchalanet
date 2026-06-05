import { InputSignal } from '@angular/core';

import { WidgetConfig } from '../../shared/types';

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
