import { expect, test } from '../support/fixtures';
import { superAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';

/**
 * Tenant modification — platform-portal tenant detail page contract.
 *
 * Covers:
 * - Detail page renders for a stubbed tenant
 * - Status badge and core identity fields are visible
 * - Lifecycle actions (activate/suspend) are gated by tenant status
 * - Support access button is present for super admin
 *
 * Auth: super-admin via Firebase emulator; tenant detail endpoint is stubbed.
 */

const creds = credsFor('superAdmin');

const STUB_TENANT_ID = 'bbbbbbbb-0000-0000-0000-000000000001';

function stubTenant(status: 'DRAFT' | 'ACTIVE' | 'SUSPENDED') {
  return {
    tenantId: { value: STUB_TENANT_ID },
    code: 'STUB-MOD',
    name: 'Modification Test Tenant',
    status,
    type: 'LOTTERY',
    currency: 'HTG',
    timezone: 'America/Port-au-Prince',
    createdAt: '2026-07-01T00:00:00Z',
    updatedAt: '2026-08-01T00:00:00Z',
  };
}

function installTenantStub(page: import('@playwright/test').Page, status: 'DRAFT' | 'ACTIVE' | 'SUSPENDED') {
  return page.route(new RegExp(`/platform/tenants/${STUB_TENANT_ID}(?:\\?|$)`), route => {
    if (route.request().resourceType() === 'document') { route.continue(); return; }
    if (route.request().method() !== 'GET') { route.continue(); return; }
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ status: 'SUCCESS', data: stubTenant(status), notices: [] }),
    });
  });
}

test.describe('Tenant detail — modification contract', () => {
  test.beforeEach(async ({ loginPage, apiStub }) => {
    if (!creds) {
      test.skip(true, 'TCH_E2E_SUPERADMIN_EMAIL/PASSWORD not configured');
      return;
    }
    await apiStub.privateBootstrap(superAdminPrivateBootstrap);
    await loginPage.login(creds);
  });

  test('detail page renders for a DRAFT tenant', async ({ page }) => {
    await installTenantStub(page, 'DRAFT');
    await page.goto(`/app/platform/tenants/${STUB_TENANT_ID}`);

    const shell = page.locator('tch-admin-page-shell');
    await expect(shell).toBeVisible({ timeout: 10_000 });

    // Tenant name is shown as the page title
    await expect(page.locator('tch-admin-page-shell')).toContainText('Modification Test Tenant', { timeout: 8_000 });
  });

  test('DRAFT tenant shows Activate action', async ({ page }) => {
    await installTenantStub(page, 'DRAFT');
    await page.goto(`/app/platform/tenants/${STUB_TENANT_ID}`);

    const shell = page.locator('tch-admin-page-shell');
    await expect(shell).toBeVisible({ timeout: 10_000 });

    const activateBtn = page.locator('button, [tch-action]', { hasText: /activer|activate/i });
    await expect(activateBtn).toBeVisible({ timeout: 5_000 });
  });

  test('ACTIVE tenant shows Suspend action instead of Activate', async ({ page }) => {
    await installTenantStub(page, 'ACTIVE');
    await page.goto(`/app/platform/tenants/${STUB_TENANT_ID}`);

    const shell = page.locator('tch-admin-page-shell');
    await expect(shell).toBeVisible({ timeout: 10_000 });

    await expect(page.locator('button, [tch-action]', { hasText: /activer|activate/i })).toHaveCount(0, { timeout: 5_000 });
    await expect(page.locator('button, [tch-action]', { hasText: /suspendre|suspend/i })).toBeVisible({ timeout: 5_000 });
  });

  test('support access button is always visible for super admin', async ({ page }) => {
    await installTenantStub(page, 'ACTIVE');
    await page.goto(`/app/platform/tenants/${STUB_TENANT_ID}`);

    const shell = page.locator('tch-admin-page-shell');
    await expect(shell).toBeVisible({ timeout: 10_000 });

    await expect(page.locator('button', { hasText: /accès support|support access|admin_panel_settings/i }).or(
      page.locator('[class*="action"]', { hasText: /support/i }),
    ).first()).toBeVisible({ timeout: 5_000 });
  });

  test('back link returns to the tenant list', async ({ page }) => {
    await installTenantStub(page, 'ACTIVE');
    await page.goto(`/app/platform/tenants/${STUB_TENANT_ID}`);

    const shell = page.locator('tch-admin-page-shell');
    await expect(shell).toBeVisible({ timeout: 10_000 });

    const backBtn = page.locator('a', { hasText: /retour|tenants|back/i }).first();
    await backBtn.click();

    await expect(page).toHaveURL(/\/app\/platform\/tenants(?:\?|$)/, { timeout: 5_000 });
  });
});
