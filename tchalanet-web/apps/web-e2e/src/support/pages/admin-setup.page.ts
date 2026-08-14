import { expect, type Locator, type Page } from '@playwright/test';

export class AdminSetupPage {
  readonly checklist: Locator;
  readonly operationalChecklist: Locator;

  constructor(private readonly page: Page) {
    this.checklist = page.getByTestId('admin-setup-checklist');
    this.operationalChecklist = page.getByTestId('admin-setup-operational-checklist');
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
    await expect(this.operationalChecklist).toBeVisible();
    await expect(this.page.getByTestId('admin-setup-card-pos_printing')).toBeVisible();
  }

  async expectReadySetupWithOperationalPrintingIssue(): Promise<void> {
    await expect(this.page.locator('.setup__progress-pct')).toHaveText('100%');
    await expect(this.page.getByTestId('admin-setup-card-settings').locator('.setup__card--ready')).toBeVisible();
    const posPrintingCard = this.page.getByTestId('admin-setup-card-pos_printing');
    await expect(posPrintingCard).toBeVisible();
    await expect(posPrintingCard.locator('.setup__card--missing')).toBeVisible();
    await expect(this.page.locator('.setup__card--terminal.setup__card--locked')).toHaveCount(0);
  }
}
