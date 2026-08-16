import { expect, test } from '../support/fixtures';
import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';

test.describe('Admin business V1 — setup, Maryaj gratis, limits, reports', () => {
  test.beforeEach(async ({ loginPage, apiStub }) => {
    test.skip(
      process.env['WEB_E2E_API'] === '1',
      'Admin business UI contract tests use deterministic REST stubs until the Docker API seed exposes matching data.',
    );
    const creds = credsFor('admin');
    if (!creds) {
      test.skip(true, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');
      return;
    }

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await loginPage.login(creds);
  });

  test('setup renders the tenant readiness checklist from backend sections', async ({ adminSetupPage }) => {
    await adminSetupPage.goto();

    await expect(adminSetupPage.checklist).toBeVisible();
    await adminSetupPage.expectReadinessCards();
  });

  test('setup keeps missing printing operational when sales readiness is complete', async ({
    adminSetupPage,
    apiStub,
  }) => {
    await apiStub.adminSetupPrintingMissingOperational();
    await adminSetupPage.goto();

    await adminSetupPage.expectReadySetupWithOperationalPrintingIssue();
  });

  test('Maryaj gratis renders config panels and saves seller-selection changes', async ({ adminMaryajGratisPage }) => {
    await adminMaryajGratisPage.goto();

    await adminMaryajGratisPage.expectPanels();
    await adminMaryajGratisPage.expectMainSummaryDisplay();
    expect(await adminMaryajGratisPage.saveSellerSelectionMode()).toBeTruthy();
  });

  test('limits renders active policies and blocks invalid number-limit saves', async ({ adminLimitsPage }) => {
    await adminLimitsPage.gotoGlobal();

    await adminLimitsPage.startAddingFirstAvailableRule();
    await adminLimitsPage.expectNumberSelectionRequired();

    await adminLimitsPage.addBlockedSelection('13');
    await expect(adminLimitsPage.save).toBeEnabled();
  });

  test('seller report shows terminal rows, French money formatting, CSV, and report-root PDF export', async ({
    adminSellerReportPage,
  }) => {
    await adminSellerReportPage.goto();

    await adminSellerReportPage.expectTerminalRows();
    await adminSellerReportPage.expectFrenchMoneyFormatting();

    expect(await adminSellerReportPage.exportCsvAndFilename())
      .toContain('rapport-vendeurs-2026-07-09-2026-07-17.csv');

    await adminSellerReportPage.expectPdfPrintScope();

    await adminSellerReportPage.installPrintSpy();
    await adminSellerReportPage.expectPdfActionPrints();
  });
});
