import { expect, test } from '../support/fixtures';

/**
 * Titre du document par route. Seul un vrai navigateur peut le prouver : c'est `document.title`
 * qui alimente l'onglet, l'historique, les favoris et l'annonce du changement de page.
 *
 * Les assertions restent **agnostiques de la langue** — le portail démarre en créole
 * (`PORTAL_I18N_CONFIG.defaultLang = 'ht'`) et cette valeur n'a pas à figer un test de navigation.
 *
 * Cf. `libs/web/shell/src/lib/title/tch-title.strategy.ts`.
 */

/** Titre une fois les traductions chargées : avant, il ne porte que la marque. */
const TITLED = /.+ · Tchalanet/;

test.describe('public page titles', () => {
  test.beforeEach(async ({ apiStub }) => {
    void apiStub;
  });

  test('names the landing page, not just the brand', async ({ page }) => {
    await page.goto('/');

    await expect(page).toHaveTitle(TITLED);
  });

  test('renames the tab on navigation', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(TITLED);
    const home = await page.title();

    await page.goto('/public/results');
    await expect(page).toHaveTitle(TITLED);

    expect(await page.title()).not.toBe(home);
  });

  test('keeps a distinct title per route', async ({ page }) => {
    const titles: string[] = [];
    for (const path of ['/public/rules', '/public/check-ticket', '/public/help']) {
      await page.goto(path);
      await expect(page).toHaveTitle(TITLED);
      titles.push(await page.title());
    }

    expect(new Set(titles).size).toBe(titles.length);
  });
});
