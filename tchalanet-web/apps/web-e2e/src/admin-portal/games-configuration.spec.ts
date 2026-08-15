import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';
import { expect, test } from '../support/fixtures';

const creds = credsFor('admin');

test.describe('Admin games configuration — desktop', () => {
  test.beforeEach(async ({ loginPage, apiStub }) => {
    const adminCreds = creds;
    test.skip(!adminCreds, 'requires TCH_E2E_ADMIN_EMAIL / TCH_E2E_ADMIN_PASSWORD');
    test.skip(process.env['WEB_E2E_API'] === '1', 'Games UX contract uses deterministic REST stubs.');
    if (!adminCreds) return;

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await apiStub.adminBusiness();
    await loginPage.login(adminCreds);
  });

  test('renders games as a focused sales configuration list', async ({ page }) => {
    await page.goto('/app/admin/games');

    await expect(page.getByRole('heading', { name: /Jeux disponibles|Jwèt disponib|Available games/ })).toBeVisible();

    const firstCard = page.locator('.gp-game-card').first();
    await expect(firstCard).toContainText(/Bolèt|Bolet/);
    await expect(firstCard.locator('.gp-game-card__logo img')).toBeVisible();
    await expect(firstCard).toContainText(/Mises|Miz|Stakes/);
    await expect(firstCard).toContainText(/Barème|Barèm|Payouts/);
    await expect(firstCard).toContainText(/Disponibilité|Disponib|Available/);
    await expect(
      firstCard.getByRole('button', { name: /Modifier le jeu|Modifye jwèt la|Edit game|Configurer|Konfigire|Configure/ }),
    ).toBeVisible();
    await expect(
      firstCard.getByRole('button', { name: /Jeux par tirage|Jwèt pa tiraj|Games by draw/ }),
    ).toHaveCount(0);
    await expect(
      page.getByRole('link', { name: /Disponibilité|Disponibilite|Jeux par tirage|Jwèt pa tiraj|Games by draw/ }),
    ).toBeVisible();

    const needsAttentionCard = page.locator('.gp-game-card--attention').filter({ hasText: /Loto 4/ });
    await expect(needsAttentionCard).toContainText(/À configurer|Pou konfigire|Needs setup/);
    await expect(needsAttentionCard).toContainText(/Barème non configuré|Barèm pa konfigire|Pricing not configured/);

    const inactiveCard = page.locator('.gp-game-card--inactive').filter({ hasText: /Loto 5/ });
    await expect(inactiveCard).toContainText(/Inactif|Inaktif|Inactive/);
  });
});
