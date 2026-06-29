# Web State Management — Tchalanet

> **Status**: DRAFT v0.2
> **Scope**: Angular Signals, store services, SignalStore if adopted, NgRx if ever needed
> **Living doc** — update in the same commit as any code that changes a rule here.

---

## 1. Mental model

State is placed by lifetime and ownership.

```text
component-local state       -> component signal()
screen/feature state        -> features/<surface>/<feature>/*.store.ts
runtime transverse state    -> owning shared/runtime lib
reusable API/cache state    -> owning active lib, or feature until second consumer
global app state            -> minimal, explicit, owned by the relevant capability
```

Do not create a global store for everything.

---

## 2. Current architecture alignment

Active libs:

```text
libs/api
libs/shared-config
libs/ui/components
libs/ui/styles
libs/ui/theme
apps/<portal>/src/app/core
apps/<portal>/src/app/features
libs/api/src/lib/contracts
```

Target libs such as `shared-auth`, `shared-i18n`, `page-model`, `widgets`, and `web` are extraction targets, not mandatory placement today.

Rule:

```text
Do not create a data-access/shared-state lib just to match a pattern.
Extract only when a second real consumer or stable boundary appears.
```

---

## 3. State levels

### 3.1 Component-local state

Use `signal()` for small UI state:

```text
selected tab
menu open/closed
dialog open/closed
expanded/collapsed
local search text
local form draft
hover/active UI state
```

Example:

```ts
readonly selectedTab = signal<'open' | 'paid'>('open');
readonly searchText = signal('');
readonly isDialogOpen = signal(false);
```

Component-local state stays in the component.

---

### 3.2 Screen / feature state

Feature state lives next to the feature/page:

```text
apps/<portal>/src/app/features/<surface>/<feature>/<feature>.store.ts
```

Examples:

```text
features/admin/outlets/outlets.store.ts
features/admin/dashboard/tenant-dashboard.store.ts
features/platform/page-models/page-model-editor.store.ts
features/cashier/sale/cashier-sale.store.ts
```

Contains:

```text
filters
pagination
selected item
loading/error
view mode
dialog state
page-specific API result
screen-specific derived state
```

A page can provide its feature store locally if the state should be destroyed with the route.

---

### 3.3 Runtime transverse state

Runtime state belongs to the owning runtime capability.

Examples:

```text
auth session      -> core/auth during migration, future shared-auth
locale            -> core/i18n during migration, future shared-i18n
runtime settings  -> libs/shared-config
feature flags     -> libs/shared-config
theme current     -> libs/ui/theme
theme mode        -> libs/ui/theme
```

Do not put theme state in a generic global store.

Do not put locale state inside PageModel.

Do not put auth state inside UI components.

---

### 3.4 Reusable API/cache state

Reusable API/cache state belongs to the owning active lib when the boundary exists.

Examples:

```text
libs/api/src/lib/cache/catalog-cache.store.ts
libs/api/src/lib/cache/page-model-cache.store.ts
libs/ui/theme/src/lib/theme-preset.store.ts
```

If there is only one consumer, keep the state in the feature.

Extract only when:

* a second feature needs the same cache/resource;
* the API boundary is stable;
* the public exports are clear;
* Nx dependencies stay clean.

Avoid creating:

```text
data-access/<domain>/state
```

as a mandatory structure before the boundary exists.

---

### 3.5 Global app state

Global app state must be small and explicit.

Allowed examples:

```text
auth session
current locale
runtime config
active theme
shell/navigation state if shared by multiple private surfaces
```

Placement should still follow ownership:

```text
auth      -> core/auth now, future shared-auth
locale    -> core/i18n now, future shared-i18n
config    -> shared-config
theme     -> ui/theme
shell nav -> web/private-shell or core during migration
```

---

## 4. Signals vs store service vs NgRx

### Native Signals

Use native `signal()` / `computed()` for small local state.

Best for:

```text
component UI state
small form state
temporary toggles
selected tab
expanded state
```

---

### Store service

Use an explicit injectable store service for page/feature state with API calls, filters, pagination, loading/error, or derived view state.

Example:

```text
outlets.store.ts
payouts.store.ts
cashier-sale.store.ts
tenant-dashboard.store.ts
```

A feature store may use:

```text
signal()
computed()
effect()
RxJS interop
firstValueFrom
```

Keep the API calls in the store/service, not in presentational components.

---

### SignalStore

SignalStore may be introduced when a feature store becomes complex enough to benefit from the pattern.

Do not introduce SignalStore globally by default.

Use it when:

```text
state transitions are numerous
derived selectors are growing
updaters/effects improve clarity
multiple pages share a store pattern
```

---

### NgRx Store

NgRx Store is not the default.

Consider NgRx only if there is a real need for:

