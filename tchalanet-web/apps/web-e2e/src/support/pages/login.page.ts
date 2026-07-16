import { expect, type Locator, type Page } from '@playwright/test';

import type { Credentials } from '../env';

/**
 * Page Object for the shared login screen (`tch-login-page`, `@tch/core/auth`),
 * mounted at `/login` in all three portals. One object, reused by every
 * Playwright project (public / admin / platform) — the single web-e2e suite.
 */
export class LoginPage {
  readonly root: Locator;
  readonly email: Locator;
  readonly password: Locator;
  readonly submit: Locator;
  readonly error: Locator;

  constructor(private readonly page: Page) {
    this.root = page.locator('tch-login-page');
    this.email = page.getByTestId('login-email');
    this.password = page.getByTestId('login-password');
    this.submit = page.getByTestId('login-submit');
    this.error = page.getByTestId('login-error');
  }

  async goto(): Promise<void> {
    await this.page.goto('/login');
    await expect(this.root).toBeVisible();
  }

  private async fillAndSubmit(creds: Credentials): Promise<void> {
    await this.email.fill(creds.email);
    await this.password.fill(creds.password);
    await this.submit.click();
  }

  /** Log in expecting a successful dispatch away from `/login`. */
  async login(creds: Credentials): Promise<void> {
    await this.goto();
    await this.fillAndSubmit(creds);
    await expect(this.page).not.toHaveURL(/\/login\b/, { timeout: 20_000 });
    await expect(this.root).toHaveCount(0);
  }

  /** Log in expecting an inline error and no navigation. */
  async loginExpectingError(creds: Credentials): Promise<void> {
    await this.goto();
    await this.fillAndSubmit(creds);
    await expect(this.error).toBeVisible();
    await expect(this.page).toHaveURL(/\/login\b/);
  }
}
