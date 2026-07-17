import { expect, type Locator, type Page } from '@playwright/test';

export class AdminLimitsPage {
  readonly activeTable: Locator;
  readonly add: Locator;
  readonly ruleSelect: Locator;
  readonly selectionInput: Locator;
  readonly selectionRequired: Locator;
  readonly save: Locator;

  constructor(private readonly page: Page) {
    this.activeTable = page.getByTestId('admin-limits-active-table');
    this.add = page.getByTestId('admin-limits-add');
    this.ruleSelect = page.getByTestId('limit-rule-select');
    this.selectionInput = page.getByTestId('limit-selection-input');
    this.selectionRequired = page.getByTestId('limit-selection-required');
    this.save = page.getByTestId('limit-save');
  }

  async gotoGlobal(): Promise<void> {
    await this.page.goto('/app/admin/limits/global');
    await expect(this.activeTable).toBeVisible();
  }

  async startAddingFirstAvailableRule(): Promise<void> {
    await this.add.click();
    await this.ruleSelect.click();
    await this.page.getByRole('option').first().click();
  }

  async expectNumberSelectionRequired(): Promise<void> {
    await expect(this.save).toBeDisabled();
    await expect(this.selectionRequired).toBeVisible();
  }

  async addBlockedSelection(selection: string): Promise<void> {
    await this.selectionInput.fill(selection);
    await this.page.keyboard.press('Enter');
  }
}
