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
  readonly accessSubmit: Locator;
  readonly accessReason: Locator;
  readonly accessConfirm: Locator;

  constructor(private readonly page: Page) {
    this.root = page.locator('tch-platform-support-tenant-page');
    this.search = page.getByTestId('support-tenant-search');
    this.openAccessButtons = page.getByTestId('support-tenant-open-access');
    this.emptyState = page.locator('tch-admin-empty-state');
    this.accessDialog = page.locator('tch-start-tenant-admin-access-dialog');
    this.accessSubmit = page.getByTestId('start-access-submit');
    this.accessReason = page.getByTestId('start-access-reason');
    this.accessConfirm = page.getByTestId('start-access-confirm');
  }

  async goto(): Promise<void> {
    await this.page.goto('/app/platform/support-tenant');
    await expect(this.root).toBeVisible();
  }

  async openAccessForFirstTenant(): Promise<void> {
    await this.openAccessButtons.first().click();
    await expect(this.accessDialog).toBeVisible();
  }

  /** Fill the reason + confirmation and submit the start-access dialog. */
  async confirmAccess(reason = 'E2E support access verification'): Promise<void> {
    await this.accessReason.fill(reason);
    await this.accessConfirm.click();
    await this.accessSubmit.click();
  }
}
