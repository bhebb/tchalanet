import { expect, test } from '../support/fixtures';
import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';

const creds = credsFor('admin');
const backendDiagnostic = 'backend diagnostic must not reach the UI';

test.describe('Admin business profile — local feedback contracts', () => {
  test.beforeEach(async ({ loginPage, apiStub }) => {
    test.skip(
      process.env['WEB_E2E_API'] === '1',
      'Business-profile UI contract tests use deterministic REST stubs.',
    );
    test.skip(!creds, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await apiStub.adminBusinessProfile();
    await loginPage.login(creds!);
  });

  test('commission validation stays on the field and preserves the form', async ({
    page,
    apiStub,
  }) => {
    await apiStub.adminBusinessProfileCommissionFieldError();
    await page.goto('/app/admin/business-profile');

    const commercial = page.locator('tch-admin-section-card').nth(4);
    await commercial.locator('button').first().click();
    await commercial.getByRole('spinbutton').fill('12');
    const updateResponse = page.waitForResponse(
      response =>
        response.url().includes('/admin/commission/default-rate') &&
        response.request().method() === 'PUT',
    );
    await commercial.locator('button').last().click();
    expect((await updateResponse).status()).toBe(422);

    await expect(commercial.locator('tch-field-error').getByRole('alert')).toBeVisible();
    await expect(commercial).not.toContainText(backendDiagnostic);
    await expect(commercial.getByRole('spinbutton')).toHaveValue('12');
    await expect(page.locator('tch-shell-feedback-banner')).toHaveCount(0);
  });

  test('address mutation failure stays in the address block', async ({ page, apiStub }) => {
    await apiStub.adminBusinessProfileAddressError();
    await page.goto('/app/admin/business-profile');

    const address = page.locator('tch-admin-section-card').nth(2);
    await address.locator('button').first().click();
    await address.locator('button').last().click();

    await expect(address.locator('tch-notice .notice[data-type="error"]')).toBeVisible();
    await expect(address).not.toContainText(backendDiagnostic);
    await expect(address.getByRole('button').last()).toBeVisible();
    await expect(page.locator('tch-shell-feedback-banner')).toHaveCount(0);
  });

  test('successful commission and address saves render local success notices', async ({ page }) => {
    await page.goto('/app/admin/business-profile');

    const commercial = page.locator('tch-admin-section-card').nth(4);
    await commercial.locator('button').first().click();
    await commercial.getByRole('spinbutton').fill('12');
    await commercial.locator('button').last().click();
    const commissionSuccessHost = commercial.locator('tch-notice.biz-profile__notice');
    const commissionSuccess = commissionSuccessHost.locator('.notice[data-type="success"]');
    await expect(commissionSuccess).toBeVisible();
    await expect(commissionSuccess).toContainText('Paramèt komèsyal yo anrejistre avèk siksè.');
    await expect(commissionSuccess).toBeInViewport();
    await expect(commissionSuccess).toBeFocused();

    const address = page.locator('tch-admin-section-card').nth(2);
    await address.locator('button').first().click();
    await address.locator('button').last().click();
    const addressSuccessHost = address.locator('tch-notice.biz-profile__notice');
    const addressSuccess = addressSuccessHost.locator('.notice[data-type="success"]');
    await expect(addressSuccess).toBeVisible();
    await expect(addressSuccess).toBeInViewport();
    await expect(addressSuccess).toBeFocused();
  });
});

test.describe('Admin setup — business notices', () => {
  test.beforeEach(async ({ loginPage, apiStub }) => {
    test.skip(
      process.env['WEB_E2E_API'] === '1',
      'Business-notice UI contract tests use deterministic REST stubs.',
    );
    test.skip(!creds, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');

    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await apiStub.adminSetupBusinessNotices();
    await loginPage.login(creds!);
  });

  test('renders targeted info and warning notices on their setup cards', async ({ page }) => {
    await page.goto('/app/admin/setup');

    await expect(
      page.locator('tch-section-error .tch-section-error[data-severity="info"]'),
    ).toBeVisible();
    await expect(
      page.locator('tch-section-error .tch-section-error[data-severity="warn"]'),
    ).toBeVisible();
    await expect(page.getByTestId('admin-setup-checklist')).toBeVisible();
    await expect(
      page.locator('tch-section-error').filter({ hasText: backendDiagnostic }),
    ).toHaveCount(0);
  });
});
