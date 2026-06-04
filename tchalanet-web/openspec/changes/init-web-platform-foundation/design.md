# Design — Init Web Platform Foundation

## 1. Architecture intention

The Web foundation should provide a stable runtime platform without creating too many libraries or overly abstract components.

The project should group code by capability, not by every small component.

Recommended high-level areas:

```text
web/src/app/
  core/
    api/
    auth/
    settings/
    i18n/
    theme/
    pagemodel/
    shell/
  shared/
    ui/
    types/
  features/
    public/
    cashier/
    tenant-admin/
    platform-admin/
```

If Nx libraries already exist, the same capabilities can map to libs, but do not create a library for every widget or route.

## 2. Dependency policy

### Runtime dependencies expected

| Dependency | Purpose | Why needed | Do not use for |
|---|---|---|---|
| Angular Material | UI primitives, dialogs, nav, controls, theme integration | Faster consistent UI and Material theme support | Business state, custom page model logic |
| NgRx Store | Global app state | Session, settings, theme, i18n status, shell state | Local form state, every list, every modal |
| NgRx Effects | Side effects for auth/bootstrap/API orchestration | Keeps stores pure and loads observable | Business rules |
| Keycloak JS or wrapper | OIDC login/logout/token integration | Tchalanet auth uses Keycloak | Manual JWT parsing in components |
| Translation package or custom service | Runtime translations and merge behavior | Needed for frontend local + backend overrides | Storing PageModel or theme |

### Dev dependencies expected

| Dependency | Purpose | Rule |
|---|---|---|
| ESLint / Angular ESLint | Linting | Configure early; do not block dirty legacy until baseline clean |
| Prettier or formatter | Format consistency | Hook only after baseline clean if needed |
| Husky/lint-staged or equivalent | Pre-commit hooks | Optional; fast checks only |

## 3. Lint/pre-commit recommendation

Set up lint and format scripts immediately.

Do not force pre-commit hooks until:

- the project compiles;
- lint baseline is clean or scoped;
- hooks complete quickly;
- the team agrees the hook will not block urgent architectural cleanup.

Recommended later hook:

```text
pre-commit:
  - format staged files
  - lint staged files
```

Not recommended in pre-commit:

```text
- e2e tests
- backend full verify
- Flutter full integration tests
- performance tests
```

## 4. Auth design

`AuthService` owns Keycloak integration.

`AuthStore` or NgRx auth slice owns current session:

```ts
interface UserSession {
  authenticated: boolean;
  userId?: string;
  username?: string;
  displayName?: string;
  tenantId?: string;
  tenantCode?: string;
  roles: UserRole[];
  tokenExpiresAt?: string;
}
```

Guards:

- `AuthGuard`: authenticated required.
- `RoleGuard`: required role/surface.

Do not read Keycloak token directly in components. Components consume session state.

## 5. Runtime bootstrap design

Public bootstrap:

```ts
loadPublicBootstrap(lang, pageKey): Observable<PublicBootstrapState> {
  return forkJoin({
    settings: settingsApi.getPublicSettings(),
    i18nOverrides: i18nApi.getOverrides(lang, 'PUBLIC'),
    theme: themeApi.getPublicTheme(),
    pageModel: pageModelApi.getPublicPageModel(pageKey, lang),
  }).pipe(
    map(({ settings, i18nOverrides, theme, pageModel }) => ({
      settings,
      translations: i18nMerge.mergeLocalWithOverrides(lang, i18nOverrides),
      theme,
      pageModel,
    }))
  );
}
```

Private bootstrap may load after auth and may use surface-specific settings/theme/i18n.

## 6. PageModel separation

PageModel may include:

```json
{
  "page_key": "public.home",
  "sections": [
    {
      "id": "hero",
      "widgets": [
        {
          "type": "hero",
          "title_key": "public.home.hero.title",
          "actions": [
            { "type": "link", "label_key": "public.home.check_ticket", "path": "/check-ticket" }
          ]
        }
      ]
    }
  ]
}
```

PageModel must not include:

```json
{
  "translations": {},
  "theme": {},
  "settings": {}
}
```

## 7. UI component policy

Build UI components in two waves.

Wave 1: generic runtime support:

- Notice;
- ErrorPanel;
- Loading;
- EmptyState;
- ActionButton/List;
- StatusBadge;
- PageHeader;
- ConfirmDialog.

Wave 2: born from real pages:

- DataTable/PagedList from Tenant Admin list pages;
- Ticket/Money/Receipt components from POS/sales flows.

## 8. Test strategy

Minimum initial tests:

- i18n merge: backend overrides win.
- route guard: required roles are enforced.
- API error mapper: ProblemDetail mapped.
- bootstrap orchestration: four calls remain separate.

Do not start with massive e2e before the runtime foundation is stable.

## 9. V1 runtime bootstrap and private layout refinement

`AppRuntimeStore` owns the first web runtime orchestration before PageModel is introduced.
It separates runtime scope from readiness:

- `initPublicRuntime()` initializes i18n and theme, refreshes the auth session opportunistically, then requests public theme and settings.
- `initPrivateRuntime()` initializes i18n and theme, refreshes the auth session first, then requests private theme and settings only when the session is authenticated.
- If private bootstrap refreshes to an anonymous session, it falls back to public theme and settings.
- `ready` remains derived from theme/settings readiness plus bootstrap state, so starting loads is not treated as complete rendering readiness.
- Repeated calls for the same scope are idempotent, while a public boot may upgrade to private boot after protected navigation.
- `RuntimeBootstrapScope` represents the loaded runtime capability, not the current route. Public-to-private upgrade is allowed; private-to-public downgrade is not needed except on a full logout/reset.
- `RuntimeBootstrapState` includes `error` so auth/session refresh failures can be surfaced instead of silently becoming ready.

Private surfaces use guarded layout routes:

- `/app/cashier` is guarded by `roleGuard('CASHIER')`.
- `/app/admin` is guarded by `roleGuard('TENANT_ADMIN')`.
- `/app/platform` is guarded by `roleGuard('SUPER_ADMIN')`.

Each route renders the private shell first, then the empty child route renders `RoleDashboardPage` as the temporary smoke dashboard.
Placeholder cashier routes such as `sale` and `tickets` stay deferred until real pages or intentional placeholders exist.

`roleGuard` remains role-based and protects only the top-level surface.
Button/action authorization is deferred to backend capabilities, for example `ticket.sell`, `payout.execute`, and `terminal.bind`.

`I18nFacade.init()` is idempotent so repeated page/runtime bootstrap calls do not reset a user-selected language to `fr`.
Changing language dispatches the state change and the i18n effect calls `TranslateService.use(language)` to reload local/backend bundles.

Future language precedence is:

1. URL language
2. user preference
3. tenant default
4. page default
5. fallback `fr`

The V1 target backend i18n shape is `ApiResponse<I18nBundleResponse>`.
Transitional normalization may continue to accept older shapes during frontend/backend alignment, but those shapes are not the final contract.

Surface bundles are requested and merged in configured order.
Common surfaces must come first and specific surfaces later, for example:

```ts
['PUBLIC_COMMON', 'PUBLIC_HOME']
```

Later surfaces override earlier keys.
