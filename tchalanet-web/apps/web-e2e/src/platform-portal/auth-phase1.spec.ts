import { expect, test } from '../support/fixtures';
import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor, seededTenantId } from '../support/env';

/**
 * Phase 1 — super-admin login, dispatch, guards & acting within a tenant
 * (platform-portal, :4303). UI-observable only. See specs/web-e2e-auth-phase1.
 */
test.describe('Phase 1 — platform', () => {
  test('unauthenticated access to /app/platform redirects to login', async ({
    page,
    loginPage,
  }) => {
    await page.goto('/app/platform');
    await expect(page).toHaveURL(/\/login\b/);
    await expect(loginPage.root).toBeVisible();
  });

  test('super admin login dispatches into the platform space', async ({
    page,
    loginPage,
    apiStub,
  }) => {
    const creds = credsFor('superAdmin');
    test.skip(!creds, 'TCH_E2E_SUPERADMIN_EMAIL/PASSWORD not configured');

    void apiStub; // stubs /runtime/private with a SUPER_ADMIN bootstrap
    await loginPage.login(creds!);
    // Platform space is served under /app/platform and at the guarded root.
    await expect(page).not.toHaveURL(/\/(login|forbidden)\b/);
  });

  test('super admin can open a tenant detail (acting within a tenant)', async ({
    page,
    loginPage,
    apiStub,
  }) => {
    const creds = credsFor('superAdmin');
    const tenantId = seededTenantId();
    test.skip(!creds, 'TCH_E2E_SUPERADMIN_EMAIL/PASSWORD not configured');
    test.skip(!tenantId, 'TCH_E2E_TENANT_ID not configured');

    void apiStub;
    await loginPage.login(creds!);
    // Opening a tenant scopes the platform screens to that tenant
    // (client reads through the asTenantAdmin / X-Tenant-Id override).
    await page.goto(`/app/platform/tenants/${tenantId}`);

    await expect(loginPage.root).toHaveCount(0);
    await expect(page).toHaveURL(new RegExp(`/tenants/${tenantId}\\b`));
  });

  test('tenant admin is blocked from the platform space', async ({
    page,
    loginPage,
    apiStub,
  }) => {
    const creds = credsFor('admin');
    test.skip(!creds, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await loginPage.login(creds!);
    await expect(page).not.toHaveURL(/\/app\/platform\b/);
    await expect(page).toHaveURL(/\/forbidden\b/);
  });
});
