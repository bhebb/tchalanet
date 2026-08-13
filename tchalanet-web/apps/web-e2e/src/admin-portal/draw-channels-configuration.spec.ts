import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';
import { expect, test } from '../support/fixtures';

const creds = credsFor('admin');

test.describe('Admin draw-channel configuration — desktop', () => {
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

  test('renders draw-channel configuration as a sellable draw list', async ({ page }) => {
    await page.goto('/app/admin/draw-channels');

    await expect(page.getByRole('heading', { name: /Canaux de tirage|Kanal tiraj/ })).toBeVisible();
    await expect(page.getByText(/5 canal|5 kanal|5 draw channel/)).toBeVisible();

    const firstCard = page.locator('.dc-page__channel-card').first();
    await expect(firstCard).toContainText('Texas · 1000');
    await expect(firstCard).not.toContainText('Haïti');
    await expect(firstCard).not.toContainText('HT · HT · 1000');
    await expect(firstCard.locator('.dc-page__logo img')).toBeVisible();
    await expect(firstCard).toContainText('10:00');
    await expect(firstCard).toContainText('09:55');
    await expect(firstCard).toContainText(/Tous les jours|Chak jou|Every day/);
    await expect(firstCard).not.toContainText(/Jeux|Jwèt|Games/);
    await expect(firstCard.getByRole('button', { name: /Configurer|Konfigire|Configure/ })).toBeVisible();

    const attentionCard = page.locator('.dc-page__channel-card--attention').filter({
      hasText: /Florida/,
    });
    await expect(attentionCard.first()).toBeVisible();
    await expect(attentionCard.first()).toContainText(/source|sous|Sous/);
  });
});
