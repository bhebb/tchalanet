import { expect, type Locator, type Page } from '@playwright/test';

export class AdminSellerReportPage {
  readonly root: Locator;
  readonly reportRoot: Locator;
  readonly reportTable: Locator;
  readonly exportCsv: Locator;
  readonly exportPdf: Locator;

  constructor(private readonly page: Page) {
    this.root = page.getByTestId('seller-report-table');
    this.reportRoot = page.locator('[data-report-export-root]');
    this.reportTable = this.reportRoot.locator('table');
    this.exportCsv = page.getByTestId('seller-report-export-csv');
    this.exportPdf = page.getByTestId('seller-report-export-pdf');
  }

  async goto(): Promise<void> {
    await this.page.goto('/app/admin/reports/sellers');
    await expect(this.root).toBeVisible();
  }

  async expectTerminalRows(): Promise<void> {
    await expect(this.reportTable.getByText('BD EPSILON main')).toBeVisible();
    await expect(this.reportTable.getByText('BD-EPSILON-BACKUP-4048C0B5')).toBeVisible();
  }

  async expectFrenchMoneyFormatting(): Promise<void> {
    await expect(this.reportTable.getByText('125,00').first()).toBeVisible();
    await expect(this.reportTable.locator('tfoot tr')).toBeVisible();
    await expect(this.reportTable.locator('tfoot').getByText('250,00')).toBeVisible();
  }

  async exportCsvAndFilename(): Promise<string> {
    const download = this.page.waitForEvent('download');
    await this.exportCsv.click();
    return (await download).suggestedFilename();
  }

  async expectPdfPrintScope(): Promise<void> {
    await this.page.emulateMedia({ media: 'print' });
    await this.page.locator('body').evaluate(body => body.classList.add('tch-report-printing'));
    await expect(this.reportRoot).toBeVisible();
    await expect(this.page.locator('aside.drawer')).toBeHidden();
    await this.page.locator('body').evaluate(body => body.classList.remove('tch-report-printing'));
    await this.page.emulateMedia({ media: 'screen' });
  }

  async installPrintSpy(): Promise<void> {
    await this.page.addInitScript(() => {
      (window as Window & { __tchPrintCalled?: boolean }).__tchPrintCalled = false;
      window.print = () => {
        (window as Window & { __tchPrintCalled?: boolean }).__tchPrintCalled = true;
      };
    });
    await this.page.reload();
    await expect(this.root).toBeVisible();
  }

  async expectPdfActionPrints(): Promise<void> {
    await this.exportPdf.click();
    await expect.poll(() => this.page.evaluate(() => (window as Window & { __tchPrintCalled?: boolean }).__tchPrintCalled))
      .toBe(true);
  }
}
