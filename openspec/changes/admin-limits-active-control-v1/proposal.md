# admin-limits-active-control-v1

## Status

`proposal` — 2026-08-14

## Context

The first limits simplification moved day-to-day configuration closer to its natural context:

- tenant defaults;
- draw channels;
- seller terminals;
- quick number blocking.

That still leaves the central **Limit** page too abstract for non-technical tenant admins. Today it mostly exposes scope cards, rule shortcuts, and global summaries. An admin cannot quickly answer:

1. Which limits are active right now?
2. Are they global, draw-specific, or terminal-specific?
3. Can I remove or pause one without hunting through several pages?
4. How do I quickly block a number for today's draw?

The most common operational action is expected to be: **block one number for one draw channel, usually for today**. The UI should make that fast without asking the admin to understand rule keys, scope scores, or internal assignment models.

## Why

- The current Limit page is not enough as a control surface for active restrictions.
- Active limits are spread across tenant, draw channel, and seller terminal scopes.
- Admins need a simple list of what is currently enforced, with obvious actions to edit, disable, or delete.
- Quick actions should optimize common flows instead of sending admins into generic technical forms.
- The frontend should not reconstruct target labels or active assignments from several unrelated endpoints.

## What

### 1. Make `/app/admin/limits` the active limits control page

Show:

- quick actions at the top;
- active limits grouped by business intent;
- one clear row/card per active limit assignment;
- contextual actions: edit, disable, delete.

Business groups:

- **Blocked numbers** — numbers that cannot be sold;
- **Number stake caps** — max exposure/stake per number and draw;
- **Ticket limits** — ticket size, max stake per line, max stake per ticket;
- **Seller limits** — limits scoped to a seller terminal or seller/agent;
- **Advanced** — supported rules that do not fit the common admin language yet.

### 2. Add a backend read model for active limits

Extend the tenant admin policies overview with an active-limits list that includes:

- assignment id;
- rule key;
- business group;
- target type;
- target id;
- target label;
- enabled state;
- duration summary fields;
- params;
- supported actions.

The backend resolves labels using stable public APIs or query buses from the owning modules. The `features/tenantadmin/policies` layer must not query repositories or SQL tables from other modules.

### 3. Optimize quick actions

The main quick actions are:

- block a number;
- cap stake on a number;
- add ticket limit;
- add seller/terminal limit.

For **Block number**:

- default scope is draw channel;
- default duration is today;
- channel selector prioritizes active/open sellable draw channels;
- tenant/global scope remains available but secondary.

### 4. Keep contextual limit entry points

Limits remain accessible from:

- dashboard quick actions;
- main Limit navigation link;
- tenant settings/config;
- draw detail;
- draw channel detail/config;
- seller terminal detail/config.

The central page is for visibility and operations. The contextual pages stay the natural places to configure limits for a specific owner.

### 5. Show effective limits in contextual detail pages

When an admin opens a draw, draw channel, or seller terminal detail page, show the limits that currently affect that context before the full configuration block.

For a draw this includes:

- limits inherited from its draw channel;
- inherited tenant/global limits.

For a draw channel this includes limits assigned directly to the draw channel and inherited tenant/global limits.

The contextual detail page does not replace the central Limit page. It answers: **why would a sale be blocked or warned here?**

## Impact

### Backend

- `tchalanet-server/tchalanet-features/src/main/java/com/tchalanet/server/features/tenantadmin/policies`
- possible read-only additions in `core/limitpolicy` public query/read model if current APIs cannot list all relevant assignments.
- possible label lookups via draw channel and seller terminal stable APIs/query buses.

### Web

- `tchalanet-web/apps/admin-portal/src/app/features/limits/pages/overview`
- `tchalanet-web/apps/admin-portal/src/app/features/limits/components/block-number-quick-dialog`
- `tchalanet-web/apps/admin-portal/src/app/features/limits/data-access`
- `tchalanet-web/libs/shared-assets/public/assets/i18n/{ht,fr,en}/feature-admin.json`
- admin e2e smoke coverage in `tchalanet-web/apps/web-e2e`

## Non-goals

- Do not change limit evaluation semantics.
- Do not expose internal scope scores.
- Do not move draw channel, game, or seller terminal ownership into the limits feature.
- Do not remove existing advanced routes in this slice.
- Do not add mobile/POS app behavior in this slice.
- Do not invent unsupported rule behavior or merge distinct backend rules only for UI convenience.
