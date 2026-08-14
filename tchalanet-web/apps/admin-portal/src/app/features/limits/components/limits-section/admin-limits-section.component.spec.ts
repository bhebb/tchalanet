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
            instant: (key: string, params?: Record<string, string>) => {
              const translations: Record<string, string> = {
                'admin.limits.section.blockedNumbers': `Nimewo bloke : ${params?.['numbers'] ?? ''}`,
                'admin.limits.section.currentScope': 'Règ kote sa a',
                'admin.limits.section.inheritedScope': `Soti nan ${params?.['scope'] ?? ''}`,
                'admin.limits.section.scope.tenant': 'santral',
                'admin.limits.dialog.block': 'Bloke',
                'admin.limits.dialog.warn': 'Avèti',
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

  it('shows only enabled rules effective at the draw time and orders blocked numbers first', () => {
    const component = createComponent();
    const data: CombinedLimitData = {
      specs: [],
      assignments: [
        assignment('draw-cap', {
          ruleKey: 'MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW',
          params: { valueCents: 50000 },
        }),
        assignment('draw-block', {
          ruleKey: 'BLOCK_SELECTION_PER_DRAW',
          params: { selections: ['12', '45'] },
        }),
        assignment('disabled-block', {
          enabled: false,
          ruleKey: 'BLOCK_SELECTION_PER_DRAW',
          params: { selections: ['99'] },
        }),
      ],
      inheritedAssignments: [
        assignment('future-tenant-block', {
          ruleKey: 'BLOCK_SELECTION_PER_DRAW',
          params: { selections: ['77'] },
          startsAt: '2026-08-13T00:00:00Z',
        }),
        assignment('expired-tenant-block', {
          ruleKey: 'BLOCK_SELECTION_PER_DRAW',
          params: { selections: ['88'] },
          endsAt: '2026-08-12T13:59:59Z',
        }),
      ],
    };

    const rows = component.activeRows(data);

    expect(rows.map(row => row.item.id.value)).toEqual(['draw-block', 'draw-cap']);
    expect(component.activeLabel(rows[0].item)).toBe('Nimewo bloke : 12, 45');
    expect(component.activeSourceLabel(rows[0])).toBe('Règ kote sa a');
  });

  it('labels inherited effective rules with the inherited scope', () => {
    const component = createComponent();
    const data: CombinedLimitData = {
      specs: [],
      assignments: [],
      inheritedAssignments: [
        assignment('tenant-block', {
          ruleKey: 'BLOCK_SELECTION_PER_DRAW',
          params: { selections: ['34'] },
        }),
      ],
    };

    const rows = component.activeRows(data);

    expect(rows.length).toBe(1);
    expect(component.activeLabel(rows[0].item)).toBe('Nimewo bloke : 34');
    expect(component.activeSourceLabel(rows[0])).toBe('Soti nan santral');
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
