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

const problemDetail = (code: string, requestId: string) => ({
  type: `https://errors.tchalanet.test/${code}`,
  title: 'Service unavailable',
  status: 503,
  detail: 'backend failure: diagnostic text must not reach the UI',
  code,
  retryable: true,
  requestId,
  traceId: `trace-${requestId}`,
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

const adminOverviewStub = {
  header: {
    tenantId: 'stub-tenant',
    tenantCode: 'STUB',
    tenantName: 'Stub Tenant',
    timezone: 'America/Port-au-Prince',
    currency: 'HTG',
    tenantType: 'LOTTERY',
    tenantStatus: 'ACTIVE',
    address: {
      line1: '12 Rue Test',
      city: 'Port-au-Prince',
      region: 'Ouest',
      country: 'HT',
      postalCode: null,
    },
  },
  status: 'PARTIAL',
  missingCount: 2,
  sections: [
    { id: 'identity', labelKey: 'admin.setup.section.identity', status: 'READY', route: '/app/admin/business-profile', issues: [] },
    { id: 'address', labelKey: 'admin.setup.section.address', status: 'READY', route: '/app/admin/business-profile', issues: [] },
    { id: 'settings', labelKey: 'admin.setup.section.settings', status: 'READY', route: '/app/admin/general', issues: [] },
    { id: 'games_pricing', labelKey: 'admin.setup.section.games', status: 'READY', route: '/app/admin/games', issues: [] },
    { id: 'draws', labelKey: 'admin.setup.section.draws', status: 'MISSING', route: '/app/admin/draw-channels', issues: [] },
    { id: 'generated_draws', labelKey: 'admin.setup.section.generatedDraws', status: 'MISSING', route: '/app/admin/draws', issues: [] },
    { id: 'theme', labelKey: 'admin.setup.section.theme', status: 'UNKNOWN', route: '/app/admin/appearance', issues: [] },
  ],
  setup: {
    totalSteps: 5,
    completedSteps: 3,
    status: 'INCOMPLETE',
    canCreateSellerTerminal: false,
    blockingSteps: ['draws', 'generated_draws'],
    nextRecommendedStep: 'draws',
  },
};

const tenantConfigStub = {
  rules: { promotions: { maryajGratisEnabled: true } },
  document: {
    receipt: {
      enabled: true,
      headerMessage: 'Lotto officiel - Bonne chance',
      footerMessage: 'Merci de votre confiance',
      showQrCode: true,
    },
  },
  locale: {
    supportedLanguages: ['ht', 'fr', 'en'],
    fallbackLanguage: 'ht',
  },
};

const subscriptionStub = {
  tenantId: 'stub-tenant',
  planCode: 'HAITI_LOTTERY',
  status: 'ACTIVE',
  startedAt: '2026-07-01T00:00:00Z',
  endsAt: null,
  version: 1,
  updatedAt: '2026-07-17T00:00:00Z',
};

const gamesPricingStub = {
  games: [
    {
      gameCode: 'HT_BOLET',
      tenantGameId: { value: 'game-bolet' },
      catalogName: 'Bolèt',
      displayName: 'Bolèt',
      enabled: true,
      visibleInPos: true,
      minStake: 1,
      maxStake: 1000000,
      limits: {
        configured: true,
        assignments: [
          { ruleKey: 'MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW', params: { valueCents: 500000 } },
        ],
      },
      pricing: {
        configured: true,
        entries: [
          {
            betType: 'BORLETTE',
            betOption: null,
            pricingVariantCode: 'BORLETTE_STANDARD',
            odds: 50,
            payoutRuleType: 'STAKE_MULTIPLIER',
            fixedAmount: null,
          },
        ],
      },
    },
    {
      gameCode: 'HT_LOTO4',
      tenantGameId: { value: 'game-loto4' },
      catalogName: 'Loto 4 chif',
      displayName: 'Loto 4 chif',
      enabled: true,
      visibleInPos: true,
      minStake: 1,
      maxStake: 1000000,
      limits: { configured: true, assignments: [] },
      pricing: { configured: false, entries: [] },
    },
    {
      gameCode: 'HT_LOTO5',
      tenantGameId: { value: 'game-loto5' },
      catalogName: 'Loto 5 chif',
      displayName: 'Loto 5 chif',
      enabled: false,
      visibleInPos: false,
      minStake: 1,
      maxStake: 1000000,
      limits: { configured: true, assignments: [] },
      pricing: {
        configured: true,
        entries: [
          {
            betType: 'LOTO5',
            betOption: null,
            pricingVariantCode: 'LOTO5_STANDARD',
            odds: 5000,
            payoutRuleType: 'STAKE_MULTIPLIER',
            fixedAmount: null,
          },
        ],
      },
    },
    {
      gameCode: 'HT_MARYAJ_GRATIS',
      tenantGameId: { value: 'game-maryaj-gratis' },
      catalogName: 'Maryaj gratis',
      displayName: 'Maryaj gratis',
      enabled: true,
      visibleInPos: true,
      minStake: 1,
      maxStake: 1,
      limits: {
        configured: true,
        assignments: [
          { ruleKey: 'MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW', params: { valueCents: 500000 } },
        ],
      },
      pricing: {
        configured: true,
        entries: [
          {
            betType: 'MARRIAGE',
            betOption: null,
            pricingVariantCode: 'MARRIAGE_EXACT_ORDER',
            odds: 800,
            payoutRuleType: 'STAKE_MULTIPLIER',
            fixedAmount: null,
          },
          {
            betType: 'MARRIAGE',
            betOption: null,
            pricingVariantCode: 'MARRIAGE_REVERSE_ALLOWED',
            odds: 400,
            payoutRuleType: 'STAKE_MULTIPLIER',
            fixedAmount: null,
          },
        ],
      },
    },
  ],
};

const maryajCampaignStub = {
  id: { value: 'campaign-maryaj-gratis' },
  code: 'DEFAULT_MARYAJ_GRATIS',
  name: 'Maryaj gratis',
  status: 'ACTIVE',
  priority: 100,
  startsAt: '2026-07-01T00:00:00Z',
  endsAt: '2036-07-01T23:59:59Z',
  rules: [
    {
      id: { value: 'rule-maryaj-gratis' },
      ruleKey: 'maryaj-gratis-default',
      priority: 100,
      eligibility: [],
      effects: [
        {
          type: 'FREE_GAME_LINE',
          params: {
            gameCode: 'HT_MARYAJ_GRATIS',
            payoutBaseAmount: 50,
            quantityMode: 'TIERED_PAID_AMOUNT',
            quantity: 1,
            quantityTiers: [
              { minPaidAmount: 100, maxPaidAmount: 199, quantity: 1 },
              { minPaidAmount: 200, maxPaidAmount: 499, quantity: 2 },
              { minPaidAmount: 500, maxPaidAmount: null, quantity: 3 },
            ],
            maxQuantity: 3,
            choiceMode: 'AUTO_GENERATE',
            generationStrategy: 'RANDOM',
            regenerableBeforeConfirm: true,
            maxRegenerationsBeforeConfirm: 3,
          },
        },
      ],
    },
  ],
};

const limitRulesStub = [
  {
    ruleKey: 'BLOCK_SELECTION_PER_DRAW',
    label: 'Bloke nimewo',
    description: 'Bloke yon nimewo sou yon tiraj.',
    defaultOutcome: 'BLOCK',
    category: 'BLOCKING',
    stateless: true,
    paramsTemplate: { selections: [] },
  },
  {
    ruleKey: 'MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW',
    label: 'Plafon pa nimewo',
    description: 'Limite kantite total ki ka vann sou yon nimewo pou yon tiraj.',
    defaultOutcome: 'BLOCK',
    category: 'EXPOSURE',
    stateless: false,
    paramsTemplate: { valueCents: 500000 },
  },
];

const limitAssignmentsStub = [
  {
    id: { value: 'limit-assignment-1' },
    ruleKey: 'MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW',
    enabled: true,
    onBreach: 'BLOCK',
    params: { valueCents: 25000 },
    startsAt: null,
    endsAt: null,
  },
];

const drawChannelsStub = [
  {
    id: 'channel-ht-1000',
    channelCode: 'HT_TX_1000',
    channelName: 'HT · 1000',
    drawTime: '10:00:00',
    salesOpenTime: '00:00:00',
    cutoffTime: '09:55:00',
    timezone: 'America/Port-au-Prince',
    daysOfWeek: 'MON-SUN',
    active: true,
    resultSlotActive: true,
    defaultSource: 'EXTERNAL',
    resultSlotKey: 'HT_TX_1000',
    resultProvider: 'TX',
    resultProviderSlotCode: '1000',
    resultSlotDaysOfWeek: 'MON-SUN',
    saleReadyGameCount: 6,
    offeredGameCount: 6,
  },
  {
    id: 'channel-ga-evening',
    channelCode: 'HT_GA_EVE',
    channelName: 'Georgia · Aswè',
    drawTime: '18:59:00',
    salesOpenTime: '00:00:00',
    cutoffTime: '18:54:00',
    timezone: 'America/Port-au-Prince',
    daysOfWeek: 'MON-SAT',
    active: true,
    resultSlotActive: true,
    defaultSource: 'EXTERNAL',
    resultSlotKey: 'GA_EVE',
    resultProvider: 'GA',
    resultProviderSlotCode: 'EVE',
    resultSlotDaysOfWeek: 'MON-SAT',
    saleReadyGameCount: 4,
    offeredGameCount: 4,
  },
  {
    id: 'channel-ca-evening',
    channelCode: 'HT_CA_EVE',
    channelName: 'California · Aswè',
    drawTime: '21:00:00',
    salesOpenTime: '00:00:00',
    cutoffTime: '20:55:00',
    timezone: 'America/Port-au-Prince',
    daysOfWeek: 'MON-FRI',
    active: true,
    resultSlotActive: true,
    defaultSource: 'EXTERNAL',
    resultSlotKey: 'CA_EVE',
    resultProvider: 'CA',
    resultProviderSlotCode: 'EVE',
    resultSlotDaysOfWeek: 'MON-FRI',
    saleReadyGameCount: 0,
    offeredGameCount: 0,
  },
  {
    id: 'channel-ny-evening',
    channelCode: 'HT_NY_EVE',
    channelName: 'New York · Aswè',
    drawTime: '22:30:00',
    salesOpenTime: '00:00:00',
    cutoffTime: '22:25:00',
    timezone: 'America/Port-au-Prince',
    daysOfWeek: 'MON-SUN',
    active: false,
    resultSlotActive: true,
    defaultSource: 'EXTERNAL',
    resultSlotKey: 'NY_EVE',
    resultProvider: 'NY',
    resultProviderSlotCode: 'EVE',
    resultSlotDaysOfWeek: 'MON-SUN',
    saleReadyGameCount: 4,
    offeredGameCount: 4,
  },
  {
    id: 'channel-fl-evening',
    channelCode: 'HT_FL_EVE',
    channelName: 'Florida · Aswè',
    drawTime: '19:00:00',
    salesOpenTime: '00:00:00',
    cutoffTime: '18:55:00',
    timezone: 'America/Port-au-Prince',
    daysOfWeek: 'SAT,SUN',
    active: true,
    resultSlotActive: false,
    defaultSource: 'EXTERNAL',
    resultSlotKey: 'FL_EVE',
    resultProvider: 'FL',
    resultProviderSlotCode: 'EVE',
    resultSlotDaysOfWeek: 'SAT,SUN',
    saleReadyGameCount: 4,
    offeredGameCount: 4,
  },
];

const drawSalesMatrixStub = {
  summary: {
    providerCount: 1,
    slotCount: 1,
    configuredChannelCount: 1,
    activeChannelCount: 1,
    supportedTenantGameCount: 2,
    offeredChannelGameCount: 2,
    saleReadyChannelGameCount: 2,
    missingStakeConfigCount: 0,
    missingLimitCount: 0,
  },
  providers: [
    {
      providerCode: 'NY',
      slots: [
        {
          slotKey: 'NY_MID',
          labelKey: null,
          resultSlot: {
            resultSlotId: 'result-slot-ny-mid',
            drawTime: '14:30:00',
            daysOfWeek: 'MON-SUN',
            active: true,
          },
          channel: {
            drawChannelId: 'channel-ny-mid',
            channelCode: 'HT_NY_MID',
            active: true,
            configured: true,
            drawTime: '14:30:00',
            salesOpenTime: '05:30:00',
            cutoffSec: 300,
            defaultSource: 'EXTERNAL',
            sortOrder: 1,
            dependsOnChannelId: null,
          },
          games: [
            {
              gameCode: 'HT_BOLET',
              tenantGameId: 'game-bolet',
              displayName: 'Bolèt',
              enabledForTenant: true,
              visibleInPos: true,
              offeredOnChannel: true,
              enabledOnChannel: true,
              minStake: 1,
              maxStake: 1000000,
              limits: { configured: true, assignments: [] },
              saleReady: true,
              warnings: [],
            },
            {
              gameCode: 'HT_MARYAJ_GRATIS',
              tenantGameId: 'game-maryaj-gratis',
              displayName: 'Maryaj gratis',
              enabledForTenant: true,
              visibleInPos: true,
              offeredOnChannel: true,
              enabledOnChannel: true,
              minStake: 1,
              maxStake: 1,
              limits: { configured: true, assignments: [] },
              saleReady: true,
              warnings: [],
            },
          ],
          slotReady: true,
          warnings: [],
        },
      ],
    },
  ],
};

const sellerReportStub = {
  analytics: {
    available: true,
    trustState: 'READY',
    trustReasonCode: 'READY',
    missingBusinessDates: [],
  },
  from: '2026-07-09',
  to: '2026-07-17',
  summary: {
    ticketsSold: 24,
    grossSales: 250,
    winningsCalculated: 0,
    payoutsPaid: 0,
    sellerCommission: 25,
    buyerCharges: 0,
    sellerCharges: 0,
    tenantCharges: 0,
    waivedCharges: 0,
    promotionLines: 4,
    promotionPricedLines: 0,
    promotionPayoutBase: 0,
    netRevenueEstimated: 225,
    netRevenuePaidBasis: 225,
  },
  rows: [
    {
      sellerTerminalId: 'seller-terminal-main',
      terminalCode: 'BD-EPSILON-MAIN-4048C0B5',
      displayName: 'BD EPSILON main',
      status: 'ACTIVE',
      refDate: '2026-07-17',
      ticketsSold: 12,
      grossSales: 125,
      sellerCommission: 12.5,
      buyerCharges: 0,
      sellerCharges: 0,
      tenantCharges: 0,
      waivedCharges: 0,
      drawCount: 1,
      averageGrossSalesPerDraw: 125,
      netRevenueEstimated: 112.5,
      netRevenuePaidBasis: 112.5,
    },
    {
      sellerTerminalId: 'seller-terminal-backup',
      terminalCode: 'BD-EPSILON-BACKUP-4048C0B5',
      displayName: 'BD EPSILON backup',
      status: 'ACTIVE',
      refDate: '2026-07-17',
      ticketsSold: 12,
      grossSales: 125,
      sellerCommission: 12.5,
      buyerCharges: 0,
      sellerCharges: 0,
      tenantCharges: 0,
      waivedCharges: 0,
      drawCount: 1,
      averageGrossSalesPerDraw: 125,
      netRevenueEstimated: 112.5,
      netRevenuePaidBasis: 112.5,
    },
  ],
};

const tenantAdminPermissions = [
  'draw_channel.manage',
  'game-pricing.update',
];

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
  entitlements: { roles: ['TENANT_ADMIN'], permissions: tenantAdminPermissions },
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

/** Tenant-admin runtime without setup mutation permissions. */
export const tenantAdminReadonlyPrivateBootstrap = {
  ...tenantAdminPrivateBootstrap,
  entitlements: { roles: ['TENANT_ADMIN'], permissions: [] },
} as const;

/** `/runtime/private` bootstrap after a super-admin handoff into tenant support mode. */
export const supportTenantAdminPrivateBootstrap = {
  ...tenantAdminPrivateBootstrap,
  user: {
    ...tenantAdminPrivateBootstrap.user,
    userId: 'stub-super-admin',
    username: 'super_admin',
    displayName: 'Stub Super Admin',
    email: 'super_admin@e2e.local',
    roles: ['SUPER_ADMIN'],
    defaultSpace: 'ADMIN',
  },
  entitlements: { roles: ['SUPER_ADMIN'], permissions: [] },
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

  /**
   * API stubs must never capture the document navigation itself. A matcher such as
   * `/admin/seller-terminals` also matches the browser URL `/app/admin/seller-terminals`;
   * without this guard Playwright returns the JSON fixture as the page document and the
   * Angular shell never boots.
   */
  private async apiRoute(
    matcher: Parameters<Page['route']>[0],
    handler: (route: Route) => void | Promise<void>,
  ): Promise<void> {
    await this.page.route(matcher, async route => {
      if (route.request().resourceType() === 'document') {
        await route.continue();
        return;
      }
      await handler(route);
    });
  }

  /** Install default stubs (strict catch-all + empty tenants + super-admin bootstrap). */
  async install(): Promise<void> {
    if (!this.enabled) return;

    // RegExp matchers (not globs) to avoid URL-glob ambiguity. Catch-all first
    // (lowest precedence): stray API calls fail loudly instead of being silently
    // treated as empty data. Later-registered routes win, so specifics override it.
    await this.apiRoute(/\/api\/v1\//, unexpectedApiCall);
    await this.apiRoute(/\/public\/runtime\/bootstrap/, (r) => json(r, envelope(null)));
    await this.apiRoute(/\/public\/auth\/login-identifier\/resolve/, (r) =>
      json(r, envelope({ resolvedIdentifier: 'admin@e2e.local' })),
    );
    await this.apiRoute(/\/runtime\/private/, (r) => json(r, envelope(superAdminPrivateBootstrap)));
    await this.apiRoute(/\/(?:admin|platform)\/notifications\/unread-count(?:\?|$)/, (r) =>
      json(r, envelope({ unreadCount: 0 })),
    );
    await this.apiRoute(/\/(?:admin|platform)\/notifications(?:\?|$)/, (r) =>
      json(r, envelope(page([]))),
    );
    await this.apiRoute(/\/platform\/auth\/portal-handoffs$/, (r) =>
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
    await this.apiRoute(/\/platform\/auth\/portal-handoffs\/handoff-1\/consume/, (r) =>
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
    await this.adminBusiness();
    await this.tenants([]);
  }

  /** Override the `/platform/tenants` list. */
  async tenants(items: TenantSummaryStub[], overrides: Partial<TchPageLike<TenantSummaryStub>> = {}): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/platform\/tenants(?:\?|$)/, (r) => json(r, envelope(page(items, overrides))));
  }

  /** Deterministic operator response for platform analytics reconciliation. */
  async analyticsReconciliation(result: unknown): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/platform\/ops\/analytics\/reconciliation$/, (r) =>
      json(r, envelope(result)),
    );
  }

  /** Override the `/runtime/private` bootstrap. */
  async privateBootstrap(bootstrap: unknown): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/runtime\/private/, (r) => json(r, envelope(bootstrap)));
  }

  /** Override the username lookup used when login identifier has no "@". */
  async loginIdentifier(identifier: string, resolvedIdentifier: string): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/public\/auth\/login-identifier\/resolve/, async (r) => {
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

    await this.apiRoute(/\/platform\/tenants\/[^/]+\/admin-access/, (r) =>
      json(r, envelope(session)),
    );
    await this.apiRoute(/\/platform\/tenants\/admin-access\/current/, (r) =>
      json(r, envelope(session)),
    );
  }

  /** Configure a handoff from the current portal to the given portal. */
  async portalHandoff(targetPortal: 'ADMIN' | 'PLATFORM', targetUrl: string, entryRoute: string): Promise<void> {
    if (!this.enabled) return;

    // Handoff tests run in parallel against the same Firebase emulator. Keep
    // both the one-time handoff id and the emulator identity isolated per page
    // so one test cannot consume or replace another test's session.
    const handoffId = `handoff-${Date.now()}-${Math.random().toString(36).slice(2)}`;
    const customToken = firebaseEmulatorCustomToken(
      `web-e2e-${targetPortal.toLowerCase()}-${handoffId}`,
    );

    await this.apiRoute(/\/platform\/auth\/portal-handoffs$/, (r) =>
      json(
        r,
        envelope({
          handoffId,
          code: 'secret',
          targetPortal,
          targetUrl,
          entryRoute,
          expiresAt: '2026-07-17T11:00:00Z',
        }),
      ),
    );
    await this.apiRoute(
      new RegExp(`/platform/auth/portal-handoffs/${handoffId}/consume`),
      (r) =>
        json(
          r,
          envelope({
            customToken,
            targetPortal,
            entryRoute,
            supportAccessSessionId:
              targetPortal === 'ADMIN' ? defaultSupportAccessSession.sessionId : null,
          }),
        ),
    );
  }

  /** Deterministic admin business screens used by the browser E2E suite. */
  async adminBusiness(): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/admin\/overview(?:\?|$)/, (r) => json(r, envelope(adminOverviewStub)));
    await this.apiRoute(/\/tenant\/subscription(?:\?|$)/, (r) => json(r, envelope(subscriptionStub)));
    await this.apiRoute(/\/admin\/tenant-config(?:\?|$)/, (r) => json(r, envelope(tenantConfigStub)));
    await this.apiRoute(/\/admin\/setup\/games-pricing(?:\?|$)/, (r) => json(r, envelope(gamesPricingStub)));
    await this.apiRoute(/\/admin\/games\/[^/]+\/(?:enable|disable)(?:\?|$)/, r =>
      r.request().method() === 'POST' ? json(r, envelope(null)) : unexpectedApiCall(r),
    );
    await this.apiRoute(/\/admin\/games\/[^/]+\/bet-options(?:\?|$)/, r =>
      json(r, envelope({ gameCode: 'HT_BOLET', betTypes: [] })),
    );
    await this.apiRoute(/\/admin\/promotions\/campaigns(?:\?|$)/, (r) =>
      json(r, envelope(page([maryajCampaignStub]))),
    );
    await this.apiRoute(/\/admin\/promotions\/campaigns\/campaign-maryaj-gratis$/, (r) =>
      json(r, envelope(maryajCampaignStub)),
    );
    await this.apiRoute(/\/admin\/promotions\/campaigns\/campaign-maryaj-gratis\/rules\/rule-maryaj-gratis\/effects$/, (r) =>
      json(r, envelope(maryajCampaignStub)),
    );
    await this.apiRoute(/\/admin\/promotions\/campaigns\/campaign-maryaj-gratis$/, (r) =>
      json(r, envelope(maryajCampaignStub)),
    );
    await this.apiRoute(/\/admin\/policies\/limits\/rules(?:\?|$)/, (r) => json(r, envelope(limitRulesStub)));
    await this.apiRoute(/\/admin\/policies\/limits\/assignments(?:\?|$)/, (r) => {
      if (r.request().method() === 'PUT') {
        return json(r, envelope({ id: { value: 'limit-assignment-new' } }));
      }
      return json(r, envelope({ limitScopeRef: null, items: limitAssignmentsStub }));
    });
    await this.apiRoute(/\/admin\/reports\/seller-terminals(?:\?|$)/, (r) =>
      json(r, envelope(sellerReportStub)),
    );
    await this.apiRoute(/\/admin\/seller-terminals\/summary(?:\?|$)/, (r) =>
      json(
        r,
        envelope({
          activeCount: 2,
          blockedCount: 0,
          salesTodayAmount: 0,
          averageCommissionRate: 10,
          currency: 'HTG',
        }),
      ),
    );
    await this.apiRoute(/\/admin\/seller-terminals(?:\?|$)/, (r) =>
      json(r, envelope(page([
        {
          id: { value: 'seller-terminal-main' },
          terminalCode: 'BD-EPSILON-MAIN-4048C0B5',
          displayName: 'BD EPSILON main',
          status: 'ACTIVE',
        },
        {
          id: { value: 'seller-terminal-backup' },
          terminalCode: 'BD-EPSILON-BACKUP-4048C0B5',
          displayName: 'BD EPSILON backup',
          status: 'ACTIVE',
        },
      ]))),
    );

    await this.adminBusinessProfile();
  }

  /** Deterministic terminal detail data used by the admin detail-page contract. */
  async adminSellerTerminalDetail(): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/admin\/seller-terminals\/stub-terminal-1\/detail(?:\?|$)/, r =>
      json(
        r,
        envelope({
          terminal: {
            id: { value: 'stub-terminal-1' },
            tenantId: { value: 'stub-tenant' },
            terminalCode: 'POS-001',
            displayName: 'Bhebbb',
            firstName: 'Stevens',
            lastName: 'Nelson',
            email: 'stevens@example.test',
            phoneNumber: '+509 3700 0000',
            status: 'ACTIVE',
            commissionRate: 13,
            addressId: { value: 'stub-address' },
            activatedAt: '2026-07-23T19:20:00Z',
            lastSeenAt: '2026-07-31T13:08:00Z',
            blockedAt: null,
            blockedReason: null,
            disabledAt: null,
            mustChangePin: false,
            pinResetAt: null,
          },
          tenant: {
            tenantId: { value: 'stub-tenant' },
            code: 'tchalanet',
            name: 'Tchalanet LLC',
            displayName: 'Tchalanet',
          },
          clientDiagnostics: {
            enabled: true,
            expiresAt: '2026-08-22T12:53:00Z',
            maxEvents: 100,
            categories: ['API', 'CONNECTIVITY', 'SALE', 'PRINT'],
            reason: 'Support test',
            updatedAt: '2026-08-22T10:53:00Z',
          },
          todayStats: {
            sellerTerminalId: 'stub-terminal-1',
            refDate: '2026-07-31',
            ticketsSold: 4,
            grossSales: 1894,
            sellerCommission: 246.22,
            buyerCharges: 0,
            sellerCharges: 0,
            tenantCharges: 0,
            waivedCharges: 0,
            promotionLines: 0,
            promotionPricedLines: 0,
            netRevenueEstimated: 1647.78,
            netRevenuePaidBasis: 1647.78,
          },
          limits: {
            specs: limitRulesStub,
            assignments: limitAssignmentsStub,
            inheritedAssignments: [],
          },
        }),
      ),
    );
    await this.apiRoute(/\/admin\/policies\/limits\/assignments(?:\?|$)/, (r) => {
      if (r.request().method() === 'PUT') {
        return json(r, envelope({ id: { value: 'limit-assignment-new' } }));
      }
      return unexpectedApiCall(r);
    });
  }

  /** Deterministic seller configuration data used by the pricing override flow. */
  async adminSellerConfiguration(): Promise<void> {
    if (!this.enabled) return;

    let overrideValue = 10;
    let overrideActive = true;

    await this.apiRoute(/\/admin\/commission\/sellers(?:\?|$)/, r =>
      json(
        r,
        envelope([
          {
            sellerTerminalId: 'stub-terminal-1',
            terminalCode: 'POS-001',
            displayName: 'Bhebbb',
            status: 'ACTIVE',
            commissionRate: 13,
            rateSource: 'CUSTOM',
          },
        ]),
      ),
    );

    await this.apiRoute(/\/admin\/controls\/pricing-rules(?:\?|$)/, r =>
      json(
        r,
        envelope([
          {
            id: 'pricing-bolet-straight',
            gameCode: 'BOLET',
            pricingVariantCode: 'DEFAULT',
            betType: 'STRAIGHT',
            betOption: null,
            odds: 10,
            payoutRuleType: 'STAKE_MULTIPLIER',
            fixedAmount: null,
            active: true,
          },
        ]),
      ),
    );

    await this.apiRoute(
      /\/admin\/controls\/pricing-rules\/seller-terminals\/stub-terminal-1(?:\?|$)/,
      async r => {
        if (r.request().method() === 'PUT') {
          const body = JSON.parse(r.request().postData() ?? '{}') as { odds?: number };
          overrideValue = body.odds ?? overrideValue;
          overrideActive = true;
          await json(r, envelope({ id: 'override-bolet-straight' }));
          return;
        }

        await json(
          r,
          envelope(
            overrideActive
              ? [
                  {
                    id: 'override-bolet-straight',
                    gameCode: 'BOLET',
                    pricingVariantCode: 'DEFAULT',
                    betType: 'STRAIGHT',
                    betOption: null,
                    odds: overrideValue,
                    payoutRuleType: 'STAKE_MULTIPLIER',
                    fixedAmount: null,
                    active: true,
                    effectiveFrom: '2026-08-01T00:00:00Z',
                    effectiveTo: null,
                    reason: 'web-e2e',
                  },
                ]
              : [],
          ),
        );
      },
    );

    await this.apiRoute(
      /\/admin\/controls\/pricing-rules\/seller-terminals\/stub-terminal-1\/overrides\/override-bolet-straight(?:\?|$)/,
      async r => {
        if (r.request().method() !== 'DELETE') {
          await unexpectedApiCall(r);
          return;
        }
        overrideActive = false;
        await json(r, envelope(null));
      },
    );
  }

  /** Deterministic draw-channel configuration data for mobile/desktop UX checks. */
  async adminDrawChannels(): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/tenant\/draw-channels\/[^/?]+(?:\?|$)/, async r => {
      const url = new URL(r.request().url());
      const id = url.pathname.split('/').pop();
      const channel = drawChannelsStub.find(item => item.id === id) ?? drawChannelsStub[0];
      await json(
        r,
        envelope({
          id: channel.id,
          code: channel.channelCode,
          name: channel.channelName,
          label: channel.channelName,
          timezone: channel.timezone,
          drawTime: channel.drawTime,
          salesOpenTime: channel.salesOpenTime,
          cutoffSec: 300,
          daysOfWeek: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'],
          active: channel.active,
          sortOrder: 1,
          period: null,
          flags: {},
          notes: null,
          resultSlotId: `slot-${channel.id}`,
          defaultSource: channel.defaultSource,
          resultSlotKey: channel.resultSlotKey,
          resultProvider: channel.resultProvider,
          resultProviderSlotCode: channel.resultProviderSlotCode,
          resultSlotDaysOfWeek: channel.resultSlotDaysOfWeek,
          resultSlotActive: channel.resultSlotActive,
        }),
      );
    });

    await this.apiRoute(/\/tenant\/draw-channels\/[^/]+\/active(?:\?|$)/, r =>
      r.request().method() === 'PATCH' ? json(r, envelope(null)) : unexpectedApiCall(r),
    );

    await this.apiRoute(/\/tenant\/draw-channels(?:\?|$)/, r =>
      json(r, envelope(drawChannelsStub)),
    );
  }

  /** Deterministic game-by-draw matrix data and successful mutations. */
  async adminDrawSalesMatrix(): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/admin\/setup\/draw-sales-matrix(?:\?|$)/, r =>
      json(r, envelope(drawSalesMatrixStub)),
    );
    await this.apiRoute(/\/admin\/draw-channels\/[^/]+\/tenant-games\/[^/]+(?:\?|$)/, r => {
      if (r.request().method() === 'PATCH' || r.request().method() === 'PUT') {
        return json(r, envelope(null));
      }
      return unexpectedApiCall(r);
    });
  }

  /** Inject a blocking failure for the terminal detail resource. */
  async adminSellerTerminalDetailError(): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/admin\/seller-terminals\/stub-terminal-1\/detail(?:\?|$)/, r =>
      json(r, problemDetail('admin.sellerTerminal.unavailable', 'req-terminal-detail-503'), 503),
    );
  }

  /** Deterministic setup data used by the routed seller-terminal form contract. */
  async adminSellerTerminalNew(): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/admin\/seller-terminals\/suggested-code(?:\?|$)/, r =>
      json(r, envelope({ terminalCode: 'POS-E2E-001' })),
    );
    await this.apiRoute(/\/admin\/commission\/overview(?:\?|$)/, r =>
      json(r, envelope({ tenantDefaultRate: 10 })),
    );
  }

  /** Deterministic tenant-config for the params-overview and receipt/delivery/calendar pages. */
  async adminParamsOverview(): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/admin\/tenant-config(?:\?|$)/, r =>
      json(r, envelope({
        document: {
          receipt: { enabled: true, defaultPaperSize: 'RECEIPT_80MM', showQrCode: true },
        },
        communication: {
          buyerTicketDelivery: {
            sms: { enabled: false },
            whatsapp: { enabled: false },
            email: { enabled: true },
          },
        },
        rules: {
          businessCalendar: {
            defaultOpen: true,
            closedWeekdays: [],
            holidays: [],
          },
        },
      })),
    );
  }

  /** Deterministic business-profile data and successful mutations. */
  async adminBusinessProfile(): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/admin\/commission\/overview(?:\?|$)/, r =>
      json(r, envelope({ tenantDefaultRate: 10 })),
    );
    await this.apiRoute(/\/admin\/commission\/default-rate(?:\?|$)/, r =>
      r.request().method() === 'PUT' ? json(r, envelope(null)) : unexpectedApiCall(r),
    );
    await this.apiRoute(/\/admin\/tenant\/address(?:\?|$)/, r =>
      r.request().method() === 'PUT' ? json(r, envelope(null)) : unexpectedApiCall(r),
    );
  }

  /** Inject a targeted commission validation violation for the profile form. */
  async adminBusinessProfileCommissionFieldError(): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/admin\/commission\/default-rate(?:\?|$)/, r =>
      json(
        r,
        {
          ...problemDetail('validation.failed', 'req-commission-422'),
          status: 422,
          violations: [
            {
              code: 'validation.out_of_range',
              field: 'rate',
              target: 'commission.rate',
              message: 'backend commission diagnostic must not reach the UI',
            },
          ],
        },
        422,
      ),
    );
  }

  /** Inject a blocking section/form error for the tenant address mutation. */
  async adminBusinessProfileAddressError(): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/admin\/tenant\/address(?:\?|$)/, r =>
      json(r, problemDetail('tenantadmin.overview.address_unavailable', 'req-address-503'), 503),
    );
  }

  /** Return targeted business notices without blocking the setup checklist. */
  async adminSetupBusinessNotices(): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/admin\/overview(?:\?|$)/, r =>
      json(r, {
        ...envelope(adminOverviewStub),
        status: 'SUCCESS_WITH_WARNINGS',
        notices: [
          {
            code: 'admin.setup.theme.info',
            message: 'backend informational diagnostic must not reach the UI',
            severity: 'INFO',
            kind: 'INFORMATION',
            target: 'admin.setup.theme',
            meta: { category: 'business_rule', surface: 'section' },
          },
          {
            code: 'admin.setup.commission.warning',
            message: 'backend warning diagnostic must not reach the UI',
            severity: 'WARN',
            kind: 'BUSINESS',
            target: 'admin.setup.commission',
          },
        ],
      }),
    );
  }

  /** Return a ready setup where POS/printing remains an operational recommendation. */
  async adminSetupPrintingMissingOperational(): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/admin\/overview(?:\?|$)/, r =>
      json(
        r,
        envelope({
          ...adminOverviewStub,
          status: 'READY',
          missingCount: 0,
          sections: adminOverviewStub.sections.map(section =>
            section.id === 'settings'
              ? {
                  ...section,
                  status: 'READY',
                  issues: [
                    {
                      key: 'settings.print.paper_size_missing',
                      messageKey: 'settings.print.paper_size_missing',
                      route: '/app/admin/company/settings/config#print',
                    },
                  ],
                }
              : section.id === 'draws' || section.id === 'generated_draws'
                ? { ...section, status: 'READY', issues: [] }
                : section,
          ),
          setup: {
            totalSteps: 5,
            completedSteps: 5,
            status: 'COMPLETE',
            canCreateSellerTerminal: true,
            blockingSteps: [],
            nextRecommendedStep: null,
          },
        }),
      ),
    );
  }

  /** Deterministic POS data used by the mobile sale-flow browser test. */
  async posSale(): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/admin\/seller-terminals\/stub-terminal-1(?:\?|$)/, r =>
      json(
        r,
        envelope({
          id: { value: 'stub-terminal-1' },
          terminalCode: 'POS-001',
          displayName: 'Terminal mobile E2E',
          status: 'ACTIVE',
          commissionRate: 10,
        }),
      ),
    );
    await this.apiRoute(/\/tenant\/cashier\/draws\/available(?:\?|$)/, r =>
      json(
        r,
        envelope([
          {
            drawId: 'draw-mobile-1',
            drawChannelId: 'channel-mobile-1',
            drawDate: '2099-01-01',
            resultSlotKey: 'MORNING',
            channelCode: 'GA-LA',
            channelLabel: 'GA - La',
            gameCodes: ['HT_BOLET'],
            status: 'OPEN',
            scheduledAt: '2099-01-01T12:00:00Z',
            cutoffAt: '2099-01-01T11:30:00Z',
          },
        ]),
      ),
    );
    await this.apiRoute(/\/tenant\/cashier\/games\/available(?:\?|$)/, r =>
      json(
        r,
        envelope([
          {
            gameCode: 'HT_BOLET',
            gameLabel: 'Borlette',
            betType: 'MATCH_1_2D',
            betTypeLabel: 'Boul',
            requiresOption: false,
            selectionPolicy: 'IMPLICIT_BEST_MATCH',
            options: [],
            selectionHint: '2 chiffres',
          },
        ]),
      ),
    );
    await this.apiRoute(/\/tenant\/cashier\/tickets\/stats(?:\?|$)/, r =>
      json(r, envelope({ ticketCount: 0, salesTotalCents: 0, currency: 'HTG' })),
    );
  }

  /** Inject blocking ProblemDetails for the admin error-management E2E flows. */
  async adminDashboardError(): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/tenant\/dashboard(?:\?|$)/, r =>
      json(r, problemDetail('admin.dashboard.unavailable', 'req-dashboard-503'), 503),
    );
    await this.apiRoute(/\/assets\/config\/page-private-fallback\.json(?:\?|$)/, r =>
      json(r, problemDetail('admin.dashboard.fallback_unavailable', 'req-dashboard-fallback-503'), 503),
    );
  }

  async adminSetupError(): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/admin\/overview(?:\?|$)/, r =>
      json(r, problemDetail('admin.setup.unavailable', 'req-setup-503'), 503),
    );
  }

  /** Fail the first setup read, then return the normal overview on retry. */
  async adminSetupErrorOnce(): Promise<void> {
    if (!this.enabled) return;

    let failed = false;
    await this.apiRoute(/\/admin\/overview(?:\?|$)/, r => {
      if (!failed) {
        failed = true;
        return json(r, problemDetail('admin.setup.unavailable', 'req-setup-503'), 503);
      }
      return json(r, envelope(adminOverviewStub));
    });
  }

  async adminReportError(): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/api\/v1\/admin\/reports\/overview(?:\?|$)/, r =>
      json(r, problemDetail('admin.reports.overview.unavailable', 'req-report-503'), 503),
    );
  }

  async adminDrawsError(): Promise<void> {
    if (!this.enabled) return;

    await this.apiRoute(/\/api\/v1\/admin\/draws(?:\?|$)/, r =>
      json(r, problemDetail('admin.generatedDraws.unavailable', 'req-draws-503'), 503),
    );
  }
}
