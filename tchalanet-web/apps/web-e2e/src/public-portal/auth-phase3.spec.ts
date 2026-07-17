import { expect, test } from '../support/fixtures';
import { credsFor } from '../support/env';

test.describe('Phase 3 — public auth handoff', () => {
  test('public login handoff lands on the platform shell without a login loop', async ({
    page,
    loginPage,
    privateShell,
    apiStub,
  }) => {
    const creds = credsFor('superAdmin');
    test.skip(!creds, 'TCH_E2E_SUPERADMIN_EMAIL/PASSWORD not configured');

    await apiStub.portalHandoff('PLATFORM', 'http://localhost:4303', '/app/platform');
    await loginPage.login(creds!);

    await expect(page).toHaveURL(/localhost:4303\/app\/platform\b/);
    await expect(loginPage.root).toHaveCount(0);
    await expect(privateShell.userMenuTrigger).toBeVisible();
  });
});
