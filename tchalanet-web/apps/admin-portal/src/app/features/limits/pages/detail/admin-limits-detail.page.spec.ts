import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';

import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';
import type { ActiveLimitItem, LimitRuleSpec } from '../../data-access/admin-limits.models';
import { AdminLimitsDetailPage } from './admin-limits-detail.page';

describe(AdminLimitsDetailPage.name, () => {
  function createComponent(assignmentId = 'assignment-1') {
    TestBed.configureTestingModule({
      imports: [AdminLimitsDetailPage],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => assignmentId } } },
        },
        {
          provide: AdminLimitsApi,
          useValue: {
            overview: () => ({ subscribe: () => undefined }),
            listRules: () => ({ subscribe: () => undefined }),
            upsertAssignment: () => ({ subscribe: () => undefined }),
            deleteAssignment: () => ({ subscribe: () => undefined }),
          },
        },
        { provide: MatDialog, useValue: { open: () => ({ componentInstance: {}, afterClosed: () => ({ subscribe: () => undefined }) }) } },
        {
          provide: TranslateService,
          useValue: {
            instant: (key: string, params?: Record<string, string>) => {
              const t: Record<string, string> = {
                'admin.limits.overview.limitStatus.active': 'Aktif',
                'admin.limits.overview.limitStatus.disabled': 'Dezaktive',
                'admin.limits.overview.duration.permanent': 'Pèmanan',
                'admin.limits.overview.duration.until': `Jiska ${params?.['date'] ?? ''}`,
                'admin.limits.overview.duration.custom': 'Peryòd espesyal',
                'admin.limits.overview.target.tenant': 'Platfòm',
                'admin.limits.dialog.block': 'Bloke',
                'admin.limits.dialog.warn': 'Avètisman',
              };
              return t[key] ?? key;
            },
          },
        },
      ],
    });

    const fixture = TestBed.createComponent(AdminLimitsDetailPage);
    const component = fixture.componentInstance;
    return component;
  }

  it('statusLabel is Aktif for enabled, Dezaktive for disabled', () => {
    const component = createComponent();

    component.item.set(item({ enabled: true }));
    expect(component.statusLabel()).toBe('Aktif');

    component.item.set(item({ enabled: false }));
    expect(component.statusLabel()).toBe('Dezaktive');
  });

  it('targetLabel returns Platfòm for TENANT scope', () => {
    const component = createComponent();
    component.item.set(item({ targetType: 'TENANT', targetId: '' }));

    expect(component.targetLabel()).toBe('Platfòm');
  });

  it('targetLabel falls back to targetCode then targetId for non-TENANT scope', () => {
    const component = createComponent();

    component.item.set(item({ targetType: 'DRAW_CHANNEL', targetLabel: null, targetCode: 'TX_1000', targetId: 'ch-id' }));
    expect(component.targetLabel()).toBe('TX_1000');

    component.item.set(item({ targetType: 'DRAW_CHANNEL', targetLabel: null, targetCode: null, targetId: 'ch-id' }));
    expect(component.targetLabel()).toBe('ch-id');

    component.item.set(item({ targetType: 'DRAW_CHANNEL', targetLabel: 'Texas 1000', targetCode: 'TX_1000', targetId: 'ch-id' }));
    expect(component.targetLabel()).toBe('Texas 1000');
  });

  it('validityLabel is Pèmanan when no dates', () => {
    const component = createComponent();
    component.item.set(item({ startsAt: null, endsAt: null }));

    expect(component.validityLabel()).toBe('Pèmanan');
  });

  it('validityLabel includes end date when endsAt is set', () => {
    const component = createComponent();
    component.item.set(item({ startsAt: null, endsAt: '2026-12-31T23:59:59Z' }));

    const label = component.validityLabel();
    expect(label).toContain('Jiska');
    expect(label).toContain('2026');
  });

  it('outcomeLabel translates BLOCK and WARN', () => {
    const component = createComponent();

    component.item.set(item({ onBreach: 'BLOCK' }));
    expect(component.outcomeLabel()).toBe('Bloke');

    component.item.set(item({ onBreach: 'WARN' }));
    expect(component.outcomeLabel()).toBe('Avètisman');
  });

  it('ruleKeyCode comes from the loaded spec', () => {
    const component = createComponent();
    component.spec.set(spec({ ruleKey: 'BLOCK_SELECTION_PER_DRAW' }));

    expect(component.ruleKeyCode()).toBe('BLOCK_SELECTION_PER_DRAW');
  });
});

function item(overrides: Partial<ActiveLimitItem> = {}): ActiveLimitItem {
  return {
    assignmentId: 'assignment-1',
    ruleKey: 'BLOCK_SELECTION_PER_DRAW',
    group: 'NUMBER_BLOCK',
    targetType: 'TENANT',
    targetId: '',
    targetLabel: null,
    targetCode: null,
    enabled: true,
    onBreach: 'BLOCK',
    params: {},
    startsAt: null,
    endsAt: null,
    actions: [],
    ...overrides,
  };
}

function spec(overrides: Partial<LimitRuleSpec> = {}): LimitRuleSpec {
  return {
    ruleKey: 'BLOCK_SELECTION_PER_DRAW',
    label: 'Bloke nimewo',
    description: 'Bloke nimewo pou yon tiraj',
    defaultOutcome: 'BLOCK',
    category: 'RESTRICTIONS',
    stateless: true,
    paramsTemplate: { schema: 'SELECTION', selections: [] },
    ...overrides,
  };
}
