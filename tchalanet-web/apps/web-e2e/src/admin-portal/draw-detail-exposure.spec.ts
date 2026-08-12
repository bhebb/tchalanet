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

function overviewBody(
  drawId: string,
  exposureAlerts: unknown[],
  effectiveLimits: unknown[],
) {
  return JSON.stringify({
    status: 'SUCCESS',
    data: {
      draw: {
        drawId,
        drawChannelId: 'cccccccc-0000-0000-0000-000000000001',
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
        { rank: 1, displaySelection: '12', gameCode: 'HT_BOLET', betType: 'MATCH_1_2D', count: 9, totalStakeCents: 90000 },
      ],
      effectiveLimits,
      exposureAlerts,
    },
    notices: [],
  });
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
    const section = page.locator('.draw-detail-activity__exposure, [data-testid="exposure-section"]');
    await expect(section).toBeVisible({ timeout: 10_000 });

    // Must have at least one chip for the ratio
    const chip = page.locator('.draw-detail-activity__exposure-chip, [data-testid="exposure-chip"]');
    await expect(chip.first()).toBeVisible();

    // ratio 0.9 ≥ 0.8 → must carry the "is-red" class
    await expect(chip.first()).toHaveClass(/is-red/);
  });

  test('section absent when exposureAlerts is empty', async ({ page }) => {
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
    await page.waitForLoadState('networkidle');

    await expect(
      page.locator('.draw-detail-activity__exposure, [data-testid="exposure-section"]'),
    ).toHaveCount(0);
  });

  test('section absent when effectiveLimits has no exposure rule (limitConfigured: false)', async ({
    page,
  }) => {
    await page.route(`**/api/v1/admin/draws/${DRAW_ID_NO_LIMIT}/overview`, route =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        // exposureAlerts present but no MAX_STAKE_EXPOSURE rule → limitConfigured=false
        body: overviewBody(DRAW_ID_NO_LIMIT, [
          {
            betType: 'MATCH_1_2D',
            selectionKey: '34',
            stakeTotal: 500,
            salesCount: 5,
            maxStakeExposureLimit: null,
            stakeRatio: null,
          },
        ], []),
      }),
    );

    await page.goto(`/app/admin/draws/${DRAW_ID_NO_LIMIT}`);
    await page.waitForLoadState('networkidle');

    await expect(
      page.locator('.draw-detail-activity__exposure, [data-testid="exposure-section"]'),
    ).toHaveCount(0);
  });
});
