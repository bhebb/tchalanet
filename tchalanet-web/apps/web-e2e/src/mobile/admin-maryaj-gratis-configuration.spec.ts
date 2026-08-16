import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';
import { expect, test } from '../support/fixtures';

const creds = credsFor('admin');

test.describe('Admin Maryaj Gratis configuration — mobile', () => {
  test.beforeEach(async ({ loginPage, apiStub }) => {
    const adminCreds = creds;
    test.skip(!adminCreds, 'requires TCH_E2E_ADMIN_EMAIL / TCH_E2E_ADMIN_PASSWORD');
    test.skip(
      process.env['WEB_E2E_API'] === '1',
      'Maryaj Gratis UX contract uses deterministic REST stubs.',
    );
    if (!adminCreds) return;

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await apiStub.adminBusiness();
    await loginPage.login(adminCreds);
  });

  test('keeps offer editing actions reachable at phone width', async ({
    adminMaryajGratisPage,
    page,
  }) => {
    await page.setViewportSize({ width: 360, height: 740 });
    await adminMaryajGratisPage.goto();

    await adminMaryajGratisPage.editOffer.click();
    await expect(adminMaryajGratisPage.offerPanel).toContainText(/Atribisyon|Attribution/);

    const lastTierQuantity = adminMaryajGratisPage.offerPanel
      .locator('input[formcontrolname="quantity"]')
      .last();
    await lastTierQuantity.scrollIntoViewIfNeeded();
    await expect(lastTierQuantity).toBeVisible();
    await lastTierQuantity.focus();
    await expect(lastTierQuantity).toBeFocused();

    await adminMaryajGratisPage.save.scrollIntoViewIfNeeded();
    await expect(adminMaryajGratisPage.save).toBeVisible();
    const saveBox = await adminMaryajGratisPage.save.boundingBox();
    expect(saveBox).not.toBeNull();
    expect(saveBox?.height ?? 0).toBeGreaterThanOrEqual(40);

    const cancelButton = adminMaryajGratisPage.offerPanel.getByRole('button', {
      name: /Annuler|Anile|Cancel/,
    });
    await expect(cancelButton).toBeVisible();
    const cancelBox = await cancelButton.boundingBox();
    expect(cancelBox).not.toBeNull();
    expect(cancelBox?.height ?? 0).toBeGreaterThanOrEqual(40);

    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    expect(overflow).toBeLessThanOrEqual(0);
  });
});
