import { FeatureGridWidget } from './widgets/feature-grid.widget';
import { HeroWidget } from './widgets/hero.widget';
import { NewsTickerWidget } from './widgets/news-ticker.widget';
import { PlansWidget } from './widgets/plans.widget';
import { resolveWidget } from './widget-registry';

describe('resolveWidget', () => {
  it.each([
    ['HeroWidget', HeroWidget],
    ['NewsTickerWidget', NewsTickerWidget],
    ['FeatureGridWidget', FeatureGridWidget],
    ['PlansWidget', PlansWidget],
  ])('resolves the V1 component for %s', (type, component) => {
    expect(resolveWidget(type)).toBe(component);
  });

  it.each(['PublicDrawResultsWidget', 'CheckTicketWidget', 'TchalaSearchWidget'])(
    'returns null for the out-of-scope widget %s',
    type => {
      expect(resolveWidget(type)).toBeNull();
    },
  );

  it('returns null for an unknown or missing type', () => {
    expect(resolveWidget('NopeWidget')).toBeNull();
    expect(resolveWidget(undefined)).toBeNull();
  });
});
