import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';
import { expect, test } from '../support/fixtures';

const creds = credsFor('admin');

test.describe('Admin games configuration — mobile', () => {
  test.beforeEach(async ({ loginPage, apiStub }) => {
    const adminCreds = creds;
    test.skip(!adminCreds, 'requires TCH_E2E_ADMIN_EMAIL / TCH_E2E_ADMIN_PASSWORD');
    test.skip(process.env['WEB_E2E_API'] === '1', 'Games UX contract uses deterministic REST stubs.');
    if (!adminCreds) return;

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await apiStub.adminBusiness();
    await loginPage.login(adminCreds);
  });

  test('keeps game configuration cards readable without horizontal scrolling', async ({ page }) => {
    await page.goto('/app/admin/games');

    const firstCard = page.locator('.gp-game-card').first();
    await expect(firstCard).toBeVisible();
    await expect(firstCard).toContainText(/Bolèt|Bolet/);
    await expect(firstCard.locator('.gp-game-card__logo img')).toBeVisible();
    await expect(firstCard).toContainText(/POS/);
    await expect(firstCard).toContainText(/Mises|Miz|Stakes/);
    await expect(firstCard).toContainText(/Barème|Barèm|Payouts/);

    const configureButton = firstCard.getByRole('button', {
      name: /Configurer|Konfigire|Configure/,
    });
    await expect(configureButton).toBeVisible();
    const buttonBox = await configureButton.boundingBox();
    expect(buttonBox).not.toBeNull();
    expect(buttonBox?.height ?? 0).toBeGreaterThanOrEqual(40);

    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    expect(overflow).toBeLessThanOrEqual(0);
  });
});
