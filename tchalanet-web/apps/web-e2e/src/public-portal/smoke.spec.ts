import { expect, test } from '@playwright/test';

test('renders the public portal shell', async ({ page }) => {
  const response = await page.goto('/');

  expect(response?.ok()).toBe(true);
  await expect(page).toHaveTitle(/public-portal/i);
  await expect(page.locator('tch-public-shell')).toBeVisible();
});
