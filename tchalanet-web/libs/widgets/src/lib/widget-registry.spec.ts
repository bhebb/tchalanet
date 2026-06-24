import { FeatureGridWidget } from './widgets/feature-grid.widget';
import { HeroWidget } from './widgets/public/hero.widget';
import { HowItWorksWidget } from './widgets/how-it-works.widget';
import { NewsTickerWidget } from './widgets/news-ticker.widget';
import { PublicDrawResultsWidget } from './widgets/public-draw-results.widget';
import { TicketVerificationWidget } from './widgets/ticket-verification.widget';
import { PlansWidget } from './widgets/plans.widget';
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
