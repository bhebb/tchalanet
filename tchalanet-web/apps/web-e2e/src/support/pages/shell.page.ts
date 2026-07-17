import { type Locator, type Page } from '@playwright/test';

/**
 * Page Object for the authenticated private shell (`tch-private-shell-layout`):
 * the user menu (logout) and the platform support banner shown when a
 * super-admin is acting on a tenant in support mode.
 */
const SUPPORT_STORAGE_KEY = 'tch.support.tenantAdminAccess';

export interface SupportSessionSeed {
  readonly tenantId: string;
  readonly tenantName: string;
  readonly tenantCode?: string;
  readonly mode?: 'SUPPORT_OVERRIDE' | 'SUPPORT_READONLY';
}

export class ShellPage {
  readonly userMenuTrigger: Locator;
  readonly logoutItem: Locator;
  /** Support banner entry: label is `Support: <tenantName>`. */
  readonly supportBanner: Locator;

  constructor(private readonly page: Page) {
    this.userMenuTrigger = page.getByTestId('user-menu-trigger');
    this.logoutItem = page.getByTestId('user-menu-logout');
    this.supportBanner = page.getByText(/Support:/);
  }

  async logout(): Promise<void> {
    await this.userMenuTrigger.click();
    await this.logoutItem.click();
  }

  /**
   * Seed a support-access session in sessionStorage before the app loads so the
   * shell renders the support banner (the store also re-hydrates from
   * `/platform/tenants/admin-access/current`, so stub that too via
   * `apiStub.startAdminAccess`).
   */
  async seedSupportSession(seed: SupportSessionSeed): Promise<void> {
    const session = {
      sessionId: 'stub-support-session',
      tenantId: seed.tenantId,
      tenantCode: seed.tenantCode ?? 'STUB',
      tenantName: seed.tenantName,
      startedAt: new Date().toISOString(),
      expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
      actorRole: 'SUPER_ADMIN',
      mode: seed.mode ?? 'SUPPORT_OVERRIDE',
      sensitiveDataMasked: false,
    };
    await this.page.addInitScript(
      ([key, value]) => window.sessionStorage.setItem(key, value),
      [SUPPORT_STORAGE_KEY, JSON.stringify(session)] as const,
    );
  }
}
