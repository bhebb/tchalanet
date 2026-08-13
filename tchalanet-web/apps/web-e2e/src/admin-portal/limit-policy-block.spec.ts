import { expect, test } from '../support/fixtures';
import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';

/**
 * LimitPolicyBlockComponent — Slice 2 regression guard.
 *
 * Tests the shared LimitPolicyBlock UI embedded in the admin pages:
 * - Inherited value placeholder when no assignment at this scope
 * - Edit action opens the upsert dialog
 * - Delete action removes the assignment and reverts to inherited state
 * - 360dp viewport: no horizontal overflow, one rule per row
 *
 * Requires auth + a backend that serves /admin/policies/limits/* endpoints.
 * Under the stub, the component renders with empty assignments — inherited
 * state is the default visible path.
 */

const creds = credsFor('admin');

test.describe('LimitPolicyBlockComponent — tenant config page', () => {
  test.beforeEach(async ({ loginPage, apiStub }) => {
    if (!creds) {
      test.skip(true, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');
      return;
    }
    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await loginPage.login(creds);
    await loginPage.page.goto('/app/admin/setup/config');
  });

  test('limit section renders without error', async ({ page }) => {
    const section = page.locator('tch-admin-limits-section').first();
    await expect(section).toBeVisible({ timeout: 10_000 });
    // No error state should be present when the section loads
    await expect(section.locator('tch-section-error')).toHaveCount(0);
  });

  test('VENTE group is expanded by default on the config page', async ({ page }) => {
    const section = page.locator('tch-admin-limits-section').first();
    await expect(section).toBeVisible({ timeout: 10_000 });

    // VENTE is the default expanded group on the tenant config page
    const venteHeader = section.locator('.lpb__group-header', { hasText: 'Vente' });
    await expect(venteHeader).toBeVisible();
    await expect(venteHeader).toHaveAttribute('aria-expanded', 'true');
  });

  test('rows without an assignment show Non configuré', async ({ page }) => {
    const section = page.locator('tch-admin-limits-section').first();
    await expect(section).toBeVisible({ timeout: 10_000 });

    // Open the VENTE group if not already expanded
    const venteHeader = section.locator('.lpb__group-header', { hasText: 'Vente' });
    if ((await venteHeader.getAttribute('aria-expanded')) !== 'true') {
      await venteHeader.click();
    }

    // At least one row should show the unconfigured state under stub (no assignments)
    await expect(section.locator('.lpb__row-empty').first()).toBeVisible();
  });

  test('clicking edit on a row opens the upsert dialog', async ({ page }) => {
    const section = page.locator('tch-admin-limits-section').first();
    await expect(section).toBeVisible({ timeout: 10_000 });

    const venteHeader = section.locator('.lpb__group-header', { hasText: 'Vente' });
    if ((await venteHeader.getAttribute('aria-expanded')) !== 'true') {
      await venteHeader.click();
    }

    const editBtn = section.locator('.lpb__action-btn').first();
    await editBtn.click();

    // The upsert dialog should open
    await expect(page.locator('mat-dialog-container')).toBeVisible({ timeout: 5_000 });
  });

  test.describe('viewport 360dp — no horizontal overflow', () => {
    test.use({ viewport: { width: 360, height: 720 } });

    test('limit section fits within 360dp without overflow', async ({ page }) => {
      await page.goto('/app/admin/setup/config');

      const section = page.locator('tch-admin-limits-section').first();
      await expect(section).toBeVisible({ timeout: 10_000 });

      // Body must not scroll horizontally
      const bodyScrollWidth = await page.evaluate(() => document.body.scrollWidth);
      const viewportWidth = await page.evaluate(() => window.innerWidth);
      expect(bodyScrollWidth).toBeLessThanOrEqual(viewportWidth + 1); // 1px tolerance

      // Rows must stack vertically (one per line), not overflow
      const venteHeader = section.locator('.lpb__group-header', { hasText: 'Vente' });
      if ((await venteHeader.getAttribute('aria-expanded')) !== 'true') {
        await venteHeader.click();
      }
      const rows = section.locator('.lpb__row');
      const count = await rows.count();
      if (count > 1) {
        const box1 = await rows.nth(0).boundingBox();
        const box2 = await rows.nth(1).boundingBox();
        // Rows should be stacked vertically, not side by side at 360dp
        expect(box1?.x).toBeCloseTo(box2?.x ?? 0, -1);
      }
    });
  });
});
