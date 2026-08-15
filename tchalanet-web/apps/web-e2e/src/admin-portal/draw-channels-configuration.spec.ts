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

    await expect(
      page.getByRole('heading', {
        name: /Configuration des tirages|Konfigirasyon tiraj yo|Draw configuration/,
      }),
    ).toBeVisible();
    await expect(page.getByText(/5 canal|5 kanal|5 draw channel/)).toBeVisible();

    const firstCard = page.locator('.dc-channel-card').first();
    await expect(firstCard).toContainText('Texas · 1000');
    await expect(firstCard).not.toContainText('Haïti');
    await expect(firstCard).not.toContainText('HT · HT · 1000');
    await expect(firstCard.locator('.dc-channel-card__logo img')).toBeVisible();
    await expect(firstCard).toContainText('10:00');
    await expect(firstCard).toContainText('09:55');
    await expect(firstCard).toContainText(/Tous les jours|Chak jou|Every day/);
    await expect(firstCard).not.toContainText(/Jeux|Jwèt|Games/);
    await expect(
      firstCard.getByRole('button', { name: /Configurer|Konfigire|Configure/ }),
    ).toBeVisible();
    await firstCard.getByRole('button', { name: /Plus d’actions|Plis aksyon|More actions/ }).click();
    await expect(page.getByRole('menuitem', { name: /Voir détails|Gade detay|View details/ })).toBeVisible();
    await page.keyboard.press('Escape');
    await firstCard.getByRole('button', { name: /Configurer|Konfigire|Configure/ }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog.getByRole('heading', { name: /Vente|Vant|Sales/ })).toBeVisible();
    await expect(
      dialog.getByRole('heading', {
        name: /Informations système|Enfòmasyon sistèm|System information/,
      }),
    ).toHaveCount(0);
    await expect(dialog).toContainText('Texas');
    await expect(
      dialog.getByLabel(/Ouverture des ventes|Lè tiraj sa louvri|Sales open/),
    ).toHaveValue('00:00');
    await expect(dialog).toContainText(/Tous les jours|Chak jou|Every day/);
    await page.keyboard.press('Escape');

    await firstCard.getByRole('button', { name: /Plus d’actions|Plis aksyon|More actions/ }).click();
    await page.getByRole('menuitem', { name: /Voir détails|Gade detay|View details/ }).click();
    await expect(page).toHaveURL(/\/app\/admin\/draw-channels\/channel-ht-1000$/);
    await expect(
      page.getByTestId('admin-page-header').getByRole('heading', { name: /Texas · 1000/ }),
    ).toBeVisible();
    const detailContent = page.locator('tch-console-entity-detail');
    await expect(page.getByTestId('draw-channel-detail-logo').locator('img')).toBeVisible();
    await expect(detailContent.getByRole('heading', { name: /Tirage|Tiraj|Draw/ })).toBeVisible();
    await expect(
      detailContent.getByRole('heading', { name: /Résultats|Rezilta|Results/ }),
    ).toBeVisible();
    await expect(detailContent.getByRole('heading', { name: /Vente|Vant|Sales/ })).toBeVisible();
    await expect(detailContent).toContainText(/Source|Sous|Fournisseur/);
    await page.goBack();

    const attentionCard = page.locator('.dc-channel-card--attention').filter({
      hasText: /Florida/,
    });
    await expect(attentionCard.first()).toBeVisible();
    await expect(attentionCard.first()).toContainText(/source|sous|Sous/);
  });
});
