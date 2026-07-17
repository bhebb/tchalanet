import { expect, test } from '../support/fixtures';
import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor, seededSellerTerminalId } from '../support/env';

/**
 * Phase 1 — admin login, dispatch, guards & acting on a seller terminal
 * (admin-portal, :4302). UI-observable only. See specs/web-e2e-auth-phase1.
 *
 * Authenticated tests use the emulator for the session and stub `/runtime/private`
 * with a TENANT_ADMIN bootstrap (variant A) — see api-stub.ts.
 */
test.describe('Phase 1 — admin', () => {
  test('unauthenticated access to /app/admin redirects to login', async ({
    page,
    loginPage,
  }) => {
    await page.goto('/app/admin');
    await expect(page).toHaveURL(/\/login\b/);
    await expect(loginPage.root).toBeVisible();
  });

  test('admin login dispatches into the admin space', async ({ page, loginPage, apiStub }) => {
    const creds = credsFor('admin');
    test.skip(!creds, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await loginPage.login(creds!);
    // spaceDispatchGuard lands the tenant-admin under /app/admin
    // (or /account/activation when the admin is not yet activated).
    await expect(page).toHaveURL(/\/(app\/admin|account\/activation)\b/);
  });

  test('admin can open a seller-terminal overrides screen', async ({
    page,
    loginPage,
    apiStub,
  }) => {
    const creds = credsFor('admin');
    const terminalId = seededSellerTerminalId();
    test.skip(!creds, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');
    test.skip(!terminalId, 'TCH_E2E_SELLER_TERMINAL_ID not configured');

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await loginPage.login(creds!);
    await page.goto(`/app/admin/seller-terminals/${terminalId}/overrides`);

    await expect(loginPage.root).toHaveCount(0);
    await expect(page).toHaveURL(new RegExp(`/seller-terminals/${terminalId}/overrides`));
  });
});
