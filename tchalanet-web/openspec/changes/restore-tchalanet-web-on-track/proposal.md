# Change: Restore tchalanet-web on track

## Status

Proposed

## Owner

Tchalanet Web

## Context

The web application has not been actively maintained recently and is now desynchronized from the backend.

The previous web state included:

- public homepage rendering from an older PageModel contract
- initial auth flow working
- early public header/footer and widget rendering
- Angular/Nx setup with older dependencies
- previous design tokens and layout decisions

Since then, the backend and product architecture evolved:

- PageModel contract changed
- public home structure changed
- header/footer decisions changed
- auth/access control flow evolved
- backend APIs and response shapes likely changed
- mobile and edge are now separate priorities
- super admin UI can remain Swagger for MVP
- tenant admin UI is not the immediate priority

The goal is to bring `tchalanet-web` back to a stable baseline before focusing on mobile.

## Problem

The web app may now have:

- outdated Angular/Nx dependencies
- stale PageModel types
- broken API client mappings
- broken or outdated auth flow
- UI glitches caused by design changes
- old homepage widgets incompatible with new backend
- no current private shell aligned with authenticated user flow
- environment/runtime config drift

## Goals

Restore the web app to a minimal, working, MVP-compatible state:

- dependencies updated/migrated safely
- Angular/Nx workspace builds
- auth flow works again
- public homepage renders using the current PageModel contract
- design is adjusted to current header/footer/theming decisions
- private shell exists after login
- private shell shows sidebar and placeholder content
- unsupported widgets fail gracefully
- app can be used for client demo/testing
- work remains limited and does not attempt to build full tenant admin or super admin UI

## Non-goals

- Do not build full tenant admin UI.
- Do not build super admin UI.
- Do not implement all dashboard widgets.
- Do not finalize every public home widget.
- Do not refactor the entire design system.
- Do not introduce large new frontend architecture.
- Do not modify backend APIs unless explicitly required by a separate server change.
- Do not build mobile features in the web project.

## MVP target

The web MVP baseline is:

```text
Public
├── Public homepage
│   ├── Header
│   ├── Hero
│   ├── Today draws / recent results
│   ├── Feature cards
│   ├── News/Tchala placeholders if needed
│   └── Footer
│
Auth
├── Login redirect
├── Callback handling
├── Token/session restore
├── API interceptor
├── 401/403 handling
└── Logout
│
Private
├── Authenticated layout
├── Sidebar
└── Content placeholder
```

## Public home section order

The public homepage should follow the saved product decision:

1. Header
2. Hero
3. Tirages du jour / Résultats récents
4. Fonctionnalités clés
5. Actualités du monde de la loterie
6. Le Tchala
7. Témoignages / Plans & Tarifs
8. Footer

Not all widgets must be fully dynamic in this change. Placeholders are acceptable where backend or content is not ready.

## Auth target

The minimum working auth flow is:

```text
public home
   ↓ login
Keycloak/OIDC
   ↓ callback
private shell
   ↓
sidebar + content placeholder
```

## Private shell target

The private shell should include:

```text
PrivateShell
├── Sidebar
│   ├── Dashboard
│   ├── Tickets
│   ├── Tirages
│   ├── Résultats
│   └── Profil
└── Main content
    └── Placeholder text
```

The placeholder can be simple:

```text
Bienvenue dans l’espace privé Tchalanet.
```

## Dependency upgrade strategy

Use controlled migrations instead of random package upgrades.

Expected workflow:

```bash
npm install
npx nx report
npx nx migrate latest
npm install
npx nx migrate --run-migrations
npx nx report
npx nx build tchalanet-web
npx nx lint tchalanet-web
```

If latest migration is too risky, stop at the highest stable compatible version and document the reason.

## Design rules

The web app must continue to follow Tchalanet frontend rules:

- mobile-first
- Angular OnPush where applicable
- Angular signals where appropriate
- template control flow `@if` / `@for`
- Angular Material
- CSS variables/design tokens
- no hardcoded colors in components
- i18n fr/en/ht
- responsive breakpoints 480/768/1024
- accessible focus/contrast
- RTL-aware styling where practical

## Header/footer current direction

Respect current public header decisions:

- burger mobile only
- no burger on tablet/desktop
- tablet/desktop L1 has Brand left, Nav center, Account right
- mobile/tablet L2 has CTA left and actions right
- search icon hidden if search feature flag is disabled
- overlay z-index conventions preserved
- avatar/account is a simple link initially
- desktop right side: search, language, theme, CTA/account

This change does not need to perfect all visual details, but must avoid reintroducing the old incompatible layout.

## PageModel target

The web must consume the current backend PageModel shape through a clear adapter layer.

Expected flow:

```text
Backend PageModel API
        ↓
PageModelApiClient
        ↓
PageModelService
        ↓
PublicShell/PublicHomePage
        ↓
Header + GridLayout + WidgetRenderer + Footer
```

Unsupported widgets should be handled by `UnsupportedWidget` or equivalent fallback.

## Expected outcome

After this change:

- `tchalanet-web` installs successfully
- Angular/Nx versions are current enough for the project direction
- build passes or known blockers are documented
- auth flow reaches private shell
- homepage renders from backend PageModel or fallback PageModel
- private shell sidebar renders
- content placeholder renders
- major visual glitches are fixed if they block navigation/testing
- no super admin UI is built
- tenant admin UI is deferred

## Risks

- Angular/Nx migration may require intermediate versions.
- Backend PageModel contract may still be unstable.
- Auth changes may require updated Keycloak/client config.
- Runtime environment config may differ between local/staging/prod.
- Old components may rely on deprecated Angular/Material APIs.
- PageModel widgets may be missing backend data.

## Rollback strategy

- Keep dependency migration in its own commit.
- Keep auth repair in its own commit.
- Keep PageModel resync in its own commit.
- Keep private shell placeholder in its own commit.
- If migration fails, revert only the dependency commit and continue with compatibility fixes separately.
