import { Provider, Type } from '@angular/core';
import { WIDGET_REGISTRY as WIDGET_REGISTRY_TOKEN } from '@tch/page-model';

import { AlertsWidget } from './widgets/alerts.widget';
import { CommissionSummaryWidget } from './widgets/commission-summary.widget';
import { ContactCtaWidget } from './widgets/contact-cta.widget';
import { KpiGridWidget } from './widgets/kpi-grid.widget';
import { QuickActionsWidget } from './widgets/quick-actions.widget';
import { ReadinessSummaryWidget } from './widgets/readiness-summary.widget';
import { FaqWidget } from './widgets/faq.widget';
import { FeatureGridWidget } from './widgets/public/feature-grid/feature-grid.widget';
import { HeroWidget } from './widgets/public/hero/hero.widget';
import { HowItWorksWidget } from './widgets/how-it-works.widget';
import { NewsTickerWidget } from './widgets/news-ticker.widget';
import { OperatorCtaWidget } from './widgets/operator-cta.widget';
import { PlansWidget } from './widgets/public/plans/plans.widget';
import { PublicBusinessAccessControlWidget } from './widgets/public-business-access-control.widget';
import { PublicBusinessFeaturesWidget } from './widgets/public-business-features.widget';
import { PublicBusinessHeroWidget } from './widgets/public/business-hero/business-hero.widget';
import { PublicBusinessLeadFormWidget } from './widgets/public/business-lead-form/business-lead-form.widget';
import { PublicBusinessPlansWidget } from './widgets/public/business-plans/business-plans.widget';
import { PublicBusinessProcessWidget } from './widgets/public-business-process.widget';
import { PublicDrawResultsWidget } from './widgets/public/draw-results/draw-results.widget';
import { RulesSimulationWidget } from './widgets/rules-simulation.widget';
import { TchalaSearchWidget } from './widgets/tchala-search.widget';
import { TicketVerificationWidget } from './widgets/public/ticket-verification/ticket-verification.widget';

/**
 * Maps a backend widget `type` string to its Angular component. The key is the real backend type
 * (e.g. `HeroWidget`), so adding backend widgets only requires registering the matching component.
 *
 * V1 supported set. The registry keys are backend widget type strings.
 */
export const WIDGET_REGISTRY: Readonly<Record<string, Type<unknown>>> = {
  KpiGridWidget,
  AlertsWidget,
  CommissionSummaryWidget,
  ReadinessSummaryWidget,
  QuickActionsWidget,
  HeroWidget,
  NewsTickerWidget,
  PublicDrawResultsWidget,
  LatestResultsWidget: PublicDrawResultsWidget,
  CheckTicketWidget: TicketVerificationWidget,
  TicketVerificationWidget,
  HowItWorksWidget,
  RulesWidget: RulesSimulationWidget,
  SimulationWidget: RulesSimulationWidget,
  ManagerCtaWidget: OperatorCtaWidget,
  OperatorCtaWidget,
  TchalaSearchWidget,
  FeatureGridWidget,
  PlansWidget,
  ContactCtaWidget,
  PublicBusinessHeroWidget,
  PublicBusinessFeaturesWidget,
  PublicBusinessProcessWidget,
  PublicBusinessPlansWidget,
  PublicBusinessAccessControlWidget,
  PublicBusinessLeadFormWidget,
  FaqWidget,
};

export function resolveWidget(type: string | undefined): Type<unknown> | null {
  if (!type) {
    return null;
  }
  return WIDGET_REGISTRY[type] ?? null;
}

export function provideWidgets(): Provider {
  return { provide: WIDGET_REGISTRY_TOKEN, useValue: WIDGET_REGISTRY };
}
