# Design — private-bootstrap-v1 (web)

## File map

```
apps/tch-portal/src/app/
  core/
    auth/
      auth.guard.ts                          ← EXTENDED (bootstrap trigger)
    runtime/
      app-runtime.store.ts                   ← EXTENDED (initPrivateRuntime delegates)
      private-bootstrap.model.ts             ← NEW
      private-bootstrap.service.ts           ← NEW
      private-bootstrap.store.ts             ← NEW
      private-runtime-initializer.ts         ← NEW
      runtime-theme.service.ts               ← NEW
      runtime-i18n.service.ts                ← NEW
      notification-polling.service.ts        ← NEW
      index.ts                               ← EXTENDED (export new types/services)
  features/
    private/
      shell/
        private-shell.component.ts           ← EXTENDED (reads bootstrap store)
        private-shell.service.ts             ← EXTENDED (or replaced by store reads)
        private-navigation.model.ts          ← DELETED (navigation comes from PageModel shell)
      page-model/                            ← NEW directory
        private-page-model.model.ts          ← NEW
        private-page-model.service.ts        ← NEW
        private-page-renderer.component.ts   ← NEW
```

## Key interfaces (`private-bootstrap.model.ts`)

```typescript
export type PrivateSpace = 'platform' | 'admin' | 'cashier';

export type PrivateBootstrapStatus =
  | 'idle' | 'loading' | 'ready' | 'partial' | 'blocked' | 'error';

export interface RuntimeBootstrapResponse {
  readonly space: PrivateSpace;
  readonly user: AuthenticatedUserView;
  readonly tenantContext?: TenantContextView | null;
  readonly settings: RuntimeSettingsView;
  readonly theme: RuntimeThemeView;
  readonly i18n: RuntimeI18nBundle;
  readonly entitlements: EntitlementsView;
  // navigation is ABSENT — comes from PageModel shell.navigationDrawer
  readonly readiness: RuntimeReadinessView;
  readonly notifications: NotificationSummaryView;
  readonly pageModelRef: PageModelRef;
  readonly notices?: readonly RuntimeBootstrapNotice[];
}

export interface PageModelRef {
  readonly route: string;
  readonly endpoint: string;
  readonly version?: string | null;
}
```

`PrivateNavigationModel`, `PrivateNavigationSection`, `PrivateNavigationItem` are **not in the
bootstrap contract**. The sidenav is rendered from `PageRuntimeResponse.shell.navigationDrawer`
which `features.pagemodel` resolves via JSON fragments (`private_sidebar_cashier`, etc.).

Full interface list: `AuthenticatedUserView`, `TenantContextView`, `RuntimeSettingsView`,
`RuntimeThemeView`, `RuntimeI18nBundle`, `EntitlementsView`, `RuntimeReadinessView`,
`RuntimeReadinessCheck`, `NotificationSummaryView`, `NotificationPreview`,
`RuntimeBootstrapNotice`.

## Bootstrap endpoint

Single endpoint — space is resolved server-side from the JWT:

```typescript
// PrivateBootstrapService
bootstrap(): Observable<RuntimeBootstrapResponse> {
  return this.client.get<ApiResponse<RuntimeBootstrapResponse>>('/runtime/bootstrap').pipe(
    map(r => r.data)
  );
}
```

`TchBackendClient` is used — not `HttpClient` directly (per web HTTP convention).

No `endpointFor(space)` mapping needed — one URL for all spaces.

## Bootstrap store (signal-based)

`PrivateBootstrapStore` uses Angular signals:

- `status: Signal<PrivateBootstrapStatus>`
- `bootstrap: Signal<RuntimeBootstrapResponse | null>`
- Computed: `user`, `space`, `readiness`, `notifications`, `pageModelRef`
- No `navigation` computed — navigation comes from page-model store

`AppRuntimeStore.initPrivateRuntime()` delegates to `PrivateRuntimeInitializer.initialize()`.

## PrivateShell and navigation

`PrivateShell` no longer reads navigation from the bootstrap store. It reads navigation from
the `PageRuntimeResponse.shell.navigationDrawer` loaded by `PrivatePageModelService`.
`private-navigation.model.ts` is deleted — the `ActionItem` / `NavigationSection` contracts
from `@tch/api` already cover the page-model shell contract.

## Notification polling rules

- Start: called once by `PrivateRuntimeInitializer` after successful bootstrap.
- Interval: 10 minutes.
- Forced refresh: 30 minutes after login (one-shot timer after `startAfterLogin()`).
- Stop on logout: clears interval + timer.
- No duplicate pollers: guard with a running flag.
- Failure: does not break app; updates store with `unreadCount: 0`.
- Polling endpoint: confirm exact path from `platform.notification` (e.g.
  `GET /tenant/me/notifications/summary`) — use `TchBackendClient`.

## Error handling matrix

| Condition         | Status    | Shell renders? | Action                              |
|-------------------|-----------|----------------|-------------------------------------|
| loading           | loading   | skeleton        | show private loading shell          |
| success           | ready     | yes             | render shell + page                 |
| soft failure      | partial   | yes             | render shell + notice banner        |
| cashier blocked   | blocked   | yes             | render shell + blocked state        |
| 401               | error     | no              | redirect to Keycloak login          |
| 403               | error     | no              | show no-access page                 |
| entitlements null | error     | no              | render private error page           |
| navigation null   | error     | no              | render private error page           |
| theme failure     | partial   | yes             | apply default theme, notice         |
| i18n failure      | partial   | yes             | use fallback bundle, notice         |
| notif failure     | partial   | yes             | unreadCount=0, no crash             |
| pageModel failure | ready*    | yes             | page-level error only, shell intact |

*Bootstrap status is `ready`; page error is scoped to the content area.

## `PrivatePageModelService`

```typescript
load(ref: PageModelRef): Observable<PageRuntimeResponse> {
  return this.client.get<ApiResponse<PageRuntimeResponse>>(ref.endpoint).pipe(
    map(r => r.data)
  );
}
```

`PrivatePageModelResponse` is an alias or thin wrapper over `PageRuntimeResponse` from
`libs/page-model`. No parallel contract is introduced.

## Caching rules

| Data              | Cache scope                          |
|-------------------|--------------------------------------|
| bootstrap         | memory until logout / token change   |
| theme             | applied once from bootstrap          |
| i18n              | memory per lang/surface              |
| navigation        | from bootstrap, memory per space     |
| pageModel         | no cache in V1                       |
| notifications     | summary from bootstrap, then polling |

Logout clears: `PrivateBootstrapStore`, notification polling, page-model cache.
