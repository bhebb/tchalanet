# Error Management Convention

> Status: ACTIVE v1  
> Scope: backend error contract consumption, web normalization, shell/page/section/field ownership

## Rule

Every user-facing failure must have exactly one UI owner:

```text
shell -> page -> section -> field
```

The owner is determined from the normalized `WebAppError` fields:

```ts
surface: 'shell' | 'page' | 'section' | 'field'
placement: 'top' | 'inline' | 'summary'
target?: string
field?: string
```

The web app consumes the backend contract. It does not redefine it:

- `2xx` responses use `ApiResponse<T>`;
- `4xx/5xx` responses use `ProblemDetail`;
- partial BFF failures use `ApiResponse.notices` and, when useful, `services`.

## Ownership

### Shell top

Use shell feedback only for failures that no page, section, form, or field owns.

Examples:

- session/auth state problem;
- network/global service disruption;
- action from shell navigation/header;
- API failure not consumed locally;
- runtime frontend error outside a specific page/section.

Shell feedback may survive route changes inside the same shell, but it must stay bounded,
deduplicated, and scoped to public/private shell context.

### Page top

Use page-level errors when the routed page owns the failure.

Examples:

- the page's required data cannot load;
- access to the page is denied;
- the page cannot render meaningful content.

Page errors must clear with the page lifecycle and must suppress duplicate shell feedback.

### Section top

Use section errors when a block/widget/card owns the failure and the rest of the page can continue.

Examples:

- dashboard commissions unavailable;
- readiness draw channel check unavailable;
- one PageModel widget provider fails.

The backend should send a notice like:

```json
{
  "code": "dashboard.commissions.unavailable",
  "message": "Commissions are temporarily unavailable.",
  "domain": "dashboard",
  "severity": "WARN",
  "meta": {
    "surface": "section",
    "placement": "top",
    "target": "dashboard.commissions",
    "source": "commissions",
    "service": "commission-service",
    "operation": "loadWidget",
    "traceId": "..."
  }
}
```

`target` must be a stable UI target. For PageModel dashboards, prefer the widget id.

For admin cards, prefer the host directive on the section card instead of adding a separate error
element inside each page:

```html
<tch-admin-section-card
  title="Commissions"
  tchSectionErrorTarget="dashboard.commissions"
  [tchSectionErrors]="sectionErrors()"
>
  ...
</tch-admin-section-card>
```

The page/store still owns `sectionErrors`. The directive only selects the error matching the card
target and lets `tch-admin-section-card` render the standard `tch-section-error` area under the
card header.

Current migrated example:

```text
apps/tch-portal/src/app/features/private/admin/business-profile/pages/overview
page target:
- admin.businessProfile.overview
section target:
- admin.businessProfile.commercial
form targets:
- admin.businessProfile.identity
- admin.businessProfile.region
- admin.businessProfile.address
```

The business profile overview load is blocking and renders as a page error. The commission overview
is non-blocking; if it fails, the page stays usable and only the commercial settings card shows the
warning. Identity, region, commission, and address submit failures stay inside the open form and
must suppress shell feedback.

Readiness/health cards follow the same rule. They must render local preview/check failures with
`tch-section-error`, not a page panel or shell banner.

Current migrated example:

```text
apps/tch-portal/src/app/features/private/platform/tenants/pages/onboarding
target: platform.tenantProvisioning.readiness.preview
```

The tenant provisioning page treats preview/readiness calculation as non-blocking. If preview fails,
the tenant creation form remains usable and only the readiness health card shows the warning.

Dashboard/overview pages that aggregate multiple slices should keep each card as the UI owner for
its own failure.

Current migrated example:

```text
apps/tch-portal/src/app/features/private/platform/pages/ops/platform-ops.page.ts
targets:
- platform.ops.overview.results
- platform.ops.overview.draws
- platform.ops.overview.jobs
- platform.ops.overview.cache
```

