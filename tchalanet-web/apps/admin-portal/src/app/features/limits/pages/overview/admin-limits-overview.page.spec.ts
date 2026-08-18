import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';

import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';
import type { ActiveLimitItem } from '../../data-access/admin-limits.models';
import { AdminLimitsOverviewPage } from './admin-limits-overview.page';

describe(AdminLimitsOverviewPage.name, () => {
  function createComponent() {
    TestBed.configureTestingModule({
      imports: [AdminLimitsOverviewPage],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        {
          provide: AdminLimitsApi,
          useValue: {
            overview: () => ({ subscribe: () => ({ unsubscribe: () => undefined }) }),
            listRules: () => ({ subscribe: () => undefined }),
            upsertAssignment: () => ({ subscribe: () => undefined }),
            deleteAssignment: () => ({ subscribe: () => undefined }),
          },
        },
        { provide: MatDialog, useValue: { open: () => ({ componentInstance: {}, afterClosed: () => ({ subscribe: () => undefined }) }) } },
        { provide: TranslateService, useValue: { instant: (k: string) => k } },
      ],
    });

    const fixture = TestBed.createComponent(AdminLimitsOverviewPage);
    const component = fixture.componentInstance;
    return component;
  }

  it('filteredLimits returns all items when scopeFilter is ALL', () => {
    const component = createComponent();
    component.overview.set({ activeLimits: [
      limitItem('a1', 'TENANT'),
      limitItem('a2', 'DRAW_CHANNEL'),
      limitItem('a3', 'SELLER_TERMINAL'),
    ] } as never);

    const result = component.filteredLimits();

    expect(result.length).toBe(3);
  });

  it('filteredLimits returns only TENANT items when filter is TENANT', () => {
    const component = createComponent();
    component.overview.set({ activeLimits: [
      limitItem('a1', 'TENANT'),
      limitItem('a2', 'DRAW_CHANNEL'),
    ] } as never);
    component.scopeFilter.set('TENANT');

    const result = component.filteredLimits();

    expect(result.length).toBe(1);
    expect(result[0].assignmentId).toBe('a1');
  });

  it('filteredLimits returns only DRAW_CHANNEL items when filter is DRAW_CHANNEL', () => {
    const component = createComponent();
    component.overview.set({ activeLimits: [
      limitItem('a1', 'TENANT'),
      limitItem('a2', 'DRAW_CHANNEL'),
      limitItem('a3', 'DRAW_CHANNEL'),
    ] } as never);
    component.scopeFilter.set('DRAW_CHANNEL');

    const result = component.filteredLimits();

    expect(result.length).toBe(2);
  });

  it('activeCount counts only enabled assignments', () => {
    const component = createComponent();
    component.overview.set({ activeLimits: [
      limitItem('a1', 'TENANT', { enabled: true }),
      limitItem('a2', 'TENANT', { enabled: false }),
      limitItem('a3', 'DRAW_CHANNEL', { enabled: true }),
    ] } as never);

    expect(component.activeCount()).toBe(2);
  });

  it('blockCount counts enabled NUMBER_BLOCK assignments only', () => {
    const component = createComponent();
    component.overview.set({ activeLimits: [
      limitItem('a1', 'TENANT', { enabled: true, group: 'NUMBER_BLOCK' }),
      limitItem('a2', 'TENANT', { enabled: true, group: 'NUMBER_CAP' }),
      limitItem('a3', 'TENANT', { enabled: false, group: 'NUMBER_BLOCK' }),
    ] } as never);

    expect(component.blockCount()).toBe(1);
    expect(component.capCount()).toBe(1);
  });

  it('inlineStats returns null when no active limits', () => {
    const component = createComponent();
    component.overview.set({ activeLimits: [
      limitItem('a1', 'TENANT', { enabled: false }),
    ] } as never);

    expect(component.inlineStats()).toBeNull();
  });

  it('inlineStats formats correctly with blocks and caps', () => {
    const component = createComponent();
    component.overview.set({ activeLimits: [
      limitItem('a1', 'TENANT', { enabled: true, group: 'NUMBER_BLOCK' }),
      limitItem('a2', 'TENANT', { enabled: true, group: 'NUMBER_CAP' }),
      limitItem('a3', 'DRAW_CHANNEL', { enabled: true, group: 'NUMBER_BLOCK' }),
    ] } as never);

    const stats = component.inlineStats();

    expect(stats).toContain('3 limit aktif');
    expect(stats).toContain('2 blokaj nimewo');
    expect(stats).toContain('1 limit miz');
  });

  it('inlineStats omits zero-count sections', () => {
    const component = createComponent();
    component.overview.set({ activeLimits: [
      limitItem('a1', 'TENANT', { enabled: true, group: 'NUMBER_BLOCK' }),
    ] } as never);

    const stats = component.inlineStats();

    expect(stats).toContain('1 limit aktif');
    expect(stats).toContain('1 blokaj nimewo');
    expect(stats).not.toContain('plafon');
  });
});

function limitItem(
  assignmentId: string,
  targetType: ActiveLimitItem['targetType'],
  overrides: Partial<ActiveLimitItem> = {},
): ActiveLimitItem {
  return {
    assignmentId,
    ruleKey: 'BLOCK_SELECTION_PER_DRAW',
    group: 'NUMBER_BLOCK',
    targetType,
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
