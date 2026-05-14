# Tasks: Restore tchalanet-web on track

## 0. Mandatory context check

Before editing, run:

```bash
pwd
git rev-parse --show-toplevel
git branch --show-current
git status --short
git log -1 --oneline
find . -maxdepth 4 -type d -name openspec
```

- [ ] Confirm active project is `tchalanet-web`.
- [ ] Confirm OpenSpec path is `tchalanet-web/openspec`.
- [ ] Confirm worktree is not stale/detached/missing OpenSpec.
- [ ] If desktop app forces a worktree, verify it is synced with the real project.
- [ ] Do not modify server/mobile/edge unless explicitly requested.

## 1. Baseline inventory

- [ ] Run dependency/tooling report:

```bash
npm install
npx nx report
```

- [ ] Identify current Angular version.
- [ ] Identify current Nx version.
- [ ] Identify current Node/npm versions.
- [ ] Identify current Angular Material version.
- [ ] Identify current build target name.
- [ ] Identify current app routes.
- [ ] Identify current PageModel service/types.
- [ ] Identify current auth flow files.
- [ ] Identify current runtime environment config.
- [ ] Document immediate blockers.

## 2. Dependency migration

- [ ] Create a clean commit/checkpoint before migration.
- [ ] Run controlled migration:

```bash
npx nx migrate latest
npm install
npx nx migrate --run-migrations
```

- [ ] If migration fails, document exact blocker.
- [ ] Fix TypeScript/Angular/Nx config issues caused by migration.
- [ ] Fix ESLint config drift if present.
- [ ] Fix Angular Material import/API issues if present.
- [ ] Run:

```bash
npx nx report
npx nx build tchalanet-web
npx nx lint tchalanet-web
```

- [ ] Commit dependency migration separately.

## 3. Runtime config cleanup

- [ ] Locate current environment/runtime config.
- [ ] Centralize API base URL access.
- [ ] Centralize auth issuer/client/redirect config.
- [ ] Remove scattered hardcoded backend URLs from components.
- [ ] Add local fallback config if necessary.
- [ ] Ensure config works in dev mode.

## 4. API response handling

- [ ] Locate current API client/wrapper handling.
- [ ] Align with backend `ApiResponse<T>` if applicable.
- [ ] Centralize unwrap/error handling.
- [ ] Ensure components receive clean data models.
- [ ] Add handling for 401/403/network errors.

## 5. Auth flow repair

- [ ] Locate current auth implementation.
- [ ] Verify login redirect.
- [ ] Verify callback route.
- [ ] Verify token/session persistence.
- [ ] Verify logout.
- [ ] Verify auth guard for `/app`.
- [ ] Verify HTTP interceptor sends bearer token.
- [ ] Verify expired/missing token behavior.
- [ ] Verify 401/403 handling.
- [ ] Add a simple authenticated route to private shell.

Expected flow:

```text
/ → login → callback → /app
```

## 6. Private shell/sidebar placeholder

- [ ] Create or repair private shell route:

```text
/app
```

- [ ] Add protected route guard.
- [ ] Add sidebar component.
- [ ] Add sidebar items:

  - Dashboard
  - Tickets
  - Tirages
  - Résultats
  - Profil

- [ ] Add main content placeholder:

```text
Bienvenue dans l’espace privé Tchalanet.
```

- [ ] Ensure layout is mobile-first.
- [ ] Ensure sidebar does not break mobile.
- [ ] Do not build full dashboard widgets yet.

## 7. PageModel contract resync

- [ ] Locate current PageModel DTO/types.
- [ ] Compare with current backend PageModel response.
- [ ] Update `PageModelApiClient`.
- [ ] Update `PageModelService`.
- [ ] Add mapper from backend DTO to web model.
- [ ] Add fallback behavior if backend PageModel fails.
- [ ] Ensure `PublicHomePage` consumes the new web model.
- [ ] Ensure WidgetRenderer receives normalized widget definitions.
- [ ] Add unsupported widget fallback.

Expected flow:

```text
Backend PageModel
→ PageModelApiClient
→ PageModelService
→ PublicHomePage
→ Header/Layout/Footer
```

## 8. Public home restoration

- [ ] Ensure `/` route loads public home.
- [ ] Render public header.
- [ ] Render hero section.
- [ ] Render today draws / recent results section or placeholder.
- [ ] Render feature cards.
- [ ] Render news placeholder if needed.
- [ ] Render Le Tchala placeholder if needed.
- [ ] Render testimonials/pricing placeholder if needed.
- [ ] Render footer.

Section order:

```text
1. Header
2. Hero
3. Tirages du jour / Résultats récents
4. Fonctionnalités clés
5. Actualités du monde de la loterie
6. Le Tchala
7. Témoignages / Plans & Tarifs
8. Footer
```

## 9. Header/footer design alignment

- [ ] Remove incompatible old header behavior if present.
- [ ] Ensure burger is mobile-only.
- [ ] Ensure tablet/desktop nav is visible.
- [ ] Ensure mobile/tablet L2 pattern is respected if implemented.
- [ ] Ensure CTA is visible.
- [ ] Ensure search icon can be hidden when search flag/config disabled.
- [ ] Ensure account/login link works.
- [ ] Ensure footer contains required public links.
- [ ] Ensure no hardcoded colors are introduced.

## 10. i18n cleanup

- [ ] Ensure required keys exist in `fr.json`.
- [ ] Add/update `en.json` keys.
- [ ] Add/update `ht.json` keys where practical.
- [ ] Use functional namespaces:

  - `nav.*`
  - `auth.*`
  - `cta.*`
  - `private.*`
  - `footer.section.*`
  - `pagemodel.*`

- [ ] Avoid duplicate concepts with different keys.

## 11. Styling/glitch fixes

Only fix blockers.

- [ ] Fix layout-breaking glitches.
- [ ] Fix header/sidebar overlap.
- [ ] Fix broken mobile navigation.
- [ ] Fix unreadable text in light/dark mode.
- [ ] Fix broken focus states if obvious.
- [ ] Avoid full visual redesign.

## 12. Validation

Run:

```bash
npx nx report
npx nx build tchalanet-web
npx nx lint tchalanet-web
```

If tests exist:

```bash
npx nx test tchalanet-web
```

Manual checks:

- [ ] `/` public homepage loads.
- [ ] Header visible.
- [ ] Footer visible.
- [ ] PageModel data renders or fallback works.
- [ ] Login starts auth flow.
- [ ] Callback completes.
- [ ] `/app` is protected.
- [ ] Authenticated user sees sidebar.
- [ ] Placeholder content renders.
- [ ] Search icon can be hidden if disabled.
- [ ] Mobile viewport does not break immediately.

## 13. Documentation

- [ ] Update `tchalanet-web/CLAUDE.md` with current web rules.
- [ ] Update `tchalanet-web/openspec/project.md` if missing.
- [ ] Document known deferred items:
  - full tenant admin
  - super admin UI
  - full widgets
  - PageModel admin/template editor
  - notification center
  - advanced dashboards

## 14. Acceptance criteria

- [ ] `npm install` or `npm ci` succeeds.
- [ ] `npx nx report` succeeds.
- [ ] `npx nx build tchalanet-web` succeeds, or blockers are explicitly documented.
- [ ] Public home renders with current PageModel or fallback.
- [ ] Auth flow reaches private shell.
- [ ] Private shell sidebar renders.
- [ ] Placeholder content renders.
- [ ] No backend/mobile/edge files are modified.
- [ ] OpenSpec remains project-local under `tchalanet-web/openspec`.
