import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';

import { WidgetConfig } from '@tch/page-model';

import { TrendChartWidget } from './trend-chart.widget';

describe('TrendChartWidget', () => {
  function component(config: WidgetConfig, dynamic: unknown) {
    TestBed.configureTestingModule({
      imports: [TrendChartWidget],
      providers: [provideTranslateService()],
    });
    const fixture = TestBed.createComponent(TrendChartWidget);
    fixture.componentRef.setInput('config', config);
    fixture.componentRef.setInput('dynamic', dynamic);
    return fixture.componentInstance;
  }

  it('reads dynamic points and shortens ISO date labels when requested', () => {
    const cmp = component(
      {
        type: 'TrendChartWidget',
        props: {
          points: { source: 'dynamic', path: 'dailyBreakdown' },
          labelPath: 'refDate',
          labelFormat: 'date-short',
          valuePath: 'grossSales',
        },
      },
      {
        dailyBreakdown: [
          { refDate: '2026-06-24', grossSales: '100.50' },
          { refDate: '2026-06-25', grossSales: 250 },
        ],
      },
    );

    expect(cmp.points()).toEqual([
      { id: '0-100.5', label: '06/24', value: 100.5 },
      { id: '1-250', label: '06/25', value: 250 },
    ]);
    expect(cmp.linePoints()).toContain('0.00');
    expect(cmp.areaPoints()).toContain('100,96');
  });

  it('returns no points when the configured dynamic path is not an array', () => {
    const cmp = component(
      { type: 'TrendChartWidget', props: { points: { source: 'dynamic', path: 'missing' } } },
      { points: [] },
    );

    expect(cmp.points()).toEqual([]);
    expect(cmp.linePoints()).toBe('');
  });
});
