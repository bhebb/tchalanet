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
 * The page fetches GET /admin/draws/:id (via AdminGeneratedDrawsApiService.getDrawById).
 * Shape: DrawView { id, channel, slot, drawDate, scheduledAt, cutoffAt, status, active, lastResult }.
 */

const creds = credsFor('admin');

const OPEN_DRAW_ID = 'aaaaaaaa-0000-0000-0000-000000000001';
const CLOSED_DRAW_ID = 'aaaaaaaa-0000-0000-0000-000000000002';
const DRAW_CHANNEL_ID = 'cccccccc-0000-0000-0000-000000000001';

function stubDraw(
  page: import('@playwright/test').Page,
  drawId: string,
  status: 'OPEN' | 'CLOSED',
) {
  // Intercept the draw detail endpoint — exact path, no sub-paths (top-selections etc.)
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
          channel: { id: DRAW_CHANNEL_ID, code: 'TEST-MIDDAY', name: 'Test Midday' },
          slot: {
            id: 'slot-1',
            key: 'MIDDAY',
            label: 'Midday',
            timezone: 'America/Port-au-Prince',
            drawTime: '12:00',
          },
          drawDate: '2026-08-12',
          scheduledAt: '2026-08-12T17:00:00Z',
          cutoffAt: '2026-08-12T16:45:00Z',
          status,
          active: status === 'OPEN',
          lastResult: null,
        },
        notices: [],
      }),
    });
  });
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
    await stubDraw(page, OPEN_DRAW_ID, 'OPEN');
    await page.goto(`/app/admin/draws/${OPEN_DRAW_ID}`);

    const btn = page.locator('button, [role="button"]', { hasText: /bloke nimero/i });
    await expect(btn).toBeVisible({ timeout: 10_000 });
  });

  test('draw OPEN — clicking Bloke nimero opens a dialog without a draw picker', async ({
    page,
  }) => {
    await stubDraw(page, OPEN_DRAW_ID, 'OPEN');
    await page.goto(`/app/admin/draws/${OPEN_DRAW_ID}`);

    const btn = page.locator('button, [role="button"]', { hasText: /bloke nimero/i });
    await btn.click();

    const dialog = page.locator('mat-dialog-container');
    await expect(dialog).toBeVisible({ timeout: 5_000 });

    // No draw picker inside the dialog — channel is pre-filled from context
    await expect(
      dialog.locator('[data-testid="draw-picker"], mat-select[aria-label*="tiraj"]'),
    ).toHaveCount(0);
  });

  test('draw CLOSED — "Bloke nimero" button is absent', async ({ page }) => {
    await stubDraw(page, CLOSED_DRAW_ID, 'CLOSED');
    await page.goto(`/app/admin/draws/${CLOSED_DRAW_ID}`);

    await expect(page.getByRole('heading', { name: /test midday/i })).toBeVisible();
    await expect(page.locator('button, [role="button"]', { hasText: /bloke nimero/i })).toHaveCount(
      0,
    );
  });
});