The operations overview keeps rendering available metrics. Failed slices display `--` for missing
values and a section warning on the owning card. These local API calls must set
`suppressShellFeedback` to avoid duplicate shell banners.

Setup overview cards may also own non-blocking `ApiResponse.notices` returned by
`GET /admin/overview`.

Current migrated example:

```text
apps/tch-portal/src/app/features/private/admin/setup/pages/complete-config
targets:
- admin.setup.identity
- admin.setup.address
- admin.setup.games_pricing
- admin.setup.draws
- admin.setup.generatedDraws
- admin.setup.theme
- admin.setup.promotions
- admin.setup.sellerTerminal
```

The Setup page still uses a page error when `/admin/overview` cannot load at all. Section notices
only cover partial, non-blocking degradation for the owning card.

Games/pricing action failures are owned by the game card that triggered the action:

```text
apps/tch-portal/src/app/features/private/admin/games-pricing/pages/overview
target format: admin.setup.games_pricing.<gameCode>
```

Draw channel provider source failures are owned by the provider card:

```text
apps/tch-portal/src/app/features/private/admin/draw-channels/pages/overview
target format: admin.setup.draw_channels.<providerCode>
```

The draw-channels overview still owns blocking load failures at page level. Until provider
configuration has a backend endpoint, the configure action must render as a page-local information
notice instead of a snackbar or shell message.

Draw sales matrix action failures are owned by the affected channel/game tile:

```text
apps/tch-portal/src/app/features/private/admin/draw-sales-matrix/pages
target format: admin.setup.draw_sales_matrix.<drawChannelId>.<tenantGameId>
```

Business day action failures are owned by the calendar/exceptions section:

```text
apps/tch-portal/src/app/features/private/admin/pages/business-days
targets:
- admin.businessDays.add
- admin.businessDays.delete.<businessDayId>
```

Limit rule action failures are owned by either the rule list section or the edit dialog:

```text
apps/tch-portal/src/app/features/private/admin/pages/limits
targets:
- admin.limits.delete.<ruleKey>
- admin.limits.assignment.<ruleKey>
```

Default pricing grid failures are owned by the pricing page:

```text
apps/tch-portal/src/app/features/private/admin/pages/pricing
target:
- admin.controls.pricing
```

The page suppresses shell feedback because a failed default-odds load blocks the table and renders a
single page-level `tch-error-panel`.

Draw list and detail failures are owned by the affected tab/detail section:

```text
apps/tch-portal/src/app/features/private/admin/pages/draws
targets:
- admin.draws.today
- admin.draws.upcoming
- admin.draws.all
- admin.draws.detail.<drawId>
- admin.draws.detail.<drawId>.manualResult
```

Seller-terminal creation failures are owned by the creation page/dialog, not by the shell:

```text
apps/tch-portal/src/app/features/private/admin/seller-terminals/pages/new
apps/tch-portal/src/app/features/private/admin/seller-terminals/pages/list/dialogs/create-seller-terminal.dialog
page target:
- admin.sellerTerminal.create
```

The API call must pass `suppressShellFeedback: true`. If the backend returns field violations, the
page/dialog maps them to controls. If the backend returns a blocking `ProblemDetail` without usable
field violations, the page/dialog renders one local page error at the top of the form.

Seller-terminal list failures follow the same local ownership rule:

```text
apps/tch-portal/src/app/features/private/admin/seller-terminals/pages/list
page targets:
- admin.sellerTerminal.list
- admin.sellerTerminal.unblock
- admin.sellerTerminal.disable
dialog targets:
- admin.sellerTerminal.block
- admin.sellerTerminal.resetPin
- admin.sellerTerminal.limits.rules
- admin.sellerTerminal.limits.assignments
- admin.sellerTerminal.limits.save
- admin.sellerTerminal.limits.delete
- admin.sellerTerminal.limits.reload
```

List load failures render as the page error. Row action failures render once in the list surface or
inside the owning dialog. These API calls must suppress shell feedback. Successful create/unblock
and disable actions close or reload the owning surface without creating a snackbar.

