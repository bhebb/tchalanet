import { mkdirSync } from 'node:fs';
import { join } from 'node:path';

import { workspaceRoot } from '@nx/devkit';

import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';
import { expect, test } from '../support/fixtures';

const captureEnabled = process.env['TCH_CAPTURE_GUIDES'] === '1';
const creds = credsFor('admin');
const screenshotDir = join(
  workspaceRoot,
  '..',
  'tchalanet-docs',
  'docs',
  'assets',
  'guides',
  'admin',
);

async function capture(page: import('@playwright/test').Page, name: string): Promise<void> {
  await page.waitForLoadState('networkidle').catch(() => undefined);
  await page.waitForTimeout(300);
  await page.screenshot({
    path: join(screenshotDir, `${name}.png`),
    fullPage: false,
  });
}

test.describe('admin guide mobile screenshots', () => {
  test.skip(!captureEnabled, 'set TCH_CAPTURE_GUIDES=1 to write guide screenshots');
  test.skip(!creds, 'requires TCH_E2E_ADMIN_EMAIL / TCH_E2E_ADMIN_PASSWORD');

  test.beforeEach(async ({ loginPage, apiStub }) => {
    mkdirSync(screenshotDir, { recursive: true });
    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await apiStub.adminSellerTerminalDetail();
    await apiStub.adminSellerConfiguration();
    await apiStub.posSale();
    await loginPage.login(creds!);
  });

  test('captures the core admin mobile guide screens', async ({ page }) => {
    await page.goto('/app/admin/setup');
    await expect(page.getByTestId('private-nav-toggle')).toBeVisible();
    await capture(page, '01-configuration-admin');

    await page.getByTestId('private-nav-toggle').click();
    await expect(page.locator('tch-drawer-nav')).toBeVisible();
    await capture(page, '02-sidebar-mobile');

    const screens = [
      ['03-machann-machin-pos', '/app/admin/seller-terminals'],
      ['04-jeux-baremes', '/app/admin/games'],
      ['05-maryaj-gratis', '/app/admin/maryaj-gratis'],
      ['06-limites', '/app/admin/limits/global'],
      ['07-rapport-machann', '/app/admin/reports/sellers'],
      ['08-notifications', '/app/admin/notifications'],
      ['09-vann-ak-machin-pos', '/app/admin/pos/sale/stub-terminal-1'],
      ['10-fich-machann', '/app/admin/seller-terminals/stub-terminal-1'],
      ['11-reg-espesyal-machann', '/app/admin/seller-terminals/stub-terminal-1/overrides'],
    ] as const;

    for (const [name, route] of screens) {
      await page.goto(route);
      await capture(page, name);
    }

    await page.goto('/app/admin/limits/number');
    await capture(page, '06a-bloquer-numero');

    await page.locator('.limits-number__action-card').first().getByRole('button').click();
    await expect(page.getByTestId('limit-selection-input')).toBeVisible();
    await page.getByTestId('limit-selection-input').fill('13');
    await page.keyboard.press('Enter');
    await capture(page, '06b-formulaire-bloquer-numero');
  });
});
