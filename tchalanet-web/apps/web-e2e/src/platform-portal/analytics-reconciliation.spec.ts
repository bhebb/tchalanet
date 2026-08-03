import { expect, test } from '../support/fixtures';
import { credsFor } from '../support/env';

test.describe('Platform analytics reconciliation', () => {
  test('super admin validates a tenant and sees projection mismatches before rebuild', async ({
    page,
    loginPage,
    apiStub,
  }) => {
    const creds = credsFor('superAdmin');
    test.skip(!creds, 'TCH_E2E_SUPERADMIN_EMAIL/PASSWORD not configured');

    await apiStub.tenants([
      { tenantId: 'tenant-1', code: 'TCH', name: 'Tchalanet', status: 'ACTIVE' },
    ]);
    await apiStub.analyticsReconciliation({
      runId: 'reconciliation-1',
      tenantId: 'tenant-1',
      from: '2026-07-01',
      to: '2026-07-07',
      mode: 'VALIDATE',
      status: 'MISMATCH',
      completedAt: '2026-07-07T12:00:00Z',
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
    });

    await loginPage.login(creds!);
    await page.goto('/app/platform/ops/analytics/reconciliation');

    const tenantSearch = page.getByTestId('analytics-reconciliation-tenant');
    await tenantSearch.locator('input').fill('Tch');
    await page.getByRole('option', { name: /Tchalanet/ }).click();
    await page.getByTestId('analytics-reconciliation-validate').click();

    await expect(page.getByTestId('analytics-reconciliation-mismatches')).toBeVisible();
    await expect(page.getByTestId('analytics-reconciliation-rebuild')).toBeEnabled();
  });
});

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
