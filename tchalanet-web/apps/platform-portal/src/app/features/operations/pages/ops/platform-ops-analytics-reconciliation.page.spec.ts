import '@angular/compiler';

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatDialog } from '@angular/material/dialog';
import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import {
  AnalyticsReconciliationRequest,
  AnalyticsReconciliationResponse,
  PlatformOpsApi,
} from '../../data-access/platform-ops-api.service';
import { PlatformTenantsApi, TenantSummaryView } from '../../../tenants/data-access/platform-tenants-api.service';
import { PlatformOpsAnalyticsReconciliationPage } from './platform-ops-analytics-reconciliation.page';

describe('PlatformOpsAnalyticsReconciliationPage', () => {
  it('validates the selected tenant and date window', async () => {
    const api = opsApi();
    const fixture = await createPage(api);
    const page = fixture.componentInstance;
    page.selectedTenant.set(tenant());
    page.fromControl.setValue('2026-07-01');
    page.toControl.setValue('2026-07-07');

    page.validate();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(api.reconcileAnalytics).toHaveBeenCalledWith(
      {
        tenantId: 'tenant-1',
        from: '2026-07-01',
        to: '2026-07-07',
        mode: 'VALIDATE',
        repairReason: undefined,
      } satisfies AnalyticsReconciliationRequest,
      { suppressShellFeedback: true },
    );
    expect(fixture.nativeElement.querySelector('[data-testid="analytics-reconciliation-success"]')).not.toBeNull();
  });

  it('does not call the API until tenant and a valid range are provided', async () => {
    const api = opsApi();
    const fixture = await createPage(api);
    const page = fixture.componentInstance;
    page.fromControl.setValue('2026-07-08');
    page.toControl.setValue('2026-07-01');

    page.validate();
    fixture.detectChanges();

    expect(api.reconcileAnalytics).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('platform.analyticsReconciliation.validation.scope');
  });

  it('only enables rebuilding after a mismatch', async () => {
    const api = opsApi({ reconcileAnalytics: vi.fn(() => of(mismatchResult())) });
    const fixture = await createPage(api);
    const page = fixture.componentInstance;
    page.selectedTenant.set(tenant());

    page.validate();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(page.canRebuild()).toBe(true);
    expect(fixture.nativeElement.querySelector('[data-testid="analytics-reconciliation-mismatches"]')).not.toBeNull();
  });
});

async function createPage(api: ReturnType<typeof opsApi>): Promise<ComponentFixture<PlatformOpsAnalyticsReconciliationPage>> {
  await TestBed.configureTestingModule({
    imports: [PlatformOpsAnalyticsReconciliationPage],
    providers: [
      provideNoopAnimations(),
      provideTranslateService(),
      { provide: PlatformOpsApi, useValue: api },
      { provide: PlatformTenantsApi, useValue: { listTenants: vi.fn(() => of({ items: [] })) } },
      { provide: MatDialog, useValue: { open: vi.fn() } },
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(PlatformOpsAnalyticsReconciliationPage);
  fixture.detectChanges();
  await fixture.whenStable();
  return fixture;
}

function opsApi(overrides: Partial<Record<keyof PlatformOpsApi, unknown>> = {}) {
  return {
    reconcileAnalytics: vi.fn(() => of(successResult())),
    ...overrides,
  } as unknown as PlatformOpsApi & { reconcileAnalytics: ReturnType<typeof vi.fn> };
}

function tenant(): TenantSummaryView {
  return {
    id: 'tenant-1',
    tenantId: 'tenant-1',
    name: 'Tchalanet',
    code: 'TCH',
    status: 'ACTIVE',
    currency: 'HTG',
  };
}

function successResult(): AnalyticsReconciliationResponse {
  return {
    runId: 'run-1',
    tenantId: 'tenant-1',
    from: '2026-07-01',
    to: '2026-07-07',
    mode: 'VALIDATE',
    status: 'SUCCESS',
    mismatches: [],
    completedAt: '2026-07-07T12:00:00Z',
  };
}

function mismatchResult(): AnalyticsReconciliationResponse {
  return {
    ...successResult(),
    status: 'MISMATCH',
    mismatches: [
      {
        projection: 'DAILY',
        businessDate: '2026-07-01',
        drawId: null,
        sellerTerminalId: null,
        expected: metricSnapshot(1_000),
        observed: metricSnapshot(500),
      },
    ],
  };
}

function metricSnapshot(grossSalesMinor: number) {
  return {
    ticketsSold: 1,
    ticketsCancelled: 0,
    grossSalesMinor,
    stakeTotalMinor: grossSalesMinor,
    winningsCalculatedMinor: 0,
    payoutsPaidMinor: 0,
    sellerCommissionMinor: 0,
    buyerChargeMinor: 0,
    sellerChargeMinor: 0,
    tenantChargeMinor: 0,
    waivedChargeMinor: 0,
    promotionLineCount: 0,
    promotionPricedLineCount: 0,
  };
}
