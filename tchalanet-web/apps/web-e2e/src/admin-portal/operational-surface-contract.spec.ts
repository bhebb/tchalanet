import { expect, test } from '../support/fixtures';
import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';

test.describe('Admin operational surface structure', () => {
  test.beforeEach(async ({ loginPage, apiStub }) => {
    test.skip(
      process.env['WEB_E2E_API'] === '1',
      'The structural contract uses deterministic REST stubs until the Docker seed covers these screens.',
    );
    const creds = credsFor('admin');
    if (!creds) {
      test.skip(true, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');
      return;
    }

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await loginPage.login(creds);
  });

  for (const screen of [
    {
      path: '/app/admin/seller-terminals',
      blockIds: [
        'admin-page-shell',
        'admin-page-header',
        'admin-page-actions',
        'admin-refresh-button',
        'admin-list-surface',
        'admin-list-toolbar',
        'admin-list-content',
      ],
    },
    {
      path: '/app/admin/reports/sellers',
      blockIds: [
        'admin-page-shell',
        'admin-page-header',
        'admin-page-actions',
        'admin-refresh-button',
      ],
    },
  ]) {
    test(`keeps stable block ids on ${screen.path}`, async ({ page }) => {
      await page.goto(screen.path);

      for (const blockId of screen.blockIds) {
        const block = page.getByTestId(blockId);
        await expect(block, `${blockId} must stay unique on ${screen.path}`).toHaveCount(1, {
          // The list surface is rendered only once its resource settles. Give
          // this explicit async boundary enough time on a cold CI runner.
          timeout: blockId.startsWith('admin-list-') ? 15_000 : undefined,
        });
      }
    });
  }
});
