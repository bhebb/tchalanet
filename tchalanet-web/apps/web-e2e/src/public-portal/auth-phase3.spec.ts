import { expect, test } from '../support/fixtures';
import { credsFor } from '../support/env';

/**
 * Phase 3 — cross-app handoff from the public portal (public-portal, :4301).
 */
test.describe('Phase 3 — public handoff', () => {
  // Logging in from the public portal dispatches cross-app (location.assign) to
  // the role's portal on another origin. Enabling this needs the served ports to
  // match `portalBaseUrls` (env.emulator currently points platform at :4202, not
  // the :4303 served here) and cross-origin navigation to be stable. Confirm the
  // wiring before asserting.
  test.fixme('login from public dispatches to the role portal', async ({
    page,
    loginPage,
    apiStub,
  }) => {
    const creds = credsFor('admin');
    test.skip(!creds, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');

    void apiStub;
    await loginPage.login(creds!);
    await expect(page).toHaveURL(/\/(admin|app\/admin)\b/);
  });
});
