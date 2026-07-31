import { expect, test } from '../support/fixtures';
import { tenantAdminPrivateBootstrap } from '../support/api-stub';
import { credsFor } from '../support/env';
import type { Locator } from '@playwright/test';

/**
 * Split label/chevron on the desktop sidebar (`tch-sidebar-nav`, ≥840px): a group with its own
 * destination navigates on the label, the chevron only expands/collapses. Unit tests
 * (`private-shell-drawer.spec.ts`) cover the logic against a small fixture model; these prove it
 * holds in a real browser against the shell's actual static fallback navigation
 * (`TENANT_ADMIN_NAVIGATION` — the stub sets `navigationDrawer: null`).
 *
 * Selectors target `href`, not translated label text: the e2e locale isn't pinned, and a route is
 * a stable, language-independent handle.
 */

const creds = credsFor('admin');

function childrenOf(group: Locator): Locator {
  return group.locator('xpath=following-sibling::div[contains(@class, "sidebar__children")][1]');
}

test.describe('Admin console sidenav — desktop (≥840px)', () => {
  test.skip(!creds, 'requires TCH_E2E_ADMIN_EMAIL / TCH_E2E_ADMIN_PASSWORD');

  test.beforeEach(async ({ loginPage, apiStub }) => {
    // `navigationDrawer: null` in this bootstrap: the shell falls back to the static model, so
    // these tests exercise the fallback path too.
    await apiStub.privateBootstrap(tenantAdminPrivateBootstrap);
    await loginPage.login(creds!);
  });

  test('clicking a group label navigates directly, without expanding first', async ({ page }) => {
    await page.goto('/app/admin');

    const link = page.locator(
      '[data-testid="sidebar-group-link"][href="/app/admin/seller-terminals"]',
    );
    await expect(link).toBeVisible();
    await link.click();

    await expect(page).toHaveURL(/\/app\/admin\/seller-terminals$/);
  });

  test('clicking the chevron expands children without navigating', async ({ page }) => {
    await page.goto('/app/admin');

    const group = page.locator('[data-testid="sidebar-group"]').filter({
      has: page.locator('[data-testid="sidebar-group-link"][href="/app/admin/seller-terminals"]'),
    });
    const expand = group.getByTestId('sidebar-group-expand');
    await expect(expand).toHaveAttribute('aria-expanded', 'false');

    await expand.click();

    // Still on the dashboard — the chevron never navigates.
    await expect(page).toHaveURL(/\/app\/admin$/);
    await expect(expand).toHaveAttribute('aria-expanded', 'true');
    await expect(childrenOf(group).getByTestId('sidebar-child').first()).toBeVisible();
  });

  test('collapses again on a second chevron click, still without navigating', async ({ page }) => {
    await page.goto('/app/admin');

    const group = page.locator('[data-testid="sidebar-group"]').filter({
      has: page.locator('[data-testid="sidebar-group-link"][href="/app/admin/seller-terminals"]'),
    });
    const expand = group.getByTestId('sidebar-group-expand');

    await expand.click();
    await expect(expand).toHaveAttribute('aria-expanded', 'true');

    await expand.click();

    await expect(expand).toHaveAttribute('aria-expanded', 'false');
    await expect(childrenOf(group).getByTestId('sidebar-child')).toHaveCount(0);
    await expect(page).toHaveURL(/\/app\/admin$/);
  });

  test('a group and its active child never share the same solid highlight', async ({ page }) => {
    // "reports" points at the same route as its "reports-daily" child — exactly the case that
    // used to stack two solid highlights on top of each other.
    await page.goto('/app/admin/reports/daily');

    const group = page.locator('[data-testid="sidebar-group"]').filter({
      has: page.locator('[data-testid="sidebar-group-link"][href="/app/admin/reports/daily"]'),
    });

    await expect(group).toHaveClass(/is-active-group/);
    await expect(group.getByTestId('sidebar-group-link')).not.toHaveClass(/(^|\s)is-active(\s|$)/);

    // Exactly one child carries the solid highlight, and it's the one whose route is active.
    const activeChildren = childrenOf(group).locator('[data-testid="sidebar-child"].is-active');
    await expect(activeChildren).toHaveCount(1);
    await expect(activeChildren).toHaveAttribute('href', '/app/admin/reports/daily');
  });

  test('reloading a nested route keeps the right group expanded and child active', async ({
    page,
  }) => {
    await page.goto('/app/admin/limits/number');

    const group = page.locator('[data-testid="sidebar-group"]').filter({
      has: page.locator('[data-testid="sidebar-group-link"][href="/app/admin/limits"]'),
    });
    await expect(
      childrenOf(group).locator('[data-testid="sidebar-child"][href="/app/admin/limits/number"]'),
    ).toHaveClass(/is-active/);
  });

  test('reloading directly on tickets/sell keeps the ticket group expanded and marks it active', async ({
    page,
  }) => {
    await page.goto('/app/admin/tickets/sell');

    const group = page.locator('[data-testid="sidebar-group"]').filter({
      has: page.locator('[data-testid="sidebar-group-link"][href="/app/admin/tickets"]'),
    });
    await expect(
      childrenOf(group).locator('[data-testid="sidebar-child"][href="/app/admin/tickets/sell"]'),
    ).toHaveClass(/is-active/);
  });
});
