import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';
import { expect, test } from '../support/fixtures';

const creds = credsFor('admin');

test.describe('admin POS sale on a narrow viewport', () => {
  test.skip(!creds, 'requires TCH_E2E_ADMIN_EMAIL / TCH_E2E_ADMIN_PASSWORD');

  test.beforeEach(async ({ loginPage, apiStub }) => {
    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await apiStub.posSale();
    await loginPage.login(creds!);
  });

  test('accepts a ticket selection and stake on mobile', async ({ page }) => {
    await page.goto('/app/admin/pos/sale/stub-terminal-1');

    await expect(page.locator('tch-pos-ticket-line-editor')).toBeVisible();
    const horizontalOverflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    expect(horizontalOverflow).toBeLessThanOrEqual(0);

    const selection = page.locator('input.ple__num-input').first();
    const stake = page.locator('input.ple__stake-input').first();
    const add = page.getByRole('button', { name: 'Ajouter', exact: true });

    await selection.tap();
    await selection.pressSequentially('12');
    await stake.tap();
    await stake.pressSequentially('25');

    await expect(selection).toHaveValue('12');
    await expect(stake).toHaveValue('25');
    await expect(add).toBeEnabled();

    await add.click();

    await expect(page.locator('.ple__row')).toContainText('12');
    await expect(page.locator('.ple__amount')).toContainText(/25[,.]00/);
  });
});
