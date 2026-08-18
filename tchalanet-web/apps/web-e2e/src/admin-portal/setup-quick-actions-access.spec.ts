import type { Page } from '@playwright/test';

import {
  tenantAdminPrivateBootstrap,
  tenantAdminReadonlyPrivateBootstrap,
} from '../support/api-stub';
import { credsFor } from '../support/env';
import { expect, test } from '../support/fixtures';

const creds = credsFor('admin');
const configureGame = /Modifier le jeu|Modifye jwèt la|Edit game|Configurer|Konfigire|Configure/;
const configureChannel = /Configurer|Konfigire|Configure/;
const confirmDisable = /Désactiver|Dezaktive|Disable/;

test.describe('Admin setup quick actions access', () => {
  test.beforeEach(async ({ apiStub }) => {
    test.skip(!creds, 'requires TCH_E2E_ADMIN_EMAIL / TCH_E2E_ADMIN_PASSWORD');
    test.skip(process.env['WEB_E2E_API'] === '1', 'Setup quick action access uses deterministic REST stubs.');

    await apiStub.adminBusiness();
    await apiStub.adminDrawChannels();
    await apiStub.adminDrawSalesMatrix();
  });

  test('allows a capable admin to use the three switches and two configure actions', async ({
    page,
    loginPage,
    apiStub,
  }) => {
    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await loginPage.login(requiredAdminCreds());

    await page.goto('/app/admin/draw-channels');
    const drawChannelCard = page.locator('.dc-channel-card').filter({ hasText: 'Texas · 1000' }).first();
    const drawChannelSwitch = drawChannelCard.locator('.dc-channel-card__sale-toggle');
    await expect(drawChannelSwitch).toBeEnabled();
    await drawChannelCard.getByRole('button', { name: configureChannel }).click();
    await expect(page.getByRole('dialog').getByRole('heading', { name: /Vente|Vant|Sales/ })).toBeVisible();
    await page.keyboard.press('Escape');

    await drawChannelSwitch.click();
    await expect(page.getByRole('dialog')).toBeVisible();
    await Promise.all([
      page.waitForRequest(
        request =>
          request.method() === 'PATCH' &&
          request.url().includes('/tenant/draw-channels/channel-ht-1000/active'),
      ),
      page.getByRole('dialog').getByRole('button', { name: confirmDisable }).click(),
    ]);
    await expect(drawChannelSwitch).toHaveAttribute('aria-pressed', 'false');

    await page.goto('/app/admin/games');
    const gameCard = page.locator('.gp-game-card').filter({ hasText: /Bolèt|Bolet/ }).first();
    const gameSwitch = gameCard.locator('.gp-game-card__sale-toggle');
    await expect(gameSwitch).toBeEnabled();
    await gameCard.getByRole('button', { name: configureGame }).click();
    await expect(page.getByRole('heading', { name: /Bolèt|Bolet/ })).toBeVisible();
    await page.goBack();

    await gameSwitch.click();
    await expect(page.getByRole('dialog')).toBeVisible();
    await Promise.all([
      page.waitForRequest(
        request =>
          request.method() === 'POST' &&
          request.url().includes('/admin/games/HT_BOLET/disable'),
      ),
      page.getByRole('dialog').getByRole('button', { name: confirmDisable }).click(),
    ]);
    await expect(gameSwitch).toHaveAttribute('aria-pressed', 'false');

    await page.goto('/app/admin/games/channel-matrix');
    const matrixSearch = page.getByPlaceholder(/Chèche tiraj oswa jwèt|Search draws or games|Chercher un tirage ou un jeu/);
    await expect(matrixSearch).toBeVisible();
    await matrixSearch.fill('maryaj gratis');
    await expandFirstMatrixSlot(page);
    await expect(page.locator('tch-draw-sales-matrix-game-card').filter({ hasText: /Maryaj gratis/ })).toBeVisible();
    await expect(page.locator('tch-draw-sales-matrix-game-card').filter({ hasText: /Bolèt|Bolet/ })).toHaveCount(0);
    await matrixSearch.fill('');
    await expandFirstMatrixSlot(page);
    const matrixGame = page.locator('tch-draw-sales-matrix-game-card').filter({ hasText: /Bolèt|Bolet/ }).first();
    const matrixSwitch = matrixGame.locator('.matrix-game__switch');
    await expect(matrixSwitch).toBeEnabled();
    await expect(matrixGame).not.toContainText(/\bON\b|Pare/);
    await Promise.all([
      page.waitForRequest(
        request =>
          request.method() === 'PATCH' &&
          request.url().includes('/admin/draw-channels/channel-ny-mid/tenant-games/game-bolet'),
      ),
      matrixSwitch.click(),
    ]);
    await expect(matrixSwitch).toHaveAttribute('aria-pressed', 'false');
    await expect(page.getByText(/Aksyon fèt|Action completed|Action effectuée/)).toHaveCount(0);
  });

  test('disables the same quick actions for an admin without setup mutation permissions', async ({
    page,
    loginPage,
    apiStub,
  }) => {
    await apiStub.privateBootstrap(tenantAdminReadonlyPrivateBootstrap);
    await loginPage.login(requiredAdminCreds());

    await page.goto('/app/admin/draw-channels');
    const drawChannelCard = page.locator('.dc-channel-card').filter({ hasText: 'Texas · 1000' }).first();
    await expect(drawChannelCard.locator('.dc-channel-card__sale-toggle')).toBeDisabled();
    await expect(drawChannelCard.getByRole('button', { name: configureChannel })).toBeDisabled();

    await page.goto('/app/admin/games');
    const gameCard = page.locator('.gp-game-card').filter({ hasText: /Bolèt|Bolet/ }).first();
    await expect(gameCard.locator('.gp-game-card__sale-toggle')).toBeDisabled();
    await expect(gameCard.getByRole('button', { name: configureGame })).toBeDisabled();

    await page.goto('/app/admin/games/channel-matrix');
    await expandFirstMatrixSlot(page);
    const matrixGame = page.locator('tch-draw-sales-matrix-game-card').filter({ hasText: /Bolèt|Bolet/ }).first();
    await expect(matrixGame.locator('.matrix-game__switch')).toBeDisabled();
  });
});

function requiredAdminCreds() {
  if (!creds) throw new Error('TCH_E2E_ADMIN_EMAIL/PASSWORD not configured');
  return creds;
}

async function expandFirstMatrixSlot(page: Page): Promise<void> {
  const header = page.locator('mat-expansion-panel-header').first();
  if ((await header.getAttribute('aria-expanded')) !== 'true') {
    await header.click();
  }
}
