import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';
import { expect, test } from '../support/fixtures';

const creds = credsFor('admin');

test.describe('Admin draw-channel configuration — mobile', () => {
  test.beforeEach(async ({ loginPage, apiStub }) => {
    const adminCreds = creds;
    test.skip(!adminCreds, 'requires TCH_E2E_ADMIN_EMAIL / TCH_E2E_ADMIN_PASSWORD');
    test.skip(
      process.env['WEB_E2E_API'] === '1',
      'Draw channel UX contract uses deterministic REST stubs.',
    );
    if (!adminCreds) return;

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await apiStub.adminDrawChannels();
    await loginPage.login(adminCreds);
  });

  test('keeps each draw-channel card readable without horizontal scrolling', async ({ page }) => {
    await page.goto('/app/admin/draw-channels');

    const firstCard = page.locator('.dc-page__channel-card').first();
    await expect(firstCard).toBeVisible();
    await expect(firstCard).toContainText('Texas · 1000');
    await expect(firstCard).not.toContainText('Haïti');
    await expect(firstCard).not.toContainText('HT · HT · 1000');
    await expect(firstCard.locator('.dc-page__logo img')).toBeVisible();
    await expect(firstCard).toContainText('10:00');
    await expect(firstCard).toContainText('09:55');
    await expect(firstCard).toContainText(/Tous les jours|Chak jou|Every day/);
    await expect(firstCard).not.toContainText(/Jeux|Jwèt|Games/);
    await expect(firstCard.getByRole('button', { name: /Configurer|Konfigire|Configure/ })).toBeVisible();

    const configureButton = firstCard.getByRole('button', { name: /Configurer|Konfigire|Configure/ });
    const cardBox = await firstCard.boundingBox();
    const buttonBox = await configureButton.boundingBox();
    expect(cardBox).not.toBeNull();
    expect(buttonBox).not.toBeNull();
    if (!cardBox || !buttonBox) return;
    expect(buttonBox.width).toBeGreaterThan(cardBox.width * 0.8);

    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    expect(overflow).toBeLessThanOrEqual(0);
  });
});