Seller-terminal POS failures are split by runtime block:

```text
apps/tch-portal/src/app/features/private/admin/seller-terminals/pages/pos
page target:
- admin.sellerTerminal.pos.terminal
section targets:
- admin.sellerTerminal.pos.draws
- admin.sellerTerminal.pos.games
- admin.sellerTerminal.pos.activity
local sale target:
- admin.sellerTerminal.pos.sale
```

The terminal lookup is blocking because the page cannot act without it. Draws, games, and activity
are section-owned so the POS surface stays visible when one optional slice degrades. Sale failures
stay next to the sale action and must not create shell banners.

Admin support sell uses the same local action ownership:

```text
apps/tch-portal/src/app/features/private/admin/pages/support/admin-sell-ticket.page
selector targets:
- admin.support.sell.terminals
- admin.support.sell.draws.today
- admin.support.sell.draws.upcoming
action targets:
- admin.support.sell.preview
- admin.support.sell.confirm
```

Selector load failures render above the form. Preview and sell failures render near the form actions.
Do not use snackbars for these failures, and do not expose backend raw titles/details.

Admin tickets list is page-owned:

```text
apps/tch-portal/src/app/features/private/admin/pages/support/admin-tickets.page
page target:
- admin.tickets.list
```

Ticket list load failures render as the page error and suppress shell feedback.

Admin payouts list/action failures are owned by the payouts page:

```text
apps/tch-portal/src/app/features/private/admin/pages/payouts
page target:
- admin.payouts.list
action targets:
- admin.payouts.block
- admin.payouts.unblock
- admin.payouts.cancel
- admin.payouts.reverse
```

List load failures render as the page error. Row action failures render once above the payouts
table. These calls suppress shell feedback and do not use snackbars.

Platform super-admin list/detail failures are owned by the current page:

```text
apps/tch-portal/src/app/features/private/platform/super-admins
page targets:
- platform.superAdmins.list
- platform.superAdmins.detail
action targets:
- platform.superAdmins.create
- platform.superAdmins.revoke
- platform.superAdmins.activate
- platform.superAdmins.suspend
- platform.superAdmins.archive
- platform.superAdmins.resetPassword
- platform.superAdmins.assignTenant
```

List/detail load failures render as page errors. Row/detail action failures render locally on the
same page and suppress shell feedback. Temporary-password and assignment success messages stay as
local informational notices, not snackbars.

Platform tenant-admin list/detail failures follow the same ownership model:

```text
apps/tch-portal/src/app/features/private/platform/tenant-admins
page targets:
- platform.tenantAdmins.list
- platform.tenantAdmins.detail
action targets:
- platform.tenantAdmins.activate
- platform.tenantAdmins.block
- platform.tenantAdmins.archive
- platform.tenantAdmins.resetPassword
```

List/detail load failures render as page errors. Action failures render locally on the current page
and suppress shell feedback. Temporary-password success feedback stays as a local informational
notice, not a snackbar.

Platform contact-request failures are owned by the contact-requests page:

```text
apps/tch-portal/src/app/features/private/platform/pages/contact-requests
page target:
- platform.contactRequests.list
detail target:
- platform.contactRequests.detail
action targets:
- platform.contactRequests.status
- platform.contactRequests.notes
```

List load failures render as the page error. Detail-load and detail-action failures render locally
inside the page/detail surface and suppress shell feedback. Status/notes success feedback stays as
a local informational notice, not a snackbar.

Platform public-news failures are owned by the news page:

```text
apps/tch-portal/src/app/features/private/platform/pages/news
page target:
- platform.news.list
action targets:
- platform.news.save
- platform.news.status
- platform.news.hide
- platform.news.refresh
```

List load failures render as the page error. Save/status/hide/RSS-refresh failures render locally
above the news table and suppress shell feedback. Success feedback stays as a local informational
notice, not a snackbar.

Admin notifications are page/composer/action owned:

