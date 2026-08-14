import { expect, test } from '../support/fixtures';
import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';

/**
 * Draw detail — "Numéros à risque" exposure widget — Slice 7 regression guard.
 *
 * - exposureAlerts non-vide + limitConfigured → section visible avec chips colorées
 * - exposureAlerts vide                       → section absente
 * - limitConfigured: false                    → section absente
 */

const creds = credsFor('admin');

const DRAW_ID_WITH_ALERTS = 'aaaaaaaa-0000-0000-0000-000000000010';
const DRAW_ID_EMPTY_ALERTS = 'aaaaaaaa-0000-0000-0000-000000000011';
const DRAW_ID_NO_LIMIT = 'aaaaaaaa-0000-0000-0000-000000000012';
const DRAW_CHANNEL_ID = 'cccccccc-0000-0000-0000-000000000001';

function overviewBody(drawId: string, exposureAlerts: unknown[], effectiveLimits: unknown[]) {
  return JSON.stringify({
    status: 'SUCCESS',
    data: {
      draw: {
        drawId,
        drawChannelId: DRAW_CHANNEL_ID,
        drawChannelCode: 'TEST',
        drawChannelLabel: 'Canal test',
        drawDate: '2026-08-12',
        status: 'OPEN',
        scheduledAt: '2026-08-12T14:00:00Z',
        openedAt: '2026-08-12T08:00:00Z',
        closedAt: null,
        result: null,
      },
      topSelections: [
        {
          rank: 1,
          displaySelection: '12',
          gameCode: 'HT_BOLET',
          betType: 'MATCH_1_2D',
          count: 9,
          totalStakeCents: 90000,
        },
      ],
      effectiveLimits,
      exposureAlerts,
    },
    notices: [],
  });
}

function stubDraw(page: import('@playwright/test').Page, drawId: string) {
  return page.route(new RegExp(`/api/v1/admin/draws/${drawId}(?:\\?|$)`), route => {
    if (route.request().resourceType() === 'document') {
      route.continue();
      return;
    }
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        status: 'SUCCESS',
        data: {
          id: drawId,
          tenantId: 'stub-tenant',
          channel: {
            id: DRAW_CHANNEL_ID,
            code: 'HT_TX_1000',
            name: 'Texas · 1000',
          },
          slot: {
            id: 'slot-1',
            key: 'HT_TX_1000',
            label: 'Texas · 1000',
            timezone: 'America/Port-au-Prince',
            drawTime: '10:00',
          },
          drawDate: '2026-08-12',
          scheduledAt: '2026-08-12T14:00:00Z',
          cutoffAt: '2026-08-12T13:55:00Z',
          status: 'OPEN',
          active: true,
          lastResult: null,
        },
        notices: [],
      }),
    });
  });
}

async function stubStaticLimits(page: import('@playwright/test').Page) {
  await page.route('**/api/v1/admin/policies/limits/rules**', route =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        status: 'SUCCESS',
        data: [
          {
            ruleKey: 'BLOCK_SELECTION_PER_DRAW',
            label: 'Numéros bloqués par tirage',
            description: 'Bloque une ou plusieurs sélections pour un tirage.',
            defaultOutcome: 'BLOCK',
            category: 'BLOCKING',
            stateless: false,
            paramsTemplate: { selections: [] },
          },
          {
            ruleKey: 'MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW',
            label: 'Plafond par numéro',
            description: 'Bloque une sélection au-dessus du plafond.',
            defaultOutcome: 'BLOCK',
            category: 'EXPOSURE',
            stateless: false,
            paramsTemplate: { valueCents: 100000 },
          },
        ],
        notices: [],
      }),
    }),
  );

  await page.route('**/api/v1/admin/policies/limits/assignments**', route => {
    const url = new URL(route.request().url());
    const target = url.searchParams.get('target');
    const items =
      target === 'DRAW_CHANNEL'
        ? [
            {
              id: { value: 'draw-block-1' },
              ruleKey: 'BLOCK_SELECTION_PER_DRAW',
              enabled: true,
              onBreach: 'BLOCK',
              params: { selections: ['12', '45'] },
              startsAt: null,
              endsAt: null,
            },
          ]
        : [
            {
              id: { value: 'tenant-exposure-1' },
              ruleKey: 'MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW',
              enabled: true,
              onBreach: 'WARN',
              params: { valueCents: 100000 },
              startsAt: null,
              endsAt: null,
            },
          ];

    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        status: 'SUCCESS',
        data: { limitScopeRef: null, items },
        notices: [],
      }),
    });
  });
}

