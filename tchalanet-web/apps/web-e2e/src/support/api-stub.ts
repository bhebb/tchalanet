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
 * `install()` registers the strict catch-all first and specifics after; per-test
 * overrides (e.g. `tenants([...])`) are added last and win.
 */

const json = (route: Route, body: unknown, status = 200): Promise<void> =>
  route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  });

const unexpectedApiCall = (route: Route): Promise<void> => {
  const url = new URL(route.request().url());
  return json(
    route,
    {
      status: 'ERROR',
      error: {
        code: 'WEB_E2E_UNSTUBBED_API_CALL',
        message: `Unexpected web-e2e API call: ${route.request().method()} ${url.pathname}`,
      },
      data: null,
      notices: [],
    },
    501,
  );
};

// The backend client unwraps an ApiResponse envelope (`response.data`), so every
// stubbed REST body must be wrapped the same way.
const envelope = (data: unknown) => ({ status: 'SUCCESS', data, notices: [] });

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

export interface SupportAccessSessionStub {
  sessionId: string;
  tenantId: string;
  tenantCode: string;
  tenantName: string;
  startedAt: string;
  expiresAt?: string | null;
  actorRole: 'SUPER_ADMIN';
  mode: 'SUPPORT_OVERRIDE' | 'SUPPORT_READONLY';
  sensitiveDataMasked: boolean;
}

const defaultSupportAccessSession = {
  sessionId: 'support-session-1',
  tenantId: 'stub-tenant-1',
  tenantCode: 'ACME',
  tenantName: 'Acme Lottery',
  startedAt: '2026-07-17T10:00:00Z',
  expiresAt: '2026-07-17T11:00:00Z',
  actorRole: 'SUPER_ADMIN',
  mode: 'SUPPORT_OVERRIDE',
  sensitiveDataMasked: false,
} satisfies SupportAccessSessionStub;

/**
 * Minimal `/runtime/private` bootstrap for a super-admin. Shape follows
 * `RuntimeBootstrapResponse` (see refreshSession); refine against a real run if
 * the private runtime initializer needs more fields.
 */
export const superAdminPrivateBootstrap = {
  space: 'PLATFORM',
  user: {
    userId: 'stub-super-admin',
    username: 'super_admin',
    displayName: 'Stub Super Admin',
    email: 'super_admin@e2e.local',
    roles: ['SUPER_ADMIN'],
    defaultSpace: 'PLATFORM',
    preferredLocale: 'fr',
    preferredTimezone: 'America/Toronto',
    mustChangePassword: false,
    mustCompleteProfile: false,
  },
  tenantContext: null,
  entitlements: { roles: ['SUPER_ADMIN'], permissions: [] },
  readiness: { status: 'READY', checks: [] },
  notifications: { unreadCount: 0, criticalCount: 0 },
  navigationDrawer: null,
  pageModelRef: { route: '/app/platform', endpoint: '/api/v1/platform/runtime/page' },
  entryRoute: '/app/platform',
  theme: null,
  i18n: null,
  settings: null,
  portalConfig: null,
  notices: [],
} as const;

/** `/runtime/private` bootstrap for a tenant admin. */
export const tenantAdminPrivateBootstrap = {
  space: 'ADMIN',
  user: {
    userId: 'stub-tenant-admin',
    username: 'admin',
    displayName: 'Stub Tenant Admin',
    email: 'admin@e2e.local',
    roles: ['TENANT_ADMIN'],
    defaultSpace: 'ADMIN',
    preferredLocale: 'fr',
    preferredTimezone: 'America/Toronto',
    mustChangePassword: false,
    mustCompleteProfile: false,
  },
  tenantContext: { tenantId: 'stub-tenant', tenantCode: 'STUB', tenantName: 'Stub Tenant' },
  entitlements: { roles: ['TENANT_ADMIN'], permissions: [] },
  readiness: { status: 'READY', checks: [] },
  notifications: { unreadCount: 0, criticalCount: 0 },
  navigationDrawer: null,
  pageModelRef: { route: '/app/admin', endpoint: '/api/v1/admin/runtime/page' },
  entryRoute: '/app/admin',
  theme: null,
  i18n: null,
  settings: null,
  portalConfig: null,
  notices: [],
} as const;

function firebaseEmulatorCustomToken(uid: string): string {
  const encode = (value: unknown): string =>
    btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
  const now = Math.floor(Date.now() / 1000);
  return [
    encode({ alg: 'none', typ: 'JWT' }),
    encode({
      aud: 'https://identitytoolkit.googleapis.com/google.identity.identitytoolkit.v1.IdentityToolkit',
      iat: now,
      exp: now + 3600,
      iss: 'web-e2e@test.local',
      sub: 'web-e2e@test.local',
      uid,
    }),
    '',
  ].join('.');
}

