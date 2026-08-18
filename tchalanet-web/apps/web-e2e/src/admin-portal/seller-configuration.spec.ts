import { expect, test } from '../support/fixtures';
import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';

const creds = credsFor('admin');

test.describe('Admin seller configuration — pricing overrides', () => {
  test.beforeEach(async ({ loginPage, apiStub }) => {
    test.skip(
      process.env['WEB_E2E_API'] === '1',
      'Seller configuration UI contract uses deterministic REST stubs.',
    );
    test.skip(!creds, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await apiStub.adminSellerConfiguration();
    await loginPage.login(creds!);
  });

  test('opens barèmes from a seller card, persists an override, and restores tenant inheritance', async ({
    page,
  }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto('/app/admin/seller-terminals/commissions');

    const sellerCard = page.locator('.commission-table__mobile-card').first();
    await expect(sellerCard).toBeVisible({ timeout: 10_000 });
    await expect(sellerCard).toContainText('Bhebbb');
    await sellerCard
      .locator('a[href="/app/admin/seller-terminals/stub-terminal-1/overrides"]')
      .click();

    await expect(page).toHaveURL(/\/app\/admin\/seller-terminals\/stub-terminal-1\/overrides$/);

    const row = page.locator('.seller-overrides__row').first();
    await expect(row).toBeVisible({ timeout: 10_000 });
    const input = row.locator('input[type="number"]');
    await expect(input).toHaveValue('10');

    await input.fill('12');
    const saveResponse = page.waitForResponse(
      response =>
        response.url().includes('/admin/controls/pricing-rules/seller-terminals/stub-terminal-1') &&
        response.request().method() === 'PUT',
    );
    await row.getByRole('button', { name: /enregistrer|anrejistre|save/i }).click();

    const response = await saveResponse;
    expect(response.status()).toBe(200);
    expect(JSON.parse(response.request().postData() ?? '{}')).toMatchObject({
      gameCode: 'BOLET',
      betType: 'STRAIGHT',
      odds: 12,
    });
    await expect(input).toHaveValue('12');
    await expect(row.getByText(/Règle spéciale active|Règ espesyal aktif|Special rule active/i)).toBeVisible();

    const deleteResponse = page.waitForResponse(
      response =>
        response
          .url()
          .includes('/admin/controls/pricing-rules/seller-terminals/stub-terminal-1/overrides/') &&
        response.request().method() === 'DELETE',
    );
    await row.getByRole('button', { name: /revenir.*espace|retounen sou|revert to client/i }).click();
    expect((await deleteResponse).status()).toBe(200);
    await expect(row.getByText(/Hérite de l'espace|Swiv règ tenansye|Inherits client workspace/i)).toBeVisible();
  });
});
