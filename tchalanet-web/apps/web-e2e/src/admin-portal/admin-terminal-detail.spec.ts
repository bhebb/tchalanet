import { expect, test } from '../support/fixtures';
import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';

const creds = credsFor('admin');

test.describe('Admin seller-terminal detail contract', () => {
  test.beforeEach(async ({ loginPage, apiStub }) => {
    test.skip(
      process.env['WEB_E2E_API'] === '1',
      'The detail-page contract uses deterministic REST stubs until the Docker seed exposes a stable terminal fixture.',
    );
    if (!creds) {
      test.skip(true, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');
      return;
    }

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await apiStub.adminSellerTerminalDetail();
    await loginPage.login(creds);
  });

  test('renders the shared detail structure with main and aside regions', async ({
    adminSellerTerminalDetailPage,
  }) => {
    await adminSellerTerminalDetailPage.goto();

    await adminSellerTerminalDetailPage.expectReadyStructure();
    await expect(adminSellerTerminalDetailPage.root).toBeVisible();
  });

  test('keeps the shared detail error contract for a terminal load failure', async ({
    adminSellerTerminalDetailPage,
    apiStub,
  }) => {
    await apiStub.adminSellerTerminalDetailError();
    await adminSellerTerminalDetailPage.goto();

    await adminSellerTerminalDetailPage.expectBlockingError();
    await expect(adminSellerTerminalDetailPage.main).toHaveCount(0);
    await expect(adminSellerTerminalDetailPage.aside).toHaveCount(0);
  });
});