```text
apps/tch-portal/src/app/features/private/admin/notifications
page target:
- admin.notifications.list
composer target:
- admin.notifications.create
action target:
- admin.notifications.action
```

Notification list failures render as the page error. Composer and row/bulk action failures render
inside the local notification page surface and suppress shell feedback. Success copy is local to the
same surface; do not use snackbars for notification failures.

Platform notifications follow the same page/composer/action ownership model:

```text
apps/tch-portal/src/app/features/private/platform/pages/notifications
page target:
- platform.notifications.list
composer target:
- platform.notifications.create
action targets:
- platform.notifications.markRead
- platform.notifications.archive
- platform.notifications.lifecycle
```

List failures render as the page error. Composer validation/API failures render inside the composer.
Row actions and lifecycle actions render above the notification table and suppress shell feedback.
Success feedback stays as a local informational notice, not a snackbar.

Platform support-tenant and ops-cache failures are page/action owned:

```text
apps/tch-portal/src/app/features/private/platform/pages/support-tenant
page target:
- platform.supportTenant.list

apps/tch-portal/src/app/features/private/platform/pages/ops/platform-ops-cache
page target:
- platform.ops.cache.list
action targets:
- platform.ops.cache.clear
- platform.ops.cache.clearAll
- platform.ops.cache.clearGroup
```

Support-tenant load failures render as the page error. Cache list failures render as the page
error. Cache clear failures render locally on the page or inside the dialog that owns the action,
and those calls suppress shell feedback. Cache dialogs close and let the page reload on success;
they do not show snackbars or promise support submission.

Platform communication operations are page/action owned:

```text
apps/tch-portal/src/app/features/private/platform/operations/pages/communication
page targets:
- platform.communication.list
- platform.communication.outbox.list
action targets:
- platform.communication.dispatch
- platform.communication.outbox.dispatch
- platform.communication.test.<channel>
- platform.communication.tests.<channel>
```

Message list/outbox load failures render as page errors. Dispatch and provider-test failures render
as local section feedback on the owning page and suppress shell feedback. Successful dispatch/test
results stay as local informational feedback, not snackbars.

Tenant admin support-access failures are owned by the access dialog:

```text
apps/tch-portal/src/app/features/private/platform/shared/start-tenant-admin-access-dialog
target:
- platform.tenantAdminAccess.start
```

The access dialog suppresses shell feedback and renders start-session failures locally. It must not
show raw backend titles or trace identifiers directly; support references remain part of the
normalized error model.

Platform ops draw-results failures are page/action owned:

```text
apps/tch-portal/src/app/features/private/platform/pages/ops/platform-ops-draw-results
page target:
- platform.ops.drawResults.list
action target:
- platform.ops.drawResults.confirm
```

Draw-results list failures render as a page error. Confirm failures and success copy render above
the results table and suppress shell feedback. Dialog-owned fetch/manual/override failures must
stay inside their dialogs.

Business days failures are page/action owned:

```text
apps/tch-portal/src/app/features/private/admin/pages/business-days
page target:
- admin.businessDays.list
action targets:
- admin.businessDays.add
- admin.businessDays.delete.<businessDayId>
```

Month load failures render as the page error. Add/delete failures and success copy render above the
calendar in the owning page surface. Do not use snackbars for business-days failures.

Draw sales matrix failures are page/game-card owned:

```text
apps/tch-portal/src/app/features/private/admin/draw-sales-matrix
page target:
- admin.setup.draw_sales_matrix
game action target:
- admin.setup.draw_sales_matrix.<drawChannelId>.<tenantGameId>
```

Matrix load failures render as the page error. Offer, enable/disable, and remove failures render on
the game card that owns the action. Success copy stays on the same game card and must not use a
snackbar.

Generated draws failures are page/action owned:

```text
apps/tch-portal/src/app/features/private/admin/draws/pages/overview/admin-generated-draws.page
page target:
- admin.generatedDraws.list
action targets:
- admin.generatedDraws.lifecycle.cancel
- admin.generatedDraws.lifecycle.lock
- admin.generatedDraws.lifecycle.unlock
- admin.generatedDraws.lifecycle.archive
```

