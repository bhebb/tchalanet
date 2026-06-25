import { Provider, Type } from '@angular/core';
import { WIDGET_REGISTRY as WIDGET_REGISTRY_TOKEN } from '@tch/page-model';

import { ContactCtaWidget } from './widgets/public/contact-cta/contact-cta.widget';
import { FaqWidget } from './widgets/public/faq/faq.widget';
import { FeatureGridWidget } from './widgets/public/feature-grid/feature-grid.widget';
import { HeroWidget } from './widgets/public/hero/hero.widget';
import { HowItWorksWidget } from './widgets/public/how-it-works/how-it-works.widget';
import { NewsTickerWidget } from './widgets/public/news-ticker/news-ticker.widget';
import { OperatorCtaWidget } from './widgets/public/operator-cta/operator-cta.widget';
import { PlansWidget } from './widgets/public/plans/plans.widget';
import { PublicBusinessAccessControlWidget } from './widgets/public/business-access-control/business-access-control.widget';
import { PublicBusinessFeaturesWidget } from './widgets/public/business-features/business-features.widget';
import { PublicBusinessHeroWidget } from './widgets/public/business-hero/business-hero.widget';
import { PublicBusinessLeadFormWidget } from './widgets/public/business-lead-form/business-lead-form.widget';
import { PublicBusinessPlansWidget } from './widgets/public/business-plans/business-plans.widget';
import { PublicBusinessProcessWidget } from './widgets/public/business-process/business-process.widget';
import { PublicDrawResultsWidget } from './widgets/public/draw-results/draw-results.widget';
import { RulesSimulationWidget } from './widgets/public/rules-simulation/rules-simulation.widget';
import { TchalaSearchWidget } from './widgets/public/tchala-search/tchala-search.widget';
import { TicketVerificationWidget } from './widgets/public/ticket-verification/ticket-verification.widget';
import { AlertsWidget } from './widgets/surface-admin/alerts/alerts.widget';
import { BreakdownListWidget } from './widgets/surface-admin/breakdown-list/breakdown-list.widget';
import { CommissionSummaryWidget } from './widgets/surface-admin/commission-summary/commission-summary.widget';
import { KpiGridWidget } from './widgets/surface-admin/kpi-grid/kpi-grid.widget';
import { OpsJobStatusListWidget } from './widgets/surface-admin/ops-job-status-list/ops-job-status-list.widget';
import { QuickActionsWidget } from './widgets/surface-admin/quick-actions/quick-actions.widget';
import { RankingListWidget } from './widgets/surface-admin/ranking-list/ranking-list.widget';
import { ReadinessSummaryWidget } from './widgets/surface-admin/readiness-summary/readiness-summary.widget';
import { ResourceStatusListWidget } from './widgets/surface-admin/resource-status-list/resource-status-list.widget';
import { TrendChartWidget } from './widgets/surface-admin/trend-chart/trend-chart.widget';

/**
 * Maps a backend widget `type` string to its Angular component. The key is the real backend type
 * (e.g. `HeroWidget`), so adding backend widgets only requires registering the matching component.
 *
 * V1 supported set. The registry keys are backend widget type strings.
 */
export const WIDGET_REGISTRY: Readonly<Record<string, Type<unknown>>> = {
  KpiGridWidget,
  OpsJobStatusListWidget,
  AlertsWidget,
  BreakdownListWidget,
  CommissionSummaryWidget,
  ReadinessSummaryWidget,
  ResourceStatusListWidget,
  RankingListWidget,
  TrendChartWidget,
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
