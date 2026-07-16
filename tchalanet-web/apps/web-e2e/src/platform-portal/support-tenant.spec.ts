import { expect, test } from '../support/fixtures';
import { credsFor } from '../support/env';

/**
 * Phase 2 — platform support mode (platform-portal, :4303).
 * Super-admin opens the support-tenant screen and starts admin access on a
 * tenant. UI-observable only. See specs/web-e2e-support-tenant-phase2.
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
  }) => {
    const creds = credsFor('superAdmin');
    test.skip(!creds, 'TCH_E2E_SUPERADMIN_EMAIL/PASSWORD not configured');

    await loginPage.login(creds!);
    await supportTenantPage.goto();
    // Either the tenant table or the empty-state is a valid rendered state.
    await expect(supportTenantPage.root).toBeVisible();
  });

  test('super admin opens the start-access dialog for a tenant', async ({
    loginPage,
    supportTenantPage,
  }) => {
    const creds = credsFor('superAdmin');
    test.skip(!creds, 'TCH_E2E_SUPERADMIN_EMAIL/PASSWORD not configured');

    await loginPage.login(creds!);
    await supportTenantPage.goto();

    const rows = await supportTenantPage.openAccessButtons.count();
    test.skip(rows === 0, 'no tenant rows in the support list (seed a tenant)');

    await supportTenantPage.openAccessForFirstTenant();
    await expect(supportTenantPage.accessSubmit).toBeVisible();
  });
});