export class ApiStub {
  private readonly enabled = process.env['WEB_E2E_API'] !== '1';

  constructor(private readonly page: Page) {}

  /** Install default stubs (strict catch-all + empty tenants + super-admin bootstrap). */
  async install(): Promise<void> {
    if (!this.enabled) return;

    // RegExp matchers (not globs) to avoid URL-glob ambiguity. Catch-all first
    // (lowest precedence): stray API calls fail loudly instead of being silently
    // treated as empty data. Later-registered routes win, so specifics override it.
    await this.page.route(/\/api\/v1\//, unexpectedApiCall);
    await this.page.route(/\/public\/runtime\/bootstrap/, (r) => json(r, envelope(null)));
    await this.page.route(/\/public\/auth\/login-identifier\/resolve/, (r) =>
      json(r, envelope({ resolvedIdentifier: 'admin@e2e.local' })),
    );
    await this.page.route(/\/runtime\/private/, (r) => json(r, envelope(superAdminPrivateBootstrap)));
    await this.page.route(/\/platform\/auth\/portal-handoffs$/, (r) =>
      json(
        r,
        envelope({
          handoffId: 'handoff-1',
          code: 'secret',
          targetPortal: 'PLATFORM',
          targetUrl: 'http://localhost:4303',
          entryRoute: '/app/platform',
          expiresAt: '2026-07-17T11:00:00Z',
        }),
      ),
    );
    await this.page.route(/\/platform\/auth\/portal-handoffs\/handoff-1\/consume/, (r) =>
      json(
        r,
        envelope({
          customToken: firebaseEmulatorCustomToken('web-e2e-handoff-user'),
          targetPortal: 'PLATFORM',
          entryRoute: '/app/platform',
        }),
      ),
    );
    await this.supportAccess(defaultSupportAccessSession);
    await this.tenants([]);
  }

  /** Override the `/platform/tenants` list. */
  async tenants(items: TenantSummaryStub[], overrides: Partial<TchPageLike<TenantSummaryStub>> = {}): Promise<void> {
    if (!this.enabled) return;

    await this.page.route(/\/platform\/tenants(?:\?|$)/, (r) => json(r, envelope(page(items, overrides))));
  }

  /** Override the `/runtime/private` bootstrap. */
  async privateBootstrap(bootstrap: unknown): Promise<void> {
    if (!this.enabled) return;

    await this.page.route(/\/runtime\/private/, (r) => json(r, envelope(bootstrap)));
  }

  /** Override the username lookup used when login identifier has no "@". */
  async loginIdentifier(identifier: string, resolvedIdentifier: string): Promise<void> {
    if (!this.enabled) return;

    await this.page.route(/\/public\/auth\/login-identifier\/resolve/, async (r) => {
      const body = JSON.parse(r.request().postData() ?? '{}') as { identifier?: string };
      if (body.identifier === identifier) {
        await json(r, envelope({ resolvedIdentifier }));
        return;
      }
      await json(r, envelope({ resolvedIdentifier: body.identifier ?? resolvedIdentifier }));
    });
  }

  /** Override the support session used by support-tenant and admin handoff tests. */
  async supportAccess(session: SupportAccessSessionStub): Promise<void> {
    if (!this.enabled) return;

    await this.page.route(/\/platform\/tenants\/[^/]+\/admin-access/, (r) =>
      json(r, envelope(session)),
    );
    await this.page.route(/\/platform\/tenants\/admin-access\/current/, (r) =>
      json(r, envelope(session)),
    );
  }

  /** Configure a handoff from the current portal to the given portal. */
  async portalHandoff(targetPortal: 'ADMIN' | 'PLATFORM', targetUrl: string, entryRoute: string): Promise<void> {
    if (!this.enabled) return;

    await this.page.route(/\/platform\/auth\/portal-handoffs$/, (r) =>
      json(
        r,
        envelope({
          handoffId: 'handoff-1',
          code: 'secret',
          targetPortal,
          targetUrl,
          entryRoute,
          expiresAt: '2026-07-17T11:00:00Z',
        }),
      ),
    );
    await this.page.route(/\/platform\/auth\/portal-handoffs\/handoff-1\/consume/, (r) =>
      json(
        r,
        envelope({
          customToken: firebaseEmulatorCustomToken(`web-e2e-${targetPortal.toLowerCase()}-handoff`),
          targetPortal,
          entryRoute,
          supportAccessSessionId: targetPortal === 'ADMIN' ? defaultSupportAccessSession.sessionId : null,
        }),
      ),
    );
  }
}
