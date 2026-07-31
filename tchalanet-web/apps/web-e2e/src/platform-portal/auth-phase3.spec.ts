import { expect, test } from '../support/fixtures';
import { supportTenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';

const SUPPORT_FLOW_TIMEOUT = 20_000;

test.describe('Phase 3 — platform auth completion', () => {
  test.describe.configure({ timeout: 45_000 });

  test('super admin can logout from an authenticated shell', async ({
    page,
    loginPage,
    privateShell,
    apiStub,
  }) => {
    const creds = credsFor('superAdmin');
    test.skip(!creds, 'TCH_E2E_SUPERADMIN_EMAIL/PASSWORD not configured');

    void apiStub;
    await loginPage.login(creds!);
    await expect(page).toHaveURL(/\/app\/platform\b/);

    await privateShell.logoutFromShell(SUPPORT_FLOW_TIMEOUT);
    await expect(loginPage.root).toBeVisible();
  });

  test('support tenant round-trip opens admin support mode then returns platform', async ({
    page,
    loginPage,
    privateShell,
    supportTenantPage,
    apiStub,
  }) => {
    const creds = credsFor('superAdmin');
    test.skip(!creds, 'TCH_E2E_SUPERADMIN_EMAIL/PASSWORD not configured');

    await apiStub.portalHandoff('ADMIN', 'http://localhost:4302', '/app/admin');
    await apiStub.tenants([
      { tenantId: 'stub-tenant-1', code: 'ACME', name: 'Acme Lottery', status: 'ACTIVE' },
    ]);

    await loginPage.login(creds!);
    await supportTenantPage.goto();
    await supportTenantPage.openAccessForFirstTenant();

    await apiStub.privateBootstrap(supportTenantAdminPrivateBootstrap);
    await supportTenantPage.confirmAccess();

    await expect(page).toHaveURL(/localhost:4302\/app\/admin\b/, {
      timeout: SUPPORT_FLOW_TIMEOUT,
    });
    await expect(privateShell.supportBanner).toBeVisible({ timeout: SUPPORT_FLOW_TIMEOUT });
    await expect(privateShell.supportTenantName).toContainText(/\S/, {
      timeout: SUPPORT_FLOW_TIMEOUT,
    });

    await privateShell.supportReturn.click();
    await expect(page).toHaveURL(/localhost:4303\/app\/platform\b/, {
      timeout: SUPPORT_FLOW_TIMEOUT,
    });
    await expect(privateShell.supportBanner).toHaveCount(0, { timeout: SUPPORT_FLOW_TIMEOUT });
  });

  test('logging out while supporting a tenant also clears the platform session', async ({
    page,
    loginPage,
    privateShell,
    supportTenantPage,
    apiStub,
  }) => {
    const creds = credsFor('superAdmin');
    test.skip(!creds, 'TCH_E2E_SUPERADMIN_EMAIL/PASSWORD not configured');

    await apiStub.portalHandoff('ADMIN', 'http://localhost:4302', '/app/admin');
    await apiStub.tenants([
      { tenantId: 'stub-tenant-1', code: 'ACME', name: 'Acme Lottery', status: 'ACTIVE' },
    ]);

    await loginPage.login(creds!);
    await supportTenantPage.goto();
    await supportTenantPage.openAccessForFirstTenant();
    await apiStub.privateBootstrap(supportTenantAdminPrivateBootstrap);
    await supportTenantPage.confirmAccess();
    await expect(privateShell.supportBanner).toBeVisible({ timeout: SUPPORT_FLOW_TIMEOUT });

    await privateShell.logoutFromShell(SUPPORT_FLOW_TIMEOUT);
    await expect(page).toHaveURL('http://localhost:4302/login', { timeout: SUPPORT_FLOW_TIMEOUT });
    await expect(loginPage.identifier).toBeVisible({ timeout: 15_000 });

    await page.goto('http://localhost:4303/app/platform');
    await expect(page).toHaveURL(/localhost:4303\/login\b/);
  });
});
