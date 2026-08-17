import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';

import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';
import type { CombinedLimitData } from '../../data-access/admin-limits-api.service';
import type { LimitAssignmentItem } from '../../data-access/admin-limits.models';
import { AdminLimitsSectionComponent } from './admin-limits-section.component';

describe(AdminLimitsSectionComponent.name, () => {
  function createComponent() {
    TestBed.configureTestingModule({
      imports: [AdminLimitsSectionComponent],
      providers: [
        {
          provide: AdminLimitsApi,
          useValue: {
            combinedLimitsResource: () => ({
              value: () => undefined,
              error: () => null,
              isLoading: () => false,
              reload: () => undefined,
              status: () => 'resolved',
            }),
            deleteAssignment: () => ({ subscribe: () => undefined }),
          },
        },
        {
          provide: MatDialog,
          useValue: {
            open: () => ({
              componentInstance: {},
              afterClosed: () => ({ pipe: () => ({ subscribe: () => undefined }) }),
            }),
          },
        },
        {
          provide: TranslateService,
          useValue: {
            instant: (key: string) => {
              const translations: Record<string, string> = {
                'admin.limits.section.noParams': 'San paramèt',
                'admin.limits.section.scope.tenant': 'santral',
                'admin.limits.rule.BLOCK_SELECTION_PER_DRAW': 'Blokaj nimewo',
                'admin.limits.rule.MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW': 'Plafon nimewo',
              };
              return translations[key] ?? key;
            },
          },
        },
      ],
    });

    const fixture = TestBed.createComponent(AdminLimitsSectionComponent);
    fixture.componentRef.setInput('targetType', 'DRAW_CHANNEL');
    fixture.componentRef.setInput('targetId', 'draw-channel-1');
    fixture.componentRef.setInput('inheritedTargetType', 'TENANT');
    fixture.componentRef.setInput('inheritedScopeLabel', 'santral');
    fixture.componentRef.setInput('sectionTitle', 'Limit');
    fixture.componentRef.setInput('effectiveAt', '2026-08-12T14:00:00Z');
    return fixture.componentInstance;
  }

  it('returns only enabled local assignments effective at the given time', () => {
    const component = createComponent();
    const data: CombinedLimitData = {
      specs: [],
      assignments: [
        assignment('draw-cap', { ruleKey: 'MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW', params: { valueCents: 50000 } }),
        assignment('draw-block', { ruleKey: 'BLOCK_SELECTION_PER_DRAW', params: { selections: ['12', '45'] } }),
        assignment('disabled-block', { enabled: false, ruleKey: 'BLOCK_SELECTION_PER_DRAW', params: { selections: ['99'] } }),
      ],
      inheritedAssignments: [],
    };

    const rows = component.localRows(data);

    expect(rows.map(r => r.id.value)).toContain('draw-cap');
    expect(rows.map(r => r.id.value)).toContain('draw-block');
    expect(rows.map(r => r.id.value)).not.toContain('disabled-block');
  });

  it('excludes inherited assignments not effective at the draw time', () => {
    const component = createComponent();
    const data: CombinedLimitData = {
      specs: [],
      assignments: [],
      inheritedAssignments: [
        assignment('future-block', {
          ruleKey: 'BLOCK_SELECTION_PER_DRAW',
          params: { selections: ['77'] },
          startsAt: '2026-08-13T00:00:00Z',
        }),
        assignment('expired-block', {
          ruleKey: 'BLOCK_SELECTION_PER_DRAW',
          params: { selections: ['88'] },
          endsAt: '2026-08-12T13:59:59Z',
        }),
        assignment('active-tenant-block', {
          ruleKey: 'BLOCK_SELECTION_PER_DRAW',
          params: { selections: ['34'] },
        }),
      ],
    };

    const rows = component.inheritedRows(data);

    expect(rows.length).toBe(1);
    expect(rows[0].id.value).toBe('active-tenant-block');
  });

  it('formats params for SELECTION type rules', () => {
    const component = createComponent();
    const item = assignment('block', { ruleKey: 'BLOCK_SELECTION_PER_DRAW', params: { selections: ['12', '45'] } });

    const label = component.paramsLabel(item);

    expect(label).toBe('12, 45');
  });

  it('formats params for CENTS type rules', () => {
    const component = createComponent();
    const item = assignment('cap', {
      ruleKey: 'MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW',
      params: { valueCents: 50000 },
    });

    const label = component.paramsLabel(item);

    expect(label).toContain('HTG');
  });
});

function assignment(id: string, overrides: Partial<LimitAssignmentItem>): LimitAssignmentItem {
  return {
    id: { value: id },
    ruleKey: 'BLOCK_SELECTION_PER_DRAW',
    enabled: true,
    onBreach: 'BLOCK',
    params: {},
    startsAt: null,
    endsAt: null,
    ...overrides,
  };
}
