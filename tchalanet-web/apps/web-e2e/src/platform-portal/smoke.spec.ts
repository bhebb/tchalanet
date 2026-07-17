import { expect, test } from '@playwright/test';

test('serves the platform portal', async ({ page }) => {
  const response = await page.goto('/login');

  expect(response?.ok()).toBe(true);
  await expect(page).toHaveTitle(/tchalanet/i);
  await expect(page.locator('tch-login-page')).toBeVisible();
});
