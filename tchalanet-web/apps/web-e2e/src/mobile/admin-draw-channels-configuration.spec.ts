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
    await expect(firstCard.getByRole('button', { name: /Voir détails|Gade detay|View details/ })).toBeVisible();

    const configureButton = firstCard.getByRole('button', { name: /Configurer|Konfigire|Configure/ });
    const buttonBox = await configureButton.boundingBox();
    expect(buttonBox).not.toBeNull();
    if (!buttonBox) return;
    expect(buttonBox.height).toBeGreaterThanOrEqual(40);

    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    expect(overflow).toBeLessThanOrEqual(0);

    await configureButton.click();
    const dialog = page.getByRole('dialog');
    await expect(dialog.getByRole('heading', { name: /Vente|Vant|Sales/ })).toBeVisible();
    await expect(dialog.getByRole('heading', { name: /Informations système|Enfòmasyon sistèm|System information/ })).toBeVisible();
    await expect(dialog).toContainText(/Automatique|Otomatik|Automatic/);
    await expect(dialog).toContainText('Texas');
    await expect(dialog).toContainText(/Tous les jours|Chak jou|Every day/);
  });
});
