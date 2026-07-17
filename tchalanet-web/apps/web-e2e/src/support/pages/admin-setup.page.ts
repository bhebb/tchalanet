import { expect, type Locator, type Page } from '@playwright/test';

export class AdminSetupPage {
  readonly checklist: Locator;

  constructor(private readonly page: Page) {
    this.checklist = page.getByTestId('admin-setup-checklist');
  }

  async goto(): Promise<void> {
    await this.page.goto('/app/admin/setup');
    await expect(this.checklist).toBeVisible();
  }

  async expectReadinessCards(): Promise<void> {
    await expect(this.page.getByTestId('admin-setup-card-identity_address')).toBeVisible();
    await expect(this.page.getByTestId('admin-setup-card-games_pricing')).toBeVisible();
    await expect(this.page.getByTestId('admin-setup-card-draws')).toBeVisible();
    await expect(this.page.getByTestId('admin-setup-card-generated_draws')).toBeVisible();
  }
}
