/**
 * Env-driven test configuration (Phase 1). The web suite treats the backend
 * (firebase-emulator + seeded tenant) as a fixture — credentials and seeded ids
 * come from env so tests skip gracefully when a fixture is not provided.
 * See apps/web-e2e/README.md.
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
