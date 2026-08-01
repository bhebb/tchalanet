import { expect, test } from '../support/fixtures';
import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';

const creds = credsFor('admin');

test.describe('Admin seller-terminal form contract', () => {
  test.beforeEach(async ({ loginPage, apiStub }) => {
    test.skip(
      process.env['WEB_E2E_API'] === '1',
      'The form contract uses deterministic REST stubs until the Docker seed exposes stable setup data.',
    );
    if (!creds) {
      test.skip(true, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');
      return;
    }

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await apiStub.adminSellerTerminalNew();
    await loginPage.login(creds);
  });

  test('renders the shared form layout with field, preview and action regions', async ({
    adminSellerTerminalNewPage,
  }) => {
    await adminSellerTerminalNewPage.goto();

    await adminSellerTerminalNewPage.expectReadyStructure();
    await expect(adminSellerTerminalNewPage.aside).toBeVisible();
  });
});