Generated draw list failures render as the page error. Lifecycle action feedback renders locally in
the generated-draws page surface and suppresses shell feedback. Do not use snackbars for lifecycle
failures.

Draw results list failures are page-owned:

```text
apps/tch-portal/src/app/features/private/admin/pages/draw-results
page target:
- admin.drawResults.list
```

Draw results load/filter failures render as the page error and suppress shell feedback.

Setup settings failures are page/form owned:

```text
apps/tch-portal/src/app/features/private/admin/setup/pages/settings/admin-config.page
page target:
- admin.setup.config
form targets:
- admin.setup.locale
- admin.setup.receipt
```

Tenant config load failures render as the page error. Locale and receipt submit failures render in
their owning form; field violations should target the form controls listed in the field section.
These calls suppress shell feedback and do not use snackbars.

Setup runtime failures are page-owned:

```text
apps/tch-portal/src/app/features/private/admin/setup/pages/settings/admin-runtime.page
page target:
- admin.setup.runtime
```

Runtime load/reload failures render as the page error and suppress shell feedback. Reload success
copy stays in the runtime page surface and does not use a snackbar.

Limits admin failures are page/dialog/action owned:

```text
apps/tch-portal/src/app/features/private/admin/pages/limits
page targets:
- admin.limits.rules
- admin.limits.assignments
dialog target:
- admin.limits.assignment.<ruleKey>
action target:
- admin.limits.delete.<ruleKey>
```

Rules and assignment load failures render as the page error. Upsert failures render inside the
dialog. Delete failures and success copy render in the owning limits page surface. Do not use
snackbars for limits feedback.

Commission admin failures are owned by the affected section:

```text
apps/tch-portal/src/app/features/private/admin/pages/commission
page target:
- admin.commission.overview
section targets:
- admin.commission.defaultRate
- admin.commission.sellers
```

The overview load is page-blocking. Seller list and seller/default-rate actions render through
their owning section cards and suppress shell feedback.

Games admin failures are owned by the active tab or dialog:

```text
apps/tch-portal/src/app/features/private/admin/pages/games
tab targets:
- admin.games.enabled
- admin.games.enabled.<gameCode>
- admin.games.catalog
- admin.games.catalog.<gameCode>
dialog target:
- admin.games.settings
```

Enabled-games and catalog load/action failures render inside their tab. Settings failures render
inside the settings dialog. These calls suppress shell feedback.

Subscription admin failures are page or actions-card owned:

```text
apps/tch-portal/src/app/features/private/admin/pages/subscription
page target:
- admin.subscription.load
section target:
- admin.subscription.actions
```

Subscription load failures render as the page error. Renew, cancel, suspend, and resume failures
render on the Actions card and suppress shell feedback.

### Field inline

Use field errors when the failure belongs to one form control.

The backend should send field details in `ProblemDetail`:

```json
{
  "status": 400,
  "code": "validation.failed",
  "errors": {
    "rate": ["Rate must be between 0 and 100."]
  },
  "violations": [
    {
      "code": "validation.failed",
      "field": "rate",
      "target": "admin.businessProfile.commission.rate",
      "message": "Rate must be between 0 and 100."
    }
  ]
}
```

Forms map these details to `FormControl` errors using `applyServerFieldErrors(...)`.
Fields render with `tch-field-error`.

Current migrated examples:

