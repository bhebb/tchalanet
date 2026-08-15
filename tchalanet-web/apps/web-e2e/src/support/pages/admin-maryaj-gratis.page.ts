import { expect, type Locator, type Page } from '@playwright/test';

export class AdminMaryajGratisPage {
  readonly root: Locator;
  readonly gamePanel: Locator;
  readonly offerPanel: Locator;
  readonly generationPanel: Locator;
  readonly editOffer: Locator;
  readonly sellerSelectsToggle: Locator;
  readonly save: Locator;

  constructor(private readonly page: Page) {
    this.root = page.getByTestId('admin-maryaj-gratis-page');
    this.gamePanel = page.getByTestId('maryaj-gratis-game-panel');
    this.offerPanel = page.getByTestId('maryaj-gratis-offer-panel');
    this.generationPanel = page.getByTestId('maryaj-gratis-generation-panel');
    this.editOffer = page.getByTestId('maryaj-gratis-edit-offer');
    this.sellerSelectsToggle = page.getByTestId('maryaj-gratis-seller-selects-toggle');
    this.save = page.getByTestId('maryaj-gratis-save');
  }

  async goto(): Promise<void> {
    await this.page.goto('/app/admin/maryaj-gratis');
    await expect(this.root).toBeVisible();
  }

  async expectPanels(): Promise<void> {
    await expect(this.gamePanel).toBeVisible();
    await expect(this.offerPanel).toBeVisible();
    await expect(this.generationPanel).toBeVisible();
    await expect(this.offerPanel).toContainText('Pa gen fen');
    await expect(this.offerPanel).not.toContainText('2036');
  }

  async saveSellerSelectionMode(): Promise<boolean> {
    await this.editOffer.click();
    await this.sellerSelectsToggle.click();

    const saveResponse = this.page.waitForResponse(
      response =>
        response
          .url()
          .includes(
            '/admin/promotions/campaigns/campaign-maryaj-gratis/rules/rule-maryaj-gratis/effects',
          ) && response.request().method() === 'PATCH',
    );
    await this.save.click();
    return (await saveResponse).ok();
  }
}
