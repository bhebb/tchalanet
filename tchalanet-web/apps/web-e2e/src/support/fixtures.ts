import { test as base } from '@playwright/test';

import { LoginPage } from './pages/login.page';
import { SupportTenantPage } from './pages/support-tenant.page';

/**
 * Shared test fixtures for the single web-e2e suite. Page objects are injected
 * here so every Playwright project (public / admin / platform) reuses the same
 * page abstractions. Import `test` / `expect` from this module, not directly
 * from `@playwright/test`.
 */
interface Fixtures {
  readonly loginPage: LoginPage;
  readonly supportTenantPage: SupportTenantPage;
}

export const test = base.extend<Fixtures>({
  loginPage: async ({ page }, use) => {
    await use(new LoginPage(page));
  },
  supportTenantPage: async ({ page }, use) => {
    await use(new SupportTenantPage(page));
  },
});

export { expect } from '@playwright/test';
