import { expect, test } from '../support/fixtures';

test('renders the public portal shell', async ({ page, apiStub }) => {
  void apiStub;
  const response = await page.goto('/');

  expect(response?.ok()).toBe(true);
  await expect(page).toHaveTitle(/tchalanet/i);
  await expect(page.locator('tch-public-shell')).toBeVisible();
});
