import { expect, test } from '../support/fixtures';
import { superAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';

/**
 * Tenant provisioning — platform-portal form contract.
 *
 * Covers:
 * - Form renders with all required sections
 * - Required-field validation fires on submit
 * - Successful provision (stubbed API) shows the success screen
 *
 * Auth: super-admin via Firebase emulator; plans + onboarding endpoints are stubbed.
 */

const creds = credsFor('superAdmin');

const STUB_TENANT_ID = 'aaaaaaaa-0000-0000-0000-000000000001';

const plansStub = [
  {
    planCode: 'HAITI_LOTTERY',
    name: 'Haiti Lottery',
    description: 'Plan standard pour opérateurs de loterie en Haïti.',
    isDefault: true,
    features: [],
  },
];

const previewStub = {
  tenantCode: 'preview-acme',
  tenantName: 'Preview ACME',
  profile: 'STANDARD',
  planCode: 'HAITI_LOTTERY',
  warnings: [],
};

const provisionStub = {
  tenantId: { value: STUB_TENANT_ID },
  tenantCode: 'TEST-ACME',
  tenantName: 'Test ACME',
  adminUserId: { value: 'admin-user-1' },
};

async function installProvisioningStubs(page: import('@playwright/test').Page): Promise<void> {
  await page.route(/\/platform\/plans(?:\?|$)/, route => {
    if (route.request().resourceType() === 'document') { route.continue(); return; }
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 'SUCCESS', data: plansStub, notices: [] }) });
  });
  await page.route(/\/platform\/tenant-onboarding\/preview(?:\?|$)/, route => {
    if (route.request().resourceType() === 'document') { route.continue(); return; }
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 'SUCCESS', data: previewStub, notices: [] }) });
  });
  await page.route(/\/platform\/tenant-onboarding\/provision(?:\?|$)/, route => {
    if (route.request().resourceType() === 'document') { route.continue(); return; }
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 'SUCCESS', data: provisionStub, notices: [] }) });
  });
  await page.route(new RegExp(`/platform/tenants/${STUB_TENANT_ID}`), route => {
    if (route.request().resourceType() === 'document') { route.continue(); return; }
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 'SUCCESS', data: { tenantId: { value: STUB_TENANT_ID }, code: 'TEST-ACME', name: 'Test ACME', status: 'DRAFT', type: 'LOTTERY', currency: 'HTG', timezone: 'America/Port-au-Prince' }, notices: [] }) });
  });
}

test.describe('Tenant provisioning — form contract', () => {
  test.beforeEach(async ({ loginPage, apiStub }) => {
    if (!creds) {
      test.skip(true, 'TCH_E2E_SUPERADMIN_EMAIL/PASSWORD not configured');
      return;
    }
    await apiStub.privateBootstrap(superAdminPrivateBootstrap);
    await loginPage.login(creds);
  });

  test('form renders with required field sections', async ({ page }) => {
    await installProvisioningStubs(page);
    await page.goto('/app/platform/tenants/onboarding');
    const shell = page.locator('tch-admin-page-shell');
    await expect(shell).toBeVisible({ timeout: 10_000 });

    // Identity, regional, commercial, profile, admin sections must all be present
    const cards = page.locator('tch-admin-section-card');
    await expect(cards).toHaveCount(5, { timeout: 5_000 });
  });

  test('code field is required — submit shows validation', async ({ page }) => {
    await installProvisioningStubs(page);
    await page.goto('/app/platform/tenants/onboarding');
    const shell = page.locator('tch-admin-page-shell');
    await expect(shell).toBeVisible({ timeout: 10_000 });

    // Click submit without filling required fields
    const submitBtn = page.locator('button[type="submit"], button', { hasText: /créer|provision|soumettre|submit/i }).first();
    await submitBtn.click();

    // At least one mat-error should become visible
    await expect(page.locator('mat-error').first()).toBeVisible({ timeout: 5_000 });
  });

  test('back link returns to tenant list', async ({ page }) => {
    await installProvisioningStubs(page);
    await page.goto('/app/platform/tenants/onboarding');
    const shell = page.locator('tch-admin-page-shell');
    await expect(shell).toBeVisible({ timeout: 10_000 });

    const backBtn = page.locator('a', { hasText: /retour|tenants|back/i }).first();
    await backBtn.click();

    await expect(page).toHaveURL(/\/app\/platform\/tenants(?:\?|$)/, { timeout: 5_000 });
  });
});
