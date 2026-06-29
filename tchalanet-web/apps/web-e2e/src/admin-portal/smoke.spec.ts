import { expect, test } from '@playwright/test';

test('serves the admin portal', async ({ page }) => {
  const response = await page.goto('/login');

  expect(response?.ok()).toBe(true);
  await expect(page).toHaveTitle(/admin-portal/i);
  await expect(page.locator('tch-login-page')).toBeVisible();
});
