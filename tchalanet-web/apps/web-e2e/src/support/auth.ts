import { expect, type Page } from '@playwright/test';

/**
 * Phase 1 auth support — see
 * openspec/changes/web-e2e-critical-flows-v1/specs/web-e2e-auth-phase1.
 *
 * Login is driven through the real UI form (data-testid on the shared
 * `tch-login-page`, lib `@tch/core/auth`). Credentials and seeded ids come from
 * env so the suite degrades gracefully (tests skip when not configured) — the
 * web suite treats the backend (firebase-emulator + seeded tenant) as a fixture,
 * it does not provision. Prerequisites: see apps/web-e2e/README.md.
 */

export type Role = 'admin' | 'superAdmin' | 'cashier';

export interface Credentials {
  readonly email: string;
  readonly password: string;
}

const ENV = process.env;

/** Credentials for a role, or null when not configured in the environment. */
export function credsFor(role: Role): Credentials | null {
  const prefix =
    role === 'admin'
      ? 'TCH_E2E_ADMIN'
      : role === 'superAdmin'
        ? 'TCH_E2E_SUPERADMIN'
        : 'TCH_E2E_CASHIER';
  const email = ENV[`${prefix}_EMAIL`];
  const password = ENV[`${prefix}_PASSWORD`];
  return email && password ? { email, password } : null;
}

/** A seeded tenant id (platform → tenant detail), or null. */
export const seededTenantId = (): string | null => ENV['TCH_E2E_TENANT_ID'] ?? null;

/** A seeded seller-terminal id (admin → seller-terminal overrides), or null. */
export const seededSellerTerminalId = (): string | null =>
  ENV['TCH_E2E_SELLER_TERMINAL_ID'] ?? null;

/**
 * Fill and submit the shared login form on the current project's `/login`.
 * Waits for the app to navigate away from `/login` (dispatch) unless
 * `expectFailure` is set (invalid-credentials path stays on `/login`).
 */
export async function loginViaUi(
  page: Page,
  creds: Credentials,
  opts: { expectFailure?: boolean } = {},
): Promise<void> {
  await page.goto('/login');
  await expect(page.locator('tch-login-page')).toBeVisible();

  await page.getByTestId('login-email').fill(creds.email);
  await page.getByTestId('login-password').fill(creds.password);
  await page.getByTestId('login-submit').click();

  if (opts.expectFailure) {
    await expect(page.getByTestId('login-error')).toBeVisible();
    await expect(page).toHaveURL(/\/login\b/);
    return;
  }

  // Successful login dispatches away from /login (spaceDispatchGuard).
  await expect(page).not.toHaveURL(/\/login\b/, { timeout: 20_000 });
  await expect(page.locator('tch-login-page')).toHaveCount(0);
}