```text
apps/tch-portal/src/app/features/private/admin/business-profile/pages/overview
targets:
- admin.businessProfile.identity.name -> name
- admin.businessProfile.region.timezone -> timezone
- admin.businessProfile.region.currency -> currency
- admin.businessProfile.commission.rate -> rate
- admin.businessProfile.address.line1 -> line1
- admin.businessProfile.address.line2 -> line2
- admin.businessProfile.address.city -> city
- admin.businessProfile.address.region -> region
- admin.businessProfile.address.country -> country
- admin.businessProfile.address.postalCode -> postalCode

apps/tch-portal/src/app/features/private/admin/setup/pages/settings/admin-config.page
targets:
- admin.setup.locale.defaultLanguage -> defaultLanguage
- admin.setup.locale.defaultLocale -> defaultLocale
- admin.setup.locale.fallbackLanguage -> fallbackLanguage
- admin.setup.receipt.displayName -> displayName
- admin.setup.receipt.headerMessage -> headerMessage
- admin.setup.receipt.footerMessage -> footerMessage
- admin.setup.receipt.defaultPaperSize -> defaultPaperSize

apps/tch-portal/src/app/features/private/admin/seller-terminals/pages/new
targets:
- admin.sellerTerminal.create.terminalCode -> terminalCode
- admin.sellerTerminal.create.displayName -> displayName
- admin.sellerTerminal.create.email -> email
- admin.sellerTerminal.create.phoneNumber -> phoneNumber
- admin.sellerTerminal.create.initialPin -> initialPin
- admin.sellerTerminal.create.commissionRate -> commissionRate
- admin.sellerTerminal.create.address.line1 -> address.line1
- admin.sellerTerminal.create.address.city -> address.city
- admin.sellerTerminal.create.address.country -> address.country
```

Client validators still run first. Server field errors are cleared before a new submit, when the
form is reopened, or when the user cancels the form.

## Backend Expectations

### Blocking failures

Use `ProblemDetail` for failures that block the operation or page.

Required fields:

- stable `code`;
- HTTP `status`;
- correlation fields when available: `requestId`, `traceId`, `spanId`, `errorId`.

The web translates by stable code first, then category, then generic fallback.
Raw backend `title`, `detail`, exception messages, provider messages, SQL messages, and stack traces
must not be used as public/minimal UI copy.

### Non-blocking BFF failures

Use `ApiResponse.notices` for optional slice failures.

Required notice shape:

- stable `code`;
- `domain`;
- `severity`;
- `meta.surface`;
- `meta.placement`;
- `meta.target`;
- correlation fields when available.

Backend BFFs should use `BffSlices.optional(...)` or `ApiResponseNotices` so metadata is assembled
consistently.

### Service degradation

Use both:

- `ApiResponse.notices` for user-facing copy and target;
- `ApiResponse.services` for overall response status such as `PARTIAL`.

Do not convert non-blocking notices into HTTP errors.

## Web Placement

Current placement:

```text
libs/api/src/lib/http/web-app-error.ts
apps/tch-portal/src/app/core/api/api-feedback.interceptor.ts
apps/tch-portal/src/app/core/api/local-error-routing.ts
apps/tch-portal/src/app/core/feedback/
libs/ui/components/src/lib/field-error/
libs/ui/components/src/lib/section-error/
```

Rules:

- API clients/normalizers inspect raw backend shapes.
- UI components receive normalized view input or Angular controls.
- Reusable UI components must not inspect `HttpErrorResponse`, `ProblemDetail`, or raw `ApiNotice`.
- Feature stores/pages own page, section, and field errors.
- Shell feedback owns only cross-cutting, unconsumed errors.

## Rendering Primitives

Reusable UI error primitives are documented in
[`libs/ui/components/ERRORS.md`](../../libs/ui/components/ERRORS.md).

Use:

- `ShellFeedbackStore` and shell outlet for shell top feedback;
- page state or feature store for page top errors;
- `tch-section-error` or a section-target directive for section top errors;
- `applyServerFieldErrors(...)` plus `tch-field-error` for field inline errors.

For PageModel dashboards, backend provider failures are emitted as targeted notices and mapped into
widget-local `dynamic.errors`.

## Anti-Patterns

Do not:

- create one global app-wide error store for every page, section, and field failure;
- show the same failure in shell and page/section/field;
- expose raw backend exception/provider/SQL/stack messages to users;
- add exact-code translations for temporary or raw backend strings;
- add generic retry actions for operations that do not own retry safety;
- navigate users to a different shell from an error action.
