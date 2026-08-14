import { expect, test } from '../support/fixtures';
import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';

/**
 * Limits navigation simplification — Slice 8 regression guard.
 *
 * After the limits nav simplification:
 * - limits-global, limits-draw, limits-seller-terminal removed from the nav (both
 *   the limits section and the sellers section).
 * - limits-overview (/app/admin/limits) remains as a single top-level link.
 * - limits-number is reached from limit quick actions, not from the sidenav.
 * - The removed routes are still *routable* (Angular route exists) but absent from
 *   the sidenav.
 *
 * Selectors target `href`, not translated label text (convention from sidenav-desktop.spec.ts).
 */

const creds = credsFor('admin');

test.describe('Limits nav simplification', () => {
  test.beforeEach(async ({ loginPage, apiStub }) => {
    if (!creds) {
      test.skip(true, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');
      return;
    }
    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await loginPage.login(creds);
    await loginPage.page.goto('/app/admin');
  });

  for (const viewport of [
    { name: 'mobile 360dp', width: 360, height: 720 },
    { name: 'desktop 1280dp', width: 1280, height: 800 },
  ]) {
    test.describe(`viewport ${viewport.name}`, () => {
      test.use({ viewport: { width: viewport.width, height: viewport.height } });

      test('limits-global is absent from the sidenav', async ({ page }) => {
        await expect(
          page.locator('[data-testid="sidebar-child"][href="/app/admin/limits/global"]'),
        ).toHaveCount(0);
      });

      test('limits-draw is absent from the sidenav', async ({ page }) => {
        await expect(
          page.locator('[data-testid="sidebar-child"][href="/app/admin/limits/draw"]'),
        ).toHaveCount(0);
      });

      test('limits-seller-terminal is absent from both limits and sellers sections', async ({
        page,
      }) => {
        await expect(
          page.locator('[data-testid="sidebar-child"][href="/app/admin/limits/seller-terminal"]'),
        ).toHaveCount(0);
      });

      test('limits-overview (/app/admin/limits) is present and reachable', async ({ page }) => {
        await expect(page.locator('a[href="/app/admin/limits"]').first()).toBeVisible();
      });

      test('limits-number is absent from the sidenav', async ({ page }) => {
        await expect(
          page.locator('a[href="/app/admin/limits/number"]'),
        ).toHaveCount(0);
      });
    });
  }

  test('removed route /limits/global is still routable (not a 404)', async ({ page }) => {
    await page.goto('/app/admin/limits/global');
    // Angular route exists — must not render a 404 or error page.
    await expect(page.locator('tch-not-found, [data-testid="error-404"]')).toHaveCount(0);
  });

  test('removed route /limits/draw is still routable (not a 404)', async ({ page }) => {
    await page.goto('/app/admin/limits/draw');
    await expect(page.locator('tch-not-found, [data-testid="error-404"]')).toHaveCount(0);
  });
});