```text
complex multi-page flows
global action log
time-travel/debugging
many cross-cutting effects
team-wide NgRx conventions
```

Current decision:

```text
Start with native Signals + explicit store services.
Add NgRx only after real pressure appears.
```

---

## 5. Placement guide

| Need                                          | Placement                                        |
| --------------------------------------------- | ------------------------------------------------ |
| Dialog open/closed                            | component `signal()`                             |
| Selected tab                                  | component `signal()`                             |
| Feature filters/pagination                    | `features/<surface>/<feature>/*.store.ts`        |
| Page loading/error                            | page or feature store                            |
| Current auth session                          | `core/auth` now, future `shared-auth`            |
| Permissions                                   | auth capability                                  |
| Locale                                        | `core/i18n` now, future `shared-i18n`            |
| Runtime settings                              | `libs/shared-config`                             |
| Feature flags                                 | `libs/shared-config`                             |
| Active theme/mode                             | `libs/ui/theme`                                  |
| Theme preset cache                            | `libs/ui/theme`                                  |
| PageModel editor state                        | `features/platform/page-models`                  |
| PageModel runtime cache used by many features | `libs/page-model` when a shared cache is needed  |
| Catalog/reference cache                       | `libs/api` only after stable multi-consumer need |
| Tenant config used by one page                | feature store                                    |
| Tenant config used by many pages              | owning active lib after extraction               |

---

## 6. API response state

HTTP responses follow the API contract.

Feature stores should track:

```text
loading
error
data
empty state
pagination if relevant
notices if relevant
partial/service status if relevant
```

Do not flatten important API notices away if the UI needs to display them.

Errors should be represented as user-displayable state, not raw thrown objects in components.

---

## 7. Loading/error convention

Use the three-level error model:

```text
tch-page-error   -> route/page-level failure
tch-error-panel  -> section/card/widget failure
tch-field-error  -> form field failure
```

For loading:

```text
tch-loading      -> generic page/section loading
component skeletons when layout stability matters
```

Feature/page stores should expose enough state for the page to choose the correct UI:

```text
loading
loaded
error
empty
partial
```

---

## 8. Persistence

Do not read/write `localStorage` directly from features.

Allowed owners:

```text
auth/session storage       -> auth capability
theme preference if local  -> ui/theme
locale preference          -> i18n capability
runtime config cache       -> shared-config if needed
```

Feature state should be persisted only if there is a clear UX reason.

---

## 9. Example: auth runtime store

```ts
export interface AuthSession {
  readonly userId: string;
  readonly displayName: string;
  readonly roles: readonly string[];
  readonly tenantCode?: string;
  readonly permissions: readonly string[];
}

@Injectable({ providedIn: 'root' })
export class AuthSessionStore {
  private readonly sessionSignal = signal<AuthSession | null>(null);

  readonly session = this.sessionSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.sessionSignal() !== null);
  readonly permissions = computed(() => this.sessionSignal()?.permissions ?? []);

  setSession(session: AuthSession): void {
    this.sessionSignal.set(session);
  }

  clear(): void {
    this.sessionSignal.set(null);
  }

  hasPermission(permission: string): boolean {
    return this.permissions().includes(permission);
  }
}
```

---

## 10. Example: feature store

```ts
@Injectable()
export class PayoutsStore {
  private readonly api = inject(PayoutApiService);

  readonly page = signal(0);
  readonly size = signal(20);
  readonly status = signal<PayoutStatus | null>(null);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly items = signal<readonly PayoutItem[]>([]);
  readonly total = signal(0);

  readonly empty = computed(() => !this.loading() && !this.error() && this.items().length === 0);

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);

    try {
      const response = await firstValueFrom(
        this.api.list({
          page: this.page(),
          size: this.size(),
          status: this.status(),
        }),
      );

      this.items.set(response.data.items);
      this.total.set(response.data.total);
    } catch {
      this.error.set('payouts.loadError');
    } finally {
      this.loading.set(false);
    }
  }
}
```

---

## 11. Anti-patterns

Avoid by default:

```text
shared/facades
shared/stores
global app store for all pages
BaseStore too early
CrudStore too early
GenericStore too early
duplicated API calls inside components
components injecting HttpClient
features reading localStorage directly
theme state outside ui/theme
locale state inside PageModel
PageModel state mixed with widget business state
```

---

## 12. PR checklist

Before merging state changes:

* [ ] State ownership is clear.
* [ ] Component-local state stays local.
* [ ] Feature state lives next to the feature.
* [ ] Runtime transverse state lives in the owning runtime lib/capability.
* [ ] No new shared/data-access lib was created without a real boundary.
* [ ] Presentational UI components do not call APIs.
* [ ] Loading/error/empty states are explicit.
* [ ] localStorage is not accessed directly from a feature.
* [ ] NgRx was not introduced without documented need.
