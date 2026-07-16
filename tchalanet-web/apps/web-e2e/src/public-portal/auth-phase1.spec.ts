import { expect, test } from '../support/fixtures';

/**
 * Phase 1 — public baseline & login entry (public-portal, :4301).
 * UI-observable only. See specs/web-e2e-auth-phase1.
 */
test.describe('Phase 1 — public', () => {
  test('public shell is reachable anonymously', async ({ page, loginPage }) => {
    const response = await page.goto('/');
    expect(response?.ok()).toBe(true);
    await expect(loginPage.root).toHaveCount(0);
  });

  test('login page is reachable from the public app', async ({ loginPage }) => {
    await loginPage.goto();
    await expect(loginPage.email).toBeVisible();
    await expect(loginPage.submit).toBeVisible();
  });

  test('invalid credentials surface an inline error and do not navigate', async ({
    loginPage,
  }) => {
    await loginPage.loginExpectingError({
      email: 'nobody@example.com',
      password: 'wrong-password',
    });
  });
});
