import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';

import { WidgetConfig } from '@tch/page-model';

import { KpiGridWidget } from './kpi-grid.widget';

describe('KpiGridWidget', () => {
  function component(config: WidgetConfig, dynamic: unknown) {
    TestBed.configureTestingModule({
      imports: [KpiGridWidget],
      providers: [provideTranslateService()],
    });
    const fixture = TestBed.createComponent(KpiGridWidget);
    fixture.componentRef.setInput('config', config);
    fixture.componentRef.setInput('dynamic', dynamic);
    return fixture.componentInstance;
  }

  it('renders zero danger and warning KPI values as neutral', () => {
    const cmp = component(
      {
        type: 'KpiGridWidget',
        props: {
          items: [
            {
              id: 'failed',
              labelKey: 'platform.kpi.schedulerFailed.label',
              tone: 'danger',
              value: { source: 'dynamic', path: 'failedCount' },
            },
            {
              id: 'warning',
              labelKey: 'platform.kpi.resourceWarning.label',
              tone: 'warning',
              value: { source: 'dynamic', path: 'warningCount' },
            },
          ],
        },
      },
      { failedCount: 0, warningCount: '0' },
    );

    expect(cmp.visualTone(cmp.items()[0], 0)).toBe('neutral');
    expect(cmp.visualTone(cmp.items()[1], 1)).toBe('neutral');
  });

  it('keeps danger and warning KPI tones when values are non-zero', () => {
    const cmp = component(
      {
        type: 'KpiGridWidget',
        props: {
          items: [
            { id: 'failed', labelKey: 'failed', tone: 'danger', value: 1 },
            { id: 'warning', labelKey: 'warning', tone: 'warning', value: '2' },
          ],
        },
      },
      {},
    );

    expect(cmp.visualTone(cmp.items()[0], 0)).toBe('danger');
    expect(cmp.visualTone(cmp.items()[1], 1)).toBe('warning');
  });
});
