import type { Page, Route } from '@playwright/test';

/**
 * API stub pattern for the web-e2e suite — Playwright network interception so a
 * spec can run against the served portals WITHOUT a real backend.
 *
 * Scope & limit (important):
 * - REST endpoints (`/api/v1/**`) are fully stubbable here — public/private
 *   runtime bootstrap, `/platform/tenants`, etc.
 * - **Auth is NOT stubbable this way.** Authentication is decided by the Firebase
 *   Auth SDK in the browser (`AUTH_CLIENT.isAuthenticated()`), not a REST call —
 *   `refreshSession()` only fetches `/runtime/private` once Firebase reports a
 *   user. So pure stubs cover the **unauthenticated** surface (public shell,
 *   login page, guard redirects). Authenticated flows still need the
 *   firebase-emulator for the session; the REST stubs below then make the data
 *   deterministic (hybrid: emulator auth + stubbed REST).
 *
 * Route precedence: Playwright matches the most-recently-added route first, so
 * `install()` registers the catch-all first and specifics after; per-test
 * overrides (e.g. `tenants([...])`) are added last and win.
 */

const json = (route: Route, body: unknown, status = 200): Promise<void> =>
  route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  });

/** A `TchPage<T>` envelope as consumed by the console pages. */
export function page<T>(items: T[], overrides: Partial<TchPageLike<T>> = {}): TchPageLike<T> {
  return {
    items,
    totalElements: items.length,
    page: 0,
    totalPages: items.length === 0 ? 1 : 1,
    hasNext: false,
    hasPrevious: false,
    ...overrides,
  };
}

export interface TchPageLike<T> {
  items: T[];
  totalElements: number;
  page: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface TenantSummaryStub {
  id?: string;
  tenantId?: string;
  code: string;
  name: string;
  status: string;
  updatedAt?: string;
}

/**
 * Minimal `/runtime/private` bootstrap for a super-admin. Shape follows
 * `RuntimeBootstrapResponse` (see refreshSession); refine against a real run if
 * the private runtime initializer needs more fields.
 */
export const superAdminPrivateBootstrap = {
  user: {
    userId: 'stub-super-admin',
    username: 'super_admin',
    email: 'super_admin@example.com',
    displayName: 'Stub Super Admin',
    roles: ['SUPER_ADMIN'],
    mustChangePassword: false,
    mustCompleteProfile: false,
  },
  entitlements: { roles: ['SUPER_ADMIN'], permissions: [] },
  space: 'PLATFORM',
  tenantContext: null,
  entryRoute: '/app/platform',
  pageModelRef: null,
} as const;

export class ApiStub {
  constructor(private readonly page: Page) {}

  /** Install default stubs (empty catch-all + empty tenants + super-admin bootstrap). */
  async install(): Promise<void> {
    // Catch-all first (lowest precedence): stray API calls resolve empty instead
    // of hanging or 500-ing the UI.
    await this.page.route('**/api/v1/**', (r) => json(r, {}));
    await this.page.route('**/api/v1/**/public/runtime/bootstrap*', (r) => json(r, {}));
    await this.page.route('**/api/v1/**/runtime/private*', (r) =>
      json(r, superAdminPrivateBootstrap),
    );
    await this.tenants([]);
  }

  /** Override the `/platform/tenants` list. */
  async tenants(items: TenantSummaryStub[], overrides: Partial<TchPageLike<TenantSummaryStub>> = {}): Promise<void> {
    await this.page.route('**/api/v1/**/platform/tenants*', (r) => json(r, page(items, overrides)));
  }

  /** Override the `/runtime/private` bootstrap. */
  async privateBootstrap(bootstrap: unknown): Promise<void> {
    await this.page.route('**/api/v1/**/runtime/private*', (r) => json(r, bootstrap));
  }
}
