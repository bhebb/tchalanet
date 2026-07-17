import { expect, test } from '../support/fixtures';
import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';

/**
 * Phase 3 — admin session flows: username login, logout, support banner
 * (admin-portal, :4302). UI-observable only. Emulator for the session + REST
 * stubs (variant A).
 */
test.describe('Phase 3 — admin session', () => {
  test('username login (resolved to email) dispatches into the admin space', async ({
    page,
    loginPage,
    apiStub,
  }) => {
    const creds = credsFor('admin');
    test.skip(!creds, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    // A bare username hits /public/auth/login-identifier/resolve → the seeded email.
    await apiStub.resolveLoginIdentifier(creds!.email);
    await loginPage.loginWithUsername('admin', creds!.password);

    await expect(page).toHaveURL(/\/(app\/admin|account\/activation)\b/);
  });

  test('logout returns to the login page', async ({ page, loginPage, shellPage, apiStub }) => {
    const creds = credsFor('admin');
    test.skip(!creds, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await loginPage.login(creds!);
    await expect(page).toHaveURL(/\/app\/admin\b/);

    await shellPage.logout();
    await expect(page).toHaveURL(/\/login\b/);
    await expect(loginPage.root).toBeVisible();
  });

  test('support banner shows the tenant when acting in support mode', async ({
    loginPage,
    shellPage,
    apiStub,
  }) => {
    const creds = credsFor('admin');
    test.skip(!creds, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    // The store re-hydrates from GET /platform/tenants/admin-access/current.
    await apiStub.startAdminAccess({
      sessionId: 'stub-support-session',
      tenantId: 'stub-tenant-1',
      tenantCode: 'ACME',
      tenantName: 'Acme Lottery',
      actorRole: 'SUPER_ADMIN',
      mode: 'SUPPORT_OVERRIDE',
      sensitiveDataMasked: false,
      startedAt: new Date().toISOString(),
    });
    await shellPage.seedSupportSession({
      tenantId: 'stub-tenant-1',
      tenantName: 'Acme Lottery',
      tenantCode: 'ACME',
    });

    await loginPage.login(creds!);
    await expect(shellPage.supportBanner).toBeVisible();
  });
});
