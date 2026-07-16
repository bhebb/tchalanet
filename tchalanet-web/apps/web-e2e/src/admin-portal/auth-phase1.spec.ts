import { expect, test } from '@playwright/test';

import { credsFor, loginViaUi, seededSellerTerminalId } from '../support/auth';

/**
 * Phase 1 — admin login, dispatch, guards & acting on a seller terminal
 * (admin-portal, :4302). UI-observable only. See specs/web-e2e-auth-phase1.
 */
test.describe('Phase 1 — admin', () => {
  test('unauthenticated access to /app/admin redirects to login', async ({ page }) => {
    await page.goto('/app/admin');
    await expect(page).toHaveURL(/\/login\b/);
    await expect(page.locator('tch-login-page')).toBeVisible();
  });

  test('admin login dispatches into the admin space', async ({ page }) => {
    const creds = credsFor('admin');
    test.skip(!creds, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');

    await loginViaUi(page, creds!);
    // spaceDispatchGuard lands the tenant-admin under /app/admin
    // (or /account/activation when the admin is not yet activated).
    await expect(page).toHaveURL(/\/(app\/admin|account\/activation)\b/);
  });

  test('admin can open a seller-terminal overrides screen', async ({ page }) => {
    const creds = credsFor('admin');
    const terminalId = seededSellerTerminalId();
    test.skip(!creds, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');
    test.skip(!terminalId, 'TCH_E2E_SELLER_TERMINAL_ID not configured');

    await loginViaUi(page, creds!);
    await page.goto(`/app/admin/seller-terminals/${terminalId}/overrides`);

    // The admin is operating on the selected terminal, not bounced to login.
    await expect(page.locator('tch-login-page')).toHaveCount(0);
    await expect(page).toHaveURL(new RegExp(`/seller-terminals/${terminalId}/overrides`));
  });
});
