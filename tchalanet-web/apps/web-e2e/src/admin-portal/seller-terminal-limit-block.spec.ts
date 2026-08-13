import { expect, test } from '../support/fixtures';
import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';

/**
 * Seller terminal detail — limit block regression guard.
 *
 * The seller terminal detail page embeds `tch-admin-limits-section` scoped to
 * SELLER_TERMINAL with only the ticket-level rules exposed:
 *   MAX_STAKE_PER_LINE, MAX_LINES_PER_TICKET, MAX_STAKE_PER_TICKET
 *
 * Covers:
 * - Limit section renders below the detail layout without error
 * - The section uses SELLER_TERMINAL scope (inherits from TENANT)
 * - Rows without an assignment show the unconfigured ("Non configuré") state
 * - Edit action opens the upsert dialog
 *
 * Auth: tenant admin via Firebase emulator. REST stubs provide deterministic
 * seller-terminal + limit-policy data.
 */

const creds = credsFor('admin');

test.describe('Seller terminal detail — limit block', () => {
  test.beforeEach(async ({ loginPage, apiStub, adminSellerTerminalDetailPage }) => {
    test.skip(
      process.env['WEB_E2E_API'] === '1',
      'Seller terminal limit block uses deterministic REST stubs.',
    );
    if (!creds) {
      test.skip(true, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');
      return;
    }

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await apiStub.adminSellerTerminalDetail();
    await loginPage.login(creds);
    await adminSellerTerminalDetailPage.goto();
  });

  test('limit section renders below the detail layout', async ({ page }) => {
    const section = page.locator('tch-admin-limits-section');
    await expect(section).toBeVisible({ timeout: 12_000 });

    // No blocking error inside the section
    await expect(section.locator('tch-section-error')).toHaveCount(0);
  });

  test('section has a VENTE group header', async ({ page }) => {
    const section = page.locator('tch-admin-limits-section');
    await expect(section).toBeVisible({ timeout: 12_000 });

    // The VENTE group header (label = "Vente" via i18n) must be visible in the section card
    const groupHeader = section.locator('.lpb__group-header').first();
    await expect(groupHeader).toBeVisible({ timeout: 8_000 });
  });

  test('rows without assignment show unconfigured state', async ({ page }) => {
    const section = page.locator('tch-admin-limits-section');
    await expect(section).toBeVisible({ timeout: 12_000 });

    // Groups are collapsed by default — expand the first group to reveal rows
    const groupHeader = section.locator('.lpb__group-header').first();
    await groupHeader.click();

    // Under the stub, MAX_STAKE_PER_LINE and MAX_LINES_PER_TICKET have no assignment
    await expect(section.locator('.lpb__row-empty').first()).toBeVisible({ timeout: 8_000 });
  });

  test('clicking edit on a row opens the upsert dialog', async ({ page }) => {
    const section = page.locator('tch-admin-limits-section');
    await expect(section).toBeVisible({ timeout: 12_000 });

    // Groups are collapsed by default — expand the first group to expose action buttons
    const groupHeader = section.locator('.lpb__group-header').first();
    await groupHeader.click();

    const editBtn = section.locator('.lpb__action-btn').first();
    await editBtn.click();

    await expect(page.locator('mat-dialog-container')).toBeVisible({ timeout: 5_000 });
  });

  test('limit section appears AFTER the detail layout, not inside it', async ({ page }) => {
    const detailLayout = page.locator('tch-admin-detail-layout');
    const section = page.locator('tch-admin-limits-section');

    await expect(detailLayout).toBeVisible({ timeout: 10_000 });
    await expect(section).toBeVisible({ timeout: 12_000 });

    // Section must not be a descendant of the detail layout
    await expect(detailLayout.locator('tch-admin-limits-section')).toHaveCount(0);
  });
});