async function stubActivity(page: import('@playwright/test').Page, drawId: string) {
  await page.route('**/api/v1/admin/financials/breakdown**', route =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        status: 'SUCCESS',
        data: {
          from: '2026-08-12',
          to: '2026-08-12',
          summary: {
            ticketsSold: 1,
            grossSales: 900,
            winningsCalculated: 0,
            payoutsPaid: 0,
            sellerCommission: 90,
            buyerCharges: 0,
            sellerCharges: 0,
            tenantCharges: 0,
            waivedCharges: 0,
            promotionLines: 0,
            promotionPricedLines: 0,
            promotionPayoutBase: 0,
            netRevenueEstimated: 810,
          },
          drawRows: [
            {
              drawId,
              drawChannelCode: 'HT_TX_1000',
              drawChannelLabel: 'Texas · 1000',
              slotKey: 'HT_TX_1000',
              businessDate: '2026-08-12',
              ticketsSold: 1,
              grossSales: 900,
              sellerCommission: 90,
              tenantNetEstimated: 810,
              promotionLines: 0,
              promotionPayoutBase: 0,
            },
          ],
          sellerTerminalRows: [],
          sellerTerminalDrawRows: [
            {
              sellerTerminalId: 'terminal-1',
              terminalCode: 'POS-001',
              displayName: 'POS test',
              drawId,
              ticketsSold: 1,
              grossSales: 900,
              sellerCommission: 90,
              promotionLines: 0,
              promotionPayoutBase: 0,
            },
          ],
        },
        notices: [],
      }),
    }),
  );
}

test.describe('Draw detail — Numéros à risque widget', () => {
  test.beforeEach(async ({ loginPage, apiStub }) => {
    if (!creds) {
      test.skip(true, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');
      return;
    }
    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await loginPage.login(creds);
  });

  test('section visible with colored chips when exposureAlerts is non-empty', async ({ page }) => {
    await stubDraw(page, DRAW_ID_WITH_ALERTS);
    await stubStaticLimits(page);
    await stubActivity(page, DRAW_ID_WITH_ALERTS);
    await page.route(`**/api/v1/admin/draws/${DRAW_ID_WITH_ALERTS}/overview`, route =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: overviewBody(
          DRAW_ID_WITH_ALERTS,
          [
            {
              betType: 'MATCH_1_2D',
              selectionKey: '12',
              stakeTotal: 900,
              salesCount: 9,
              maxStakeExposureLimit: 1000,
              stakeRatio: 0.9,
            },
          ],
          [{ ruleKey: 'MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW', resolvedScope: 'DRAW_CHANNEL' }],
        ),
      }),
    );

    await page.goto(`/app/admin/draws/${DRAW_ID_WITH_ALERTS}`);

    // "Numéros à risque" section must be visible
    const section = page.locator(
      '.draw-detail-activity__exposure, [data-testid="exposure-section"]',
    );
    await expect(section).toBeVisible({ timeout: 10_000 });

    // Must have at least one chip for the ratio
    const chip = page.locator(
      '.draw-detail-activity__exposure-chip, [data-testid="exposure-chip"]',
    );
    await expect(chip.first()).toBeVisible();

    // ratio 0.9 ≥ 0.8 → must carry the "is-red" class
    await expect(chip.first()).toHaveClass(/is-red/);

    await expect(page.getByText(/règ statik/i)).toBeVisible();
    await expect(page.getByText(/nimewo bloke\s*:\s*12,\s*45/i)).toBeVisible();
  });

  test('section absent when exposureAlerts is empty', async ({ page }) => {
    await stubDraw(page, DRAW_ID_EMPTY_ALERTS);
    await stubActivity(page, DRAW_ID_EMPTY_ALERTS);
    await page.route(`**/api/v1/admin/draws/${DRAW_ID_EMPTY_ALERTS}/overview`, route =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: overviewBody(
          DRAW_ID_EMPTY_ALERTS,
          [],
          [{ ruleKey: 'MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW', resolvedScope: 'DRAW_CHANNEL' }],
        ),
      }),
    );

    await page.goto(`/app/admin/draws/${DRAW_ID_EMPTY_ALERTS}`);
    await expect(page.getByRole('heading', { name: /activité du tirage/i })).toBeVisible();

    await expect(
      page.locator('.draw-detail-activity__exposure, [data-testid="exposure-section"]'),
    ).toHaveCount(0);
  });

  test('section absent when effectiveLimits has no exposure rule (limitConfigured: false)', async ({
    page,
  }) => {
    await stubDraw(page, DRAW_ID_NO_LIMIT);
    await stubActivity(page, DRAW_ID_NO_LIMIT);
    await page.route(`**/api/v1/admin/draws/${DRAW_ID_NO_LIMIT}/overview`, route =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        // exposureAlerts present but no MAX_STAKE_EXPOSURE rule → limitConfigured=false
        body: overviewBody(
          DRAW_ID_NO_LIMIT,
          [
            {
              betType: 'MATCH_1_2D',
              selectionKey: '34',
              stakeTotal: 500,
              salesCount: 5,
              maxStakeExposureLimit: null,
              stakeRatio: null,
            },
          ],
          [],
        ),
      }),
    );

    await page.goto(`/app/admin/draws/${DRAW_ID_NO_LIMIT}`);
    await expect(page.getByRole('heading', { name: /activité du tirage/i })).toBeVisible();

    await expect(
      page.locator('.draw-detail-activity__exposure, [data-testid="exposure-section"]'),
    ).toHaveCount(0);
  });
});
