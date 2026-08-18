import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';
import { expect, test } from '../support/fixtures';

const creds = credsFor('admin');

test.describe('Admin params overview — navigation contract', () => {
  test.beforeEach(async ({ loginPage, apiStub }) => {
    test.skip(!creds, 'requires TCH_E2E_ADMIN_EMAIL / TCH_E2E_ADMIN_PASSWORD');
    test.skip(
      process.env['WEB_E2E_API'] === '1',
      'Params-overview navigation uses deterministic REST stubs.',
    );

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await apiStub.adminParamsOverview();
    await loginPage.login(creds!);
  });

  test('renders the three section cards', async ({ page }) => {
    await page.goto('/app/admin/company/settings');

    const cards = page.locator('tch-settings-summary-card');
    await expect(cards).toHaveCount(3);
  });

  test('each section card has a Modifye link to the correct sub-page', async ({ page }) => {
    await page.goto('/app/admin/company/settings');

    const cards = page.locator('tch-settings-summary-card');
    await expect(cards).toHaveCount(3);

    const receiptCard = cards.nth(0);
    const deliveryCard = cards.nth(1);
    const calendarCard = cards.nth(2);

    const editLabel = /Modifye|Edit|Modifier/;

    await expect(receiptCard.getByRole('link', { name: editLabel })).toHaveAttribute(
      'href',
      /\/settings\/receipt/,
    );
    await expect(deliveryCard.getByRole('link', { name: editLabel })).toHaveAttribute(
      'href',
      /\/settings\/delivery/,
    );
    await expect(calendarCard.getByRole('link', { name: editLabel })).toHaveAttribute(
      'href',
      /\/settings\/calendar/,
    );
  });
});
