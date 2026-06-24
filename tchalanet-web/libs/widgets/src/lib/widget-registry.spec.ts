import { FeatureGridWidget } from './widgets/public/feature-grid/feature-grid.widget';
import { HeroWidget } from './widgets/public/hero/hero.widget';
import { HowItWorksWidget } from './widgets/public/how-it-works/how-it-works.widget';
import { NewsTickerWidget } from './widgets/public/news-ticker/news-ticker.widget';
import { PublicDrawResultsWidget } from './widgets/public/draw-results/draw-results.widget';
import { TicketVerificationWidget } from './widgets/public/ticket-verification/ticket-verification.widget';
import { PlansWidget } from './widgets/public/plans/plans.widget';
import { resolveWidget } from './widget-registry';

describe('resolveWidget', () => {
  it.each([
    ['HeroWidget', HeroWidget],
    ['NewsTickerWidget', NewsTickerWidget],
    ['PublicDrawResultsWidget', PublicDrawResultsWidget],
    ['LatestResultsWidget', PublicDrawResultsWidget],
    ['CheckTicketWidget', TicketVerificationWidget],
    ['TicketVerificationWidget', TicketVerificationWidget],
    ['HowItWorksWidget', HowItWorksWidget],
    ['FeatureGridWidget', FeatureGridWidget],
    ['PlansWidget', PlansWidget],
  ])('resolves the V1 component for %s', (type, component) => {
    expect(resolveWidget(type)).toBe(component);
  });

  it('returns null for an unknown or missing type', () => {
    expect(resolveWidget('NopeWidget')).toBeNull();
    expect(resolveWidget(undefined)).toBeNull();
  });
});
