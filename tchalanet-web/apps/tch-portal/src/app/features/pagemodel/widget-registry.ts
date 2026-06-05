import { Type } from '@angular/core';

import { FeatureGridWidget } from './widgets/feature-grid.widget';
import { HeroWidget } from './widgets/hero.widget';
import { HowItWorksWidget } from './widgets/how-it-works.widget';
import { NewsTickerWidget } from './widgets/news-ticker.widget';
import { OperatorCtaWidget } from './widgets/operator-cta.widget';
import { PlansWidget } from './widgets/plans.widget';
import { PublicDrawResultsWidget } from './widgets/public-draw-results.widget';
import { RulesSimulationWidget } from './widgets/rules-simulation.widget';
import { TchalaSearchWidget } from './widgets/tchala-search.widget';
import { TicketVerificationWidget } from './widgets/ticket-verification.widget';

/**
 * Maps a backend widget `type` string to its Angular component. The key is the real backend type
 * (e.g. `HeroWidget`), so adding backend widgets only requires registering the matching component.
 *
 * V1 supported set. The registry keys are backend widget type strings.
 */
export const WIDGET_REGISTRY: Readonly<Record<string, Type<unknown>>> = {
  HeroWidget,
  NewsTickerWidget,
  PublicDrawResultsWidget,
  LatestResultsWidget: PublicDrawResultsWidget,
  CheckTicketWidget: TicketVerificationWidget,
  TicketVerificationWidget,
  HowItWorksWidget,
  RulesWidget: RulesSimulationWidget,
  SimulationWidget: RulesSimulationWidget,
  OperatorCtaWidget,
  TchalaSearchWidget,
  FeatureGridWidget,
  PlansWidget,
};

export function resolveWidget(type: string | undefined): Type<unknown> | null {
  if (!type) {
    return null;
  }
  return WIDGET_REGISTRY[type] ?? null;
}
