import { expect, test } from '../support/fixtures';
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

  test('super admin login dispatches into the platform space', async ({ page, loginPage }) => {
    const creds = credsFor('superAdmin');
    test.skip(!creds, 'TCH_E2E_SUPERADMIN_EMAIL/PASSWORD not configured');

    await loginPage.login(creds!);
    // Platform space is served under /app/platform and at the guarded root.
    await expect(page).not.toHaveURL(/\/(login|forbidden)\b/);
  });

  test('super admin can open a tenant detail (acting within a tenant)', async ({
    page,
    loginPage,
  }) => {
    const creds = credsFor('superAdmin');
    const tenantId = seededTenantId();
    test.skip(!creds, 'TCH_E2E_SUPERADMIN_EMAIL/PASSWORD not configured');
    test.skip(!tenantId, 'TCH_E2E_TENANT_ID not configured');

    await loginPage.login(creds!);
    // Opening a tenant scopes the platform screens to that tenant
    // (client reads through the asTenantAdmin / X-Tenant-Id override).
    await page.goto(`/app/platform/tenants/${tenantId}`);

    await expect(loginPage.root).toHaveCount(0);
    await expect(page).toHaveURL(new RegExp(`/tenants/${tenantId}\\b`));
  });

  // Wrong-role handling on the platform portal involves a cross-app
  // (location.assign) redirect whose exact landing is environment-dependent;
  // confirm the observed behavior before asserting it. See open question in
  // specs/web-e2e-auth-phase1.
  test.fixme('tenant admin is blocked from the platform space', async ({ page, loginPage }) => {
    const creds = credsFor('admin');
    test.skip(!creds, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');

    await loginPage.login(creds!);
    await expect(page).not.toHaveURL(/\/app\/platform\/(dashboard|tenants)/);
  });
});
