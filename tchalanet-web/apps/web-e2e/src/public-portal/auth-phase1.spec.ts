import { expect, test } from '@playwright/test';

import { loginViaUi } from '../support/auth';

/**
 * Phase 1 — public baseline & login entry (public-portal, :4301).
 * UI-observable only. See specs/web-e2e-auth-phase1.
 */
test.describe('Phase 1 — public', () => {
  test('public shell is reachable anonymously', async ({ page }) => {
    const response = await page.goto('/');
    expect(response?.ok()).toBe(true);
    // No authenticated shell / login required to view the public site.
    await expect(page.locator('tch-login-page')).toHaveCount(0);
  });

  test('login page is reachable from the public app', async ({ page }) => {
    const response = await page.goto('/login');
    expect(response?.ok()).toBe(true);
    await expect(page.locator('tch-login-page')).toBeVisible();
    await expect(page.getByTestId('login-email')).toBeVisible();
    await expect(page.getByTestId('login-submit')).toBeVisible();
  });

  test('invalid credentials surface an inline error and do not navigate', async ({
    page,
  }) => {
    await loginViaUi(
      page,
      { email: 'nobody@example.com', password: 'wrong-password' },
      { expectFailure: true },
    );
  });
});
