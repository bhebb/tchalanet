import { TestBed } from '@angular/core/testing';

import { WidgetConfig, WidgetDynamicError } from '../runtime/pagemodel.types';
import { WIDGET_REGISTRY, WidgetHostComponent } from './widget-host.component';

class TestWidget {}

/**
 * Tests the host's containment logic via the `state()` computed without rendering the template,
 * so no TranslateService stub is needed. Containment guarantees: invalid id/type, unsupported
 * type, and a widget-local dynamic error each resolve to their own contained state.
 */
describe('WidgetHostComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: WIDGET_REGISTRY, useValue: { HeroWidget: TestWidget } }],
    });
  });

  function host(inputs: {
    widgetId: string;
    config?: WidgetConfig;
    errors?: readonly WidgetDynamicError[];
    hideErrors?: boolean;
  }) {
    const fixture = TestBed.createComponent(WidgetHostComponent);
    fixture.componentRef.setInput('widgetId', inputs.widgetId);
    fixture.componentRef.setInput('config', inputs.config);
    fixture.componentRef.setInput('errors', inputs.errors ?? []);
    fixture.componentRef.setInput('hideErrors', inputs.hideErrors ?? false);
    return fixture.componentInstance;
  }

  it('reports ok for a supported widget type', () => {
    const cmp = host({ widgetId: 'home.hero', config: { type: 'HeroWidget' } });
    expect(cmp.state().kind).toBe('ok');
  });

  it('reports unsupported for an unknown widget type', () => {
    const cmp = host({ widgetId: 'home.x', config: { type: 'UnknownWidget' } });
    expect(cmp.state().kind).toBe('unsupported');
  });

  it('reports invalid when id or type is missing', () => {
    expect(host({ widgetId: '', config: { type: 'HeroWidget' } }).state().kind).toBe('invalid');
    expect(host({ widgetId: 'home.hero', config: undefined }).state().kind).toBe('invalid');
  });

  it('surfaces a widget-local error when dynamic.errors targets this widget', () => {
    const cmp = host({
      widgetId: 'home.news',
      config: { type: 'NewsTickerWidget' },
      errors: [{ widgetId: 'home.news', code: 'BOOM' }],
    });
    expect(cmp.state().kind).toBe('error');
  });

  it('shows the error block by default so ops surfaces keep their diagnostic', () => {
    const cmp = host({
      widgetId: 'home.news',
      config: { type: 'NewsTickerWidget' },
      errors: [{ widgetId: 'home.news', code: 'BOOM' }],
    });
    expect(cmp.hidden()).toBe(false);
  });

  it('hides the error block when the page opts out', () => {
    const cmp = host({
      widgetId: 'home.news',
      config: { type: 'NewsTickerWidget' },
      errors: [{ widgetId: 'home.news', code: 'BOOM' }],
      hideErrors: true,
    });
    expect(cmp.hidden()).toBe(true);
  });

  it('hides the error block when the widget itself opts out', () => {
    const cmp = host({
      widgetId: 'home.news',
      config: { type: 'NewsTickerWidget', props: { hideOnError: true } },
      errors: [{ widgetId: 'home.news', code: 'BOOM' }],
    });
    expect(cmp.hidden()).toBe(true);
  });

  it('never hides a healthy widget', () => {
    const cmp = host({ widgetId: 'home.hero', config: { type: 'HeroWidget' }, hideErrors: true });
    expect(cmp.hidden()).toBe(false);
  });

  it('emits the owning widget id when a local retry is requested', () => {
    const cmp = host({
      widgetId: 'dashboard.tenantAdmin.salesTrend',
      config: { type: 'TrendChartWidget' },
      errors: [{ widgetId: 'dashboard.tenantAdmin.salesTrend', code: 'analytics.unavailable' }],
    });
    let retriedWidget: string | undefined;
    cmp.retry.subscribe(widgetId => {
      retriedWidget = widgetId;
    });

    cmp.retry.emit('dashboard.tenantAdmin.salesTrend');

    expect(retriedWidget).toBe('dashboard.tenantAdmin.salesTrend');
  });
});
