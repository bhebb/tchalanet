import { expect, type Locator, type Page } from '@playwright/test';

export class PrivateShellPage {
  readonly userMenuTrigger: Locator;
  readonly logout: Locator;
  readonly notificationBell: Locator;
  readonly notificationMenu: Locator;
  readonly supportBanner: Locator;
  readonly supportTenantName: Locator;
  readonly supportReturn: Locator;

  constructor(private readonly page: Page) {
    this.userMenuTrigger = page.getByTestId('user-menu-trigger');
    this.logout = page.getByTestId('user-menu-logout');
    this.notificationBell = page.getByTestId('notification-bell-trigger');
    this.notificationMenu = page.getByTestId('notification-menu');
    this.supportBanner = page.getByTestId('support-access-banner');
    this.supportTenantName = page.getByTestId('support-access-tenant-name');
    this.supportReturn = page.getByTestId('support-access-return');
  }

  async logoutFromShell(): Promise<void> {
    await this.userMenuTrigger.click();
    await this.logout.click();
    await expect(this.page).toHaveURL(/\/login\b/);
  }
}
