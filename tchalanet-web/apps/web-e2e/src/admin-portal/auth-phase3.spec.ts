import { expect, test } from '../support/fixtures';
import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';

test.describe('Phase 3 — admin auth completion', () => {
  test('admin login dispatches with email', async ({
    page,
    loginPage,
    apiStub,
  }) => {
    const creds = credsFor('admin');
    test.skip(!creds, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await loginPage.login(creds!);
    await expect(page).toHaveURL(/\/(app\/admin|account\/activation)\b/);
  });

  test('admin login dispatches with username lookup', async ({
    page,
    loginPage,
    apiStub,
  }) => {
    const creds = credsFor('admin');
    test.skip(!creds, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await apiStub.loginIdentifier('admin', creds!.email);
    await loginPage.login({ email: 'admin', password: creds!.password });
    await expect(page).toHaveURL(/\/(app\/admin|account\/activation)\b/);
  });
});
