import { expect, test } from '../support/fixtures';
import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';

/**
 * Draw detail — "Bloke nimero" button — Slice 6 regression guard.
 *
 * - Draw OPEN  → button visible in the header action bar
 * - Clicking   → dialog opens with channel pre-selected (no draw picker visible)
 * - Draw CLOSED → button absent
 *
 * Network interception stubs the draw overview endpoint so tests are deterministic.
 */

const creds = credsFor('admin');

const OPEN_DRAW_ID = 'aaaaaaaa-0000-0000-0000-000000000001';
const CLOSED_DRAW_ID = 'aaaaaaaa-0000-0000-0000-000000000002';
const DRAW_CHANNEL_ID = 'cccccccc-0000-0000-0000-000000000001';

function stubDrawOverview(page: import('@playwright/test').Page, drawId: string, salesStatus: 'OPEN' | 'CLOSED') {
  return page.route(`**/api/v1/admin/draws/${drawId}/overview`, route =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        status: 'SUCCESS',
        data: {
          draw: {
            drawId,
            drawChannelId: DRAW_CHANNEL_ID,
            drawChannelCode: 'TEST-CHANNEL',
            drawChannelLabel: 'Canal test',
            drawDate: '2026-08-12',
            status: salesStatus,
            scheduledAt: '2026-08-12T14:00:00Z',
            openedAt: salesStatus === 'OPEN' ? '2026-08-12T08:00:00Z' : null,
            closedAt: salesStatus === 'CLOSED' ? '2026-08-12T12:00:00Z' : null,
            result: null,
          },
          topSelections: [],
          effectiveLimits: [],
          exposureAlerts: [],
        },
        notices: [],
      }),
    }),
  );
}

test.describe('Draw detail — Bloke nimero button', () => {
  test.beforeEach(async ({ loginPage, apiStub }) => {
    if (!creds) {
      test.skip(true, 'TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');
      return;
    }
    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await loginPage.login(creds);
  });

  test('draw OPEN — "Bloke nimero" button is visible in the header', async ({ page }) => {
    await stubDrawOverview(page, OPEN_DRAW_ID, 'OPEN');
    await page.goto(`/app/admin/draws/${OPEN_DRAW_ID}`);

    const btn = page.locator('button, [role="button"]', { hasText: /bloke nimero/i });
    await expect(btn).toBeVisible({ timeout: 10_000 });
  });

  test('draw OPEN — clicking Bloke nimero opens a dialog without a draw picker', async ({
    page,
  }) => {
    await stubDrawOverview(page, OPEN_DRAW_ID, 'OPEN');
    await page.goto(`/app/admin/draws/${OPEN_DRAW_ID}`);

    const btn = page.locator('button, [role="button"]', { hasText: /bloke nimero/i });
    await btn.click();

    const dialog = page.locator('mat-dialog-container');
    await expect(dialog).toBeVisible({ timeout: 5_000 });

    // No draw picker inside the dialog — channel is pre-filled from context
    await expect(dialog.locator('[data-testid="draw-picker"], mat-select[aria-label*="tiraj"]')).toHaveCount(0);
  });

  test('draw CLOSED — "Bloke nimero" button is absent', async ({ page }) => {
    await stubDrawOverview(page, CLOSED_DRAW_ID, 'CLOSED');
    await page.goto(`/app/admin/draws/${CLOSED_DRAW_ID}`);

    // Wait for the page to load before asserting absence
    await page.waitForLoadState('networkidle');
    await expect(
      page.locator('button, [role="button"]', { hasText: /bloke nimero/i }),
    ).toHaveCount(0);
  });
});
