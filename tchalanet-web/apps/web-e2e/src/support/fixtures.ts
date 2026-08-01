import { test as base } from '@playwright/test';

import { ApiStub } from './api-stub';
import { AdminLimitsPage } from './pages/admin-limits.page';
import { AdminMaryajGratisPage } from './pages/admin-maryaj-gratis.page';
import { AdminSellerReportPage } from './pages/admin-seller-report.page';
import { AdminSellerTerminalDetailPage } from './pages/admin-seller-terminal-detail.page';
import { AdminSellerTerminalNewPage } from './pages/admin-seller-terminal-new.page';
import { AdminSetupPage } from './pages/admin-setup.page';
import { LoginPage } from './pages/login.page';
import { PrivateShellPage } from './pages/private-shell.page';
import { SupportTenantPage } from './pages/support-tenant.page';

/**
 * Shared test fixtures for the single web-e2e suite. Page objects and the API
 * stub are injected here so every Playwright project (public / admin / platform)
 * reuses the same abstractions. Import `test` / `expect` from this module, not
 * directly from `@playwright/test`.
 *
 * - `loginPage`, `supportTenantPage` — Page Objects.
 * - `apiStub` — opt-in network stub (default stubs installed); list it in a
 *   test's args to run that test backend-free / with deterministic REST data.
 *   Note: it cannot fake the Firebase session — see api-stub.ts.
 */
interface Fixtures {
  readonly loginPage: LoginPage;
  readonly privateShell: PrivateShellPage;
  readonly supportTenantPage: SupportTenantPage;
  readonly adminSetupPage: AdminSetupPage;
  readonly adminMaryajGratisPage: AdminMaryajGratisPage;
  readonly adminLimitsPage: AdminLimitsPage;
  readonly adminSellerReportPage: AdminSellerReportPage;
  readonly adminSellerTerminalDetailPage: AdminSellerTerminalDetailPage;
  readonly adminSellerTerminalNewPage: AdminSellerTerminalNewPage;
  readonly apiStub: ApiStub;
}

export const test = base.extend<Fixtures>({
  loginPage: async ({ page }, use) => {
    await use(new LoginPage(page));
  },
  privateShell: async ({ page }, use) => {
    await use(new PrivateShellPage(page));
  },
  supportTenantPage: async ({ page }, use) => {
    await use(new SupportTenantPage(page));
  },
  adminSetupPage: async ({ page }, use) => {
    await use(new AdminSetupPage(page));
  },
  adminMaryajGratisPage: async ({ page }, use) => {
    await use(new AdminMaryajGratisPage(page));
  },
  adminLimitsPage: async ({ page }, use) => {
    await use(new AdminLimitsPage(page));
  },
  adminSellerReportPage: async ({ page }, use) => {
    await use(new AdminSellerReportPage(page));
  },
  adminSellerTerminalDetailPage: async ({ page }, use) => {
    await use(new AdminSellerTerminalDetailPage(page));
  },
  adminSellerTerminalNewPage: async ({ page }, use) => {
    await use(new AdminSellerTerminalNewPage(page));
  },
  apiStub: async ({ page }, use) => {
    const stub = new ApiStub(page);
    await stub.install();
    await use(stub);
  },
});

export { expect } from '@playwright/test';
