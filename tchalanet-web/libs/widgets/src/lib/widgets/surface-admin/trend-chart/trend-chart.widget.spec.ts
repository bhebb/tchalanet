import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';

import { WidgetConfig } from '@tch/page-model';

import { TrendChartWidget } from './trend-chart.widget';

describe('TrendChartWidget', () => {
  function component(config: WidgetConfig, dynamic: unknown, widgetId = '') {
    TestBed.configureTestingModule({
      imports: [TrendChartWidget],
      providers: [provideTranslateService()],
    });
    const fixture = TestBed.createComponent(TrendChartWidget);
    fixture.componentRef.setInput('config', config);
    fixture.componentRef.setInput('dynamic', dynamic);
    fixture.componentRef.setInput('widgetId', widgetId);
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

  it('builds drawable points for a single live sales day', () => {
    const cmp = component(
      {
        type: 'TrendChartWidget',
        props: {
          points: { source: 'dynamic', path: 'points' },
          labelPath: 'label',
          labelFormat: 'date-short',
          valuePath: 'grossSales',
        },
      },
      {
        points: [{ id: '2026-07-16', label: '2026-07-16', grossSales: '250.00' }],
      },
    );

    expect(cmp.points()).toEqual([{ id: '2026-07-16', label: '07/16', value: 250 }]);
    expect(cmp.linePoints()).toBe('0.00,12.00');
    expect(cmp.hasTrend()).toBe(false);
    expect(cmp.areaPoints()).toBe('');
  });

  it('supports a bar chart for a single selected-period day', () => {
    const cmp = component(
      {
        type: 'TrendChartWidget',
        props: {
          chartType: 'bar',
          points: { source: 'dynamic', path: 'points' },
          labelPath: 'label',
          labelFormat: 'date-short',
          valuePath: 'grossSales',
        },
      },
      { points: [{ id: '2026-07-16', label: '2026-07-16', grossSales: 250 }] },
    );

    expect(cmp.chartType()).toBe('bar');
    expect(cmp.hasChart()).toBe(true);
    expect(cmp.yAxisTicks()).toEqual([250, 187.5, 125, 62.5, 0]);
    expect(cmp.barHeight(cmp.points()[0])).toBe(100);
  });

  it('keeps the tenant admin sales trend as bars for an older persisted model', () => {
    const cmp = component(
      { type: 'TrendChartWidget', props: { points: { source: 'dynamic', path: 'points' } } },
      { points: [{ label: '2026-07-31', grossSales: 250 }] },
      'dashboard.tenantAdmin.salesTrend',
    );

    expect(cmp.chartType()).toBe('bar');
  });
});
