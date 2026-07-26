import { expect, test } from '../support/fixtures';

/**
 * Navigation repliée du portail public — ce que seuls un vrai moteur de rendu et un vrai
 * viewport peuvent prouver. Les trois projets Playwright historiques sont en Desktop Chrome et
 * ne voient jamais le burger.
 *
 * Contrat vérifié ici : une seule frontière à 840px (`up(expanded)`).
 * Cf. `openspec/changes/web-nav-mobile-hardening-v1/design.md` §0.
 */

const toggle = '[data-testid="public-nav-toggle"]';
const inlineNav = '[data-testid="public-nav-inline"]';

test.describe('public navigation on a narrow viewport', () => {
  test.beforeEach(async ({ page, apiStub }) => {
    void apiStub;
    await page.goto('/');
    await expect(page.locator('tch-public-shell')).toBeVisible();
  });

  test('offers the burger and hides the inline navigation', async ({ page }) => {
    await expect(page.locator(toggle)).toBeVisible();
    await expect(page.locator(inlineNav).first()).toBeHidden();
    await expect(page.locator(inlineNav).last()).toBeHidden();
  });

  test('never scrolls sideways', async ({ page }) => {
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );

    expect(overflow).toBeLessThanOrEqual(0);
  });

  test('opens a modal menu, closes it on Escape, and hands the focus back', async ({ page }) => {
    await page.locator(toggle).click();

    const menu = page.getByRole('dialog');
    await expect(menu).toBeVisible();
    await expect(menu).toHaveAttribute('aria-modal', 'true');
    // Le focus est piégé : il ne peut pas atteindre le contenu derrière le scrim.
    await page.keyboard.press('Tab');
    await expect(menu.locator(':focus')).toHaveCount(1);

    await page.keyboard.press('Escape');

    await expect(menu).toBeHidden();
    await expect(page.locator(toggle)).toBeFocused();
  });

  test('closes the menu after navigating to a destination', async ({ page }) => {
    await page.locator(toggle).click();
    const menu = page.getByRole('dialog');
    await expect(menu).toBeVisible();

    await menu.getByRole('link').first().click();

    await expect(menu).toBeHidden();
  });
});

test.describe('the 840px boundary', () => {
  test.beforeEach(async ({ page, apiStub }) => {
    void apiStub;
    await page.goto('/');
    await expect(page.locator('tch-public-shell')).toBeVisible();
  });

  test('keeps the burger up to 839px and swaps to inline navigation at 840px', async ({ page }) => {
    await page.setViewportSize({ width: 839, height: 844 });
    await expect(page.locator(toggle)).toBeVisible();
    await expect(page.locator(inlineNav).first()).toBeHidden();

    await page.setViewportSize({ width: 840, height: 844 });
    await expect(page.locator(toggle)).toBeHidden();
    await expect(page.locator(inlineNav).first()).toBeVisible();
  });

  test('closes an open menu when the window crosses the boundary', async ({ page }) => {
    await page.locator(toggle).click();
    await expect(page.getByRole('dialog')).toBeVisible();

    await page.setViewportSize({ width: 1024, height: 844 });

    await expect(page.getByRole('dialog')).toBeHidden();
  });
});
