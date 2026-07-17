import { expect, type Locator, type Page } from '@playwright/test';

/**
 * Page Object for the platform "support tenant" screen
 * (`tch-platform-support-tenant-page`, `/app/platform/support-tenant`): the
 * super-admin support mode — a tenant table whose row action opens the
 * "start tenant admin access" dialog to operate a tenant in support context.
 */
export class SupportTenantPage {
  readonly root: Locator;
  readonly search: Locator;
  readonly openAccessButtons: Locator;
  readonly emptyState: Locator;
  readonly accessDialog: Locator;
  readonly accessReason: Locator;
  readonly accessConfirmed: Locator;
  readonly accessSubmit: Locator;

  constructor(private readonly page: Page) {
    this.root = page.locator('tch-platform-support-tenant-page');
    this.search = page.getByTestId('support-tenant-search');
    this.openAccessButtons = page.getByTestId('support-tenant-open-access');
    this.emptyState = page.locator('tch-admin-empty-state');
    this.accessDialog = page.locator('tch-start-tenant-admin-access-dialog');
    this.accessReason = page.getByTestId('start-access-reason');
    this.accessConfirmed = page.getByTestId('start-access-confirmed');
    this.accessSubmit = page.getByTestId('start-access-submit');
  }

  async goto(): Promise<void> {
    await this.page.goto('/app/platform/support-tenant');
    await expect(this.root).toBeVisible();
  }

  async openAccessForFirstTenant(): Promise<void> {
    await this.openAccessButtons.first().click();
    await expect(this.accessDialog).toBeVisible();
  }

  async confirmAccess(reason = 'E2E support access validation'): Promise<void> {
    await this.accessReason.fill(reason);
    await this.accessConfirmed.click();
    await this.accessSubmit.click();
  }
}
