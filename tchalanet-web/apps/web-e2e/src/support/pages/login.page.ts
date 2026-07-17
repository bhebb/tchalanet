import { expect, type Locator, type Page } from '@playwright/test';

import type { Credentials } from '../env';

/**
 * Page Object for the shared login screen (`tch-login-page`, `@tch/core/auth`),
 * mounted at `/login` in all three portals. One object, reused by every
 * Playwright project (public / admin / platform) — the single web-e2e suite.
 */
export class LoginPage {
  readonly root: Locator;
  readonly identifier: Locator;
  readonly password: Locator;
  readonly submit: Locator;
  readonly error: Locator;

  constructor(private readonly page: Page) {
    this.root = page.locator('tch-login-page');
    // The login accepts an identifier (username or email).
    this.identifier = page.getByTestId('login-identifier');
    this.password = page.getByTestId('login-password');
    this.submit = page.getByTestId('login-submit');
    this.error = page.getByTestId('login-error');
  }

  async goto(): Promise<void> {
    await this.page.goto('/login');
    await expect(this.root).toBeVisible();
  }

  private async fillAndSubmit(creds: Credentials): Promise<void> {
    await this.identifier.fill(creds.email);
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
    // The error can take up to the auth-operation timeout (~15s) to surface when
    // the identity provider rejects/times out, not just on a fast auth error.
    await expect(this.error).toBeVisible({ timeout: 20_000 });
    await expect(this.page).toHaveURL(/\/login\b/);
  }
}
