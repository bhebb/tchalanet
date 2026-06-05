import { Type } from '@angular/core';

import { FeatureGridWidget } from './widgets/feature-grid.widget';
import { HeroWidget } from './widgets/hero.widget';
import { NewsTickerWidget } from './widgets/news-ticker.widget';
import { PlansWidget } from './widgets/plans.widget';

/**
 * Maps a backend widget `type` string to its Angular component. The key is the real backend type
 * (e.g. `HeroWidget`), so adding backend widgets only requires registering the matching component.
 *
 * V1 supported set. `PublicDrawResultsWidget`, `CheckTicketWidget`, and `TchalaSearchWidget` are
 * intentionally NOT registered this slice and render the unsupported-widget fallback.
 */
export const WIDGET_REGISTRY: Readonly<Record<string, Type<unknown>>> = {
  HeroWidget,
  NewsTickerWidget,
  FeatureGridWidget,
  PlansWidget,
};

export function resolveWidget(type: string | undefined): Type<unknown> | null {
  if (!type) {
    return null;
  }
  return WIDGET_REGISTRY[type] ?? null;
}
