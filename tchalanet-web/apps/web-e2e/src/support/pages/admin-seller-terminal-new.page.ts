import { expect, type Locator, type Page } from '@playwright/test';

export class AdminSellerTerminalNewPage {
  readonly root: Locator;
  readonly formLayout: Locator;
  readonly main: Locator;
  readonly aside: Locator;
  readonly footer: Locator;

  constructor(private readonly page: Page) {
    this.root = page.getByTestId('admin-page-shell');
    this.formLayout = page.getByTestId('admin-form-layout');
    this.main = page.getByTestId('admin-form-main');
    this.aside = page.getByTestId('admin-form-aside');
    this.footer = page.getByTestId('admin-form-footer');
  }

  async goto(): Promise<void> {
    await this.page.goto('/app/admin/seller-terminals/new');
    await expect(this.root).toBeVisible();
  }

  async expectReadyStructure(): Promise<void> {
    await expect(this.root).toHaveCount(1);
    await expect(this.formLayout).toHaveCount(1);
    await expect(this.main).toHaveCount(1);
    await expect(this.aside).toHaveCount(1);
    await expect(this.footer).toHaveCount(1);
    await expect(this.main.getByTestId('admin-detail-section')).toHaveCount(6);
  }
}
