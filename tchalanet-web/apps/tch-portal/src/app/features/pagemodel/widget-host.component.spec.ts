import { TestBed } from '@angular/core/testing';

import { WidgetConfig, WidgetDynamicError } from '../../shared/types';
import { WidgetHostComponent } from './widget-host.component';

/**
 * Tests the host's containment logic via the `state()` computed without rendering the template,
 * so no TranslateService stub is needed. Containment guarantees: invalid id/type, unsupported
 * type, and a widget-local dynamic error each resolve to their own contained state.
 */
describe('WidgetHostComponent', () => {
  function host(inputs: {
    widgetId: string;
    config?: WidgetConfig;
    errors?: readonly WidgetDynamicError[];
  }) {
    const fixture = TestBed.createComponent(WidgetHostComponent);
    fixture.componentRef.setInput('widgetId', inputs.widgetId);
    fixture.componentRef.setInput('config', inputs.config);
    fixture.componentRef.setInput('errors', inputs.errors ?? []);
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
});
