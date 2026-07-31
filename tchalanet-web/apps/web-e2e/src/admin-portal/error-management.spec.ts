import type { Page } from '@playwright/test';

import { expect, test } from '../support/fixtures';
import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';

const creds = credsFor('admin');
const backendDiagnostic = 'backend failure: diagnostic text must not reach the UI';

test.describe('Admin error management — blocking page failures', () => {
  test.beforeEach(async ({ loginPage, apiStub }) => {
    test.skip(
      process.env['WEB_E2E_API'] === '1',
      'Admin error-management UI contract tests use deterministic ProblemDetail stubs.',
    );
    test.skip(!creds, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await loginPage.login(creds!);
  });

  async function expectBlockingError(page: Page): Promise<void> {
    const alert = page.getByRole('alert');
    await expect(alert).toBeVisible();
    await expect(alert.locator('h2')).toBeVisible();
    await expect(alert).not.toContainText(backendDiagnostic);
    const retry = alert.getByRole('button');
    await expect(retry).toHaveCount(1);
    await retry.click();
    await expect(alert).toBeVisible();
  }

  test('dashboard owns the failure when PageModel and fallback are unavailable', async ({
    page,
    apiStub,
  }) => {
    await apiStub.adminDashboardError();

    await page.goto('/app/admin/dashboard');

    await expectBlockingError(page);
  });

  test('configuration générale owns the overview failure and exposes retry', async ({
    page,
    apiStub,
  }) => {
    await apiStub.adminSetupError();

    await page.goto('/app/admin/setup');

    await expectBlockingError(page);
  });

  test('rapport owns the overview resource failure and exposes retry', async ({
    page,
    apiStub,
  }) => {
    await apiStub.adminReportError();

    await page.goto('/app/admin/reports/overview');

    await expectBlockingError(page);
  });

  test('draws owns the generated-draw list failure and exposes retry', async ({
    page,
    apiStub,
  }) => {
    await apiStub.adminDrawsError();

    await page.goto('/app/admin/draws');

    await expectBlockingError(page);
  });
});
