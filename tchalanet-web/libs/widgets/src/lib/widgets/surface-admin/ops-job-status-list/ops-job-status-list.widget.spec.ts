import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';

import { WidgetConfig } from '@tch/page-model';

import { OpsJobStatusListWidget } from './ops-job-status-list.widget';

describe('OpsJobStatusListWidget', () => {
  function component(config: WidgetConfig, dynamic: unknown) {
    TestBed.configureTestingModule({
      imports: [OpsJobStatusListWidget],
      providers: [provideTranslateService()],
    });
    const fixture = TestBed.createComponent(OpsJobStatusListWidget);
    fixture.componentRef.setInput('config', config);
    fixture.componentRef.setInput('dynamic', dynamic);
    return fixture.componentInstance;
  }

  it('reads scheduler summary and job items from the dynamic payload', () => {
    const cmp = component(
      { type: 'OpsJobStatusListWidget', props: { titleKey: 'dashboard.ops.jobs.title' } },
      {
        failedCount: '2',
        disabledGateCount: 1,
        staleCount: 0,
        neverRunCount: 3,
        historyAvailable: true,
        items: [
          {
            jobKey: 'results:external:fetch',
            displayName: 'Fetch results',
            scope: 'GLOBAL',
            status: 'FAILED',
            severity: 'CRITICAL',
            context: 'NY_EVE',
          },
        ],
      },
    );

    expect(cmp.summary()).toEqual({
      failedCount: 2,
      disabledGateCount: 1,
      staleCount: 0,
      neverRunCount: 3,
      historyAvailable: true,
    });
    expect(cmp.jobs()).toEqual([
      expect.objectContaining({
        id: 'results:external:fetch',
        displayName: 'Fetch results',
        status: 'FAILED',
        severity: 'CRITICAL',
        context: 'NY_EVE',
      }),
    ]);
  });

  it('returns defaults when scheduler data is missing', () => {
    const cmp = component({ type: 'OpsJobStatusListWidget', props: {} }, {});

    expect(cmp.summary()).toEqual({
      failedCount: 0,
      disabledGateCount: 0,
      staleCount: 0,
      neverRunCount: 0,
      historyAvailable: false,
    });
    expect(cmp.jobs()).toEqual([]);
  });
});
