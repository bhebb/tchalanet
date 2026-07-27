import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';
import { expect, test } from '../support/fixtures';

/**
 * Drawer à deux niveaux de la console admin, sur un viewport étroit.
 *
 * C'est le seul endroit où ce rendu peut être observé : il n'existe que sous 840px et derrière une
 * session authentifiée. Les tests unitaires couvrent la structure ; ceux-ci prouvent qu'elle tient
 * dans un vrai navigateur, avec le menu que le backend (ou son repli) fournit réellement.
 */

const creds = credsFor('admin');

const toggle = '[data-testid="private-nav-toggle"]';
const category = '[data-testid="drawer-category"]';
const panel = '[data-testid="drawer-panel"]';
const back = '[data-testid="drawer-back"]';
const row = '[data-testid="drawer-row"]';

/**
 * Le drawer et le panneau glissent tous deux sur 200ms. `toBeVisible()` ne les attend pas — une
 * mesure prise trop tôt lit une position intermédiaire et fait conclure à un défaut de rendu.
 */
async function settled(page: import('@playwright/test').Page, selector: string): Promise<void> {
  await page.waitForFunction(
    sel => {
      const node = document.querySelector(sel);
      if (!node) return false;
      const { x } = node.getBoundingClientRect();
      return Math.abs(x) < 1;
    },
    selector,
    { timeout: 5_000 },
  );
}

test.describe('admin console drawer on a narrow viewport', () => {
  test.skip(!creds, 'requires TCH_E2E_ADMIN_EMAIL / TCH_E2E_ADMIN_PASSWORD');

  test.beforeEach(async ({ loginPage, apiStub }) => {
    // `navigationDrawer: null` dans ce bootstrap : le shell retombe sur le modèle statique, donc
    // ces tests observent aussi le chemin de repli.
    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await loginPage.login(creds!);
  });

  test('shows the root level with category cards', async ({ page }) => {
    await page.locator(toggle).click();
    await settled(page, 'tch-drawer-nav');

    await expect(page.locator(category).first()).toBeVisible();
    await expect(page.locator(panel)).toHaveCount(0);
    // Le menu admin compte plusieurs groupes : ils deviennent tous des cartes.
    expect(await page.locator(category).count()).toBeGreaterThan(1);
  });

  test('opens a category panel and closes only that level', async ({ page }) => {
    await page.locator(toggle).click();
    const firstCategory = page.locator(category).first();
    const label = (await firstCategory.textContent())?.trim();

    await firstCategory.click();
    await expect(page.locator(panel)).toBeVisible();
    await expect(page.locator(`${panel} ${row}`).first()).toBeVisible();

    await page.locator(back).click();

    await expect(page.locator(panel)).toHaveCount(0);
    await expect(page.locator(category).first()).toBeVisible();
    expect((await page.locator(category).first().textContent())?.trim()).toBe(label);
  });

  test('never repeats the landing entry the panel title already leads to', async ({ page }) => {
    await page.locator(toggle).click();
    await page.locator(category).first().click();

    const title = page.locator(`${panel} h2`);
    await expect(title).toBeVisible();
    const heading = (await title.textContent())?.trim();

    const rows = await page.locator(`${panel} ${row}`).allTextContents();
    const link = page.locator(`${panel} a[href]`).first();

    // Quand le titre est un lien, il mène à une route qu'aucune ligne ne redouble.
    if ((await link.count()) > 0) {
      const target = await link.getAttribute('href');
      const duplicates = await page.locator(`${panel} ${row}[href="${target}"]`).count();
      expect(duplicates).toBe(0);
    }
    expect(rows.map(text => text.trim())).not.toContain(heading);
  });

  test('covers the root level instead of blending with it', async ({ page }) => {
    await page.locator(toggle).click();
    await settled(page, 'tch-drawer-nav');
    const card = page.locator(category).first();
    const box = (await card.boundingBox())!;

    await card.click();
    await expect(page.locator(panel)).toBeVisible();
    await settled(page, panel);

    // `toBeVisible` ne suffit pas : un panneau transparent ou mal empilé reste « visible ».
    // On demande au navigateur ce qu'il y a réellement sous le point où était la carte.
    const onTop = await page.evaluate(
      ({ x, y }) => {
        const node = document.elementFromPoint(x, y);
        return !!node?.closest('[data-testid="drawer-panel"]');
      },
      { x: box.x + box.width / 2, y: box.y + box.height / 2 },
    );
    expect(onTop).toBe(true);

    // Et la racine ne doit plus être atteignable au clavier derrière lui.
    await expect(page.locator('tch-drawer-nav > nav[inert]')).toHaveCount(1);
  });

  test('closes the panel then the drawer on Escape', async ({ page }) => {
    await page.locator(toggle).click();
    await page.locator(category).first().click();
    await expect(page.locator(panel)).toBeVisible();

    await page.keyboard.press('Escape');
    await expect(page.locator(panel)).toHaveCount(0);
    await expect(page.locator(category).first()).toBeVisible();

    await page.keyboard.press('Escape');
    await expect(page.locator(`${toggle}[aria-expanded="false"]`)).toBeVisible();
  });

  test('never scrolls sideways', async ({ page }) => {
    await page.locator(toggle).click();

    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );

    expect(overflow).toBeLessThanOrEqual(0);
  });
});
