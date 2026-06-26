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
import { QuickActionsWidget } from './widgets/surface-admin/quick-actions/quick-actions.widget';
import { RankingListWidget } from './widgets/surface-admin/ranking-list/ranking-list.widget';
import { ReadinessSummaryWidget } from './widgets/surface-admin/readiness-summary/readiness-summary.widget';
import { TrendChartWidget } from './widgets/surface-admin/trend-chart/trend-chart.widget';
import { resolveWidget } from './widget-registry';

describe('resolveWidget', () => {
  it.each([
    ['KpiGridWidget', KpiGridWidget],
    ['AlertsWidget', AlertsWidget],
    ['BreakdownListWidget', BreakdownListWidget],
    ['CommissionSummaryWidget', CommissionSummaryWidget],
    ['ReadinessSummaryWidget', ReadinessSummaryWidget],
    ['RankingListWidget', RankingListWidget],
    ['TrendChartWidget', TrendChartWidget],
    ['QuickActionsWidget', QuickActionsWidget],
    ['HeroWidget', HeroWidget],
    ['NewsTickerWidget', NewsTickerWidget],
    ['PublicDrawResultsWidget', PublicDrawResultsWidget],
    ['LatestResultsWidget', PublicDrawResultsWidget],
    ['CheckTicketWidget', TicketVerificationWidget],
    ['TicketVerificationWidget', TicketVerificationWidget],
    ['HowItWorksWidget', HowItWorksWidget],
    ['RulesWidget', RulesSimulationWidget],
    ['SimulationWidget', RulesSimulationWidget],
    ['ManagerCtaWidget', OperatorCtaWidget],
    ['OperatorCtaWidget', OperatorCtaWidget],
    ['TchalaSearchWidget', TchalaSearchWidget],
    ['FeatureGridWidget', FeatureGridWidget],
    ['PlansWidget', PlansWidget],
    ['ContactCtaWidget', ContactCtaWidget],
    ['PublicBusinessHeroWidget', PublicBusinessHeroWidget],
    ['PublicBusinessFeaturesWidget', PublicBusinessFeaturesWidget],
    ['PublicBusinessProcessWidget', PublicBusinessProcessWidget],
    ['PublicBusinessPlansWidget', PublicBusinessPlansWidget],
    ['PublicBusinessAccessControlWidget', PublicBusinessAccessControlWidget],
    ['PublicBusinessLeadFormWidget', PublicBusinessLeadFormWidget],
    ['FaqWidget', FaqWidget],
  ])('resolves the V1 component for %s', (type, component) => {
    expect(resolveWidget(type)).toBe(component);
  });

  it('returns null for an unknown or missing type', () => {
    expect(resolveWidget('NopeWidget')).toBeNull();
    expect(resolveWidget(undefined)).toBeNull();
  });
});
