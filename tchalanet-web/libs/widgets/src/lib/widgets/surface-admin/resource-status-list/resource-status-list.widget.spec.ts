import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';

import { WidgetConfig } from '@tch/page-model';

import { ResourceStatusListWidget } from './resource-status-list.widget';

describe('ResourceStatusListWidget', () => {
  function component(config: WidgetConfig, dynamic: unknown) {
    TestBed.configureTestingModule({
      imports: [ResourceStatusListWidget],
      providers: [provideTranslateService()],
    });
    const fixture = TestBed.createComponent(ResourceStatusListWidget);
    fixture.componentRef.setInput('config', config);
    fixture.componentRef.setInput('dynamic', dynamic);
    return fixture.componentInstance;
  }

  it('reads service resources and summary counts from the dynamic payload', () => {
    const cmp = component(
      { type: 'ResourceStatusListWidget', props: { titleKey: 'dashboard.ops.resources.title' } },
      {
        criticalCount: 1,
        warningCount: '2',
        services: [
          {
            serviceKey: 'api',
            displayName: 'API server',
            status: 'UP',
            severity: 'OK',
            memoryPercent: '61',
          },
          {
            serviceKey: 'worker',
            displayName: 'Worker',
            status: 'DOWN',
            severity: 'CRITICAL',
            oomKilled: true,
          },
        ],
      },
    );

    expect(cmp.summary()).toEqual({ criticalCount: 1, warningCount: 2 });
    expect(cmp.services()).toEqual([
      expect.objectContaining({ id: 'api', displayName: 'API server', memoryPercent: 61 }),
      expect.objectContaining({ id: 'worker', severity: 'CRITICAL', oomKilled: true }),
    ]);
  });

  it('returns an empty list when services are missing', () => {
    const cmp = component({ type: 'ResourceStatusListWidget', props: {} }, {});

    expect(cmp.summary()).toEqual({ criticalCount: 0, warningCount: 0 });
    expect(cmp.services()).toEqual([]);
  });

  it('can filter database schema resources and read capacity metrics', () => {
    const cmp = component(
      {
        type: 'ResourceStatusListWidget',
        props: { serviceKeyPrefix: 'database:schema:' },
      },
      {
        services: [
          { serviceKey: 'api', displayName: 'API server', severity: 'OK' },
          {
            serviceKey: 'database:schema:batch',
            displayName: 'DB schema batch',
            status: 'OK',
            severity: 'OK',
            sizeMb: '12',
            indexSizeMb: 3,
            tableCount: 6,
          },
        ],
      },
    );

    expect(cmp.services()).toEqual([
      expect.objectContaining({
        id: 'database:schema:batch',
        sizeMb: 12,
        indexSizeMb: 3,
        tableCount: 6,
      }),
    ]);
  });
});
