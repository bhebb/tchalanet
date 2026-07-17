import { expect, test } from '../support/fixtures';
import { credsFor } from '../support/env';

/**
 * Phase 2 — platform support mode (platform-portal, :4303).
 * Super-admin opens the support-tenant screen and starts admin access on a
 * tenant. UI-observable only. See specs/web-e2e-support-tenant-phase2.
 *
 * Authenticated tests use the emulator for the session; `/runtime/private` and
 * the tenant list are stubbed (variant A) — see api-stub.ts.
 */
test.describe('Phase 2 — support tenant', () => {
  test('unauthenticated access to support-tenant redirects to login', async ({
    page,
    loginPage,
  }) => {
    await page.goto('/app/platform/support-tenant');
    await expect(page).toHaveURL(/\/login\b/);
    await expect(loginPage.root).toBeVisible();
  });

  test('super admin opens the support-tenant screen', async ({
    loginPage,
    supportTenantPage,
    apiStub,
  }) => {
    const creds = credsFor('superAdmin');
    test.skip(!creds, 'TCH_E2E_SUPERADMIN_EMAIL/PASSWORD not configured');

    void apiStub; // SUPER_ADMIN bootstrap + empty tenants → empty-state is valid
    await loginPage.login(creds!);
    await supportTenantPage.goto();
    await expect(supportTenantPage.root).toBeVisible();
  });

  test('super admin opens the start-access dialog for a tenant', async ({
    loginPage,
    supportTenantPage,
    apiStub,
  }) => {
    const creds = credsFor('superAdmin');
    test.skip(!creds, 'TCH_E2E_SUPERADMIN_EMAIL/PASSWORD not configured');

    await apiStub.tenants([
      { tenantId: 'stub-tenant-1', code: 'ACME', name: 'Acme Lottery', status: 'ACTIVE' },
    ]);
    await loginPage.login(creds!);
    await supportTenantPage.goto();

    await supportTenantPage.openAccessForFirstTenant();
    await expect(supportTenantPage.accessSubmit).toBeVisible();
  });
});
