import { expect, type Locator, type Page } from '@playwright/test';

export class AdminSellerTerminalDetailPage {
  readonly root: Locator;
  readonly main: Locator;
  readonly aside: Locator;
  readonly identity: Locator;
  readonly alert: Locator;

  constructor(private readonly page: Page) {
    this.root = page.getByTestId('admin-page-shell');
    this.main = page.getByTestId('admin-detail-main');
    this.aside = page.getByTestId('admin-detail-aside');
    this.identity = page.getByTestId('admin-detail-identity');
    this.alert = page.getByRole('alert');
  }

  async goto(terminalId = 'stub-terminal-1'): Promise<void> {
    await this.page.goto(`/app/admin/seller-terminals/${terminalId}`);
    await expect(this.root).toBeVisible();
  }

  async expectReadyStructure(): Promise<void> {
    await expect(this.root).toHaveCount(1);
    await expect(this.page.getByTestId('admin-page-header')).toHaveCount(1);
    await expect(this.page.getByTestId('admin-page-actions')).toHaveCount(1);
    await expect(this.page.getByTestId('admin-page-body')).toHaveCount(1);
    await expect(this.page.getByTestId('admin-detail-layout')).toHaveCount(1);
    await expect(this.main).toHaveCount(1);
    await expect(this.aside).toHaveCount(1);
    await expect(this.identity).toHaveCount(1);
    await expect(this.identity).toBeVisible();
    await expect(this.main.getByTestId('admin-detail-section')).toHaveCount(4);
    await expect(this.aside.getByTestId('admin-detail-section')).toHaveCount(2);
    await expect(this.page.getByTestId('admin-page-actions').getByRole('link')).toHaveCount(4);
  }

  async expectBlockingError(): Promise<void> {
    await expect(this.alert).toBeVisible();
    await expect(this.alert.locator('h2')).toBeVisible();
    await expect(this.alert).not.toContainText(
      'backend failure: diagnostic text must not reach the UI',
    );
    await expect(this.alert.getByRole('button')).toHaveCount(1);
  }
}
