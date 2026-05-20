# Design: Restore tchalanet-web on track

## Overview

This change restores the Angular/Nx web app to a minimal working state aligned with the current backend and product direction.

The focus is not full UI completion. The focus is operational readiness:

- app builds
- auth works
- PageModel renders
- public home is visible
- authenticated shell exists
- sidebar and placeholder content render

## Scope split

This change may be implemented in four internal phases:

```text
1. Upgrade dependencies
2. Repair auth flow
3. Resync PageModel runtime
4. Add private shell/sidebar placeholder
```

These phases can become separate commits or sub-changes later, but they are grouped here as one recovery change because they are all required to make the web usable again.

## Current target architecture

```text
src/app/
├── app.config.ts
├── app.routes.ts
│
├── core/
│   ├── auth/
│   │   ├── auth.service.ts
│   │   ├── auth.guard.ts
│   │   ├── auth.interceptor.ts
│   │   ├── auth-callback.component.ts
│   │   └── auth.models.ts
│   │
│   ├── api/
│   │   ├── api-client.ts
│   │   ├── api-response.ts
│   │   └── api-error-handler.ts
│   │
│   ├── config/
│   │   ├── runtime-config.service.ts
│   │   └── environment.model.ts
│   │
│   └── i18n/
│
├── features/
│   ├── public-home/
│   │   ├── public-home.page.ts
│   │   ├── public-home.routes.ts
│   │   └── public-home.mapper.ts
│   │
│   ├── private-shell/
│   │   ├── private-shell.component.ts
│   │   ├── private-sidebar.component.ts
│   │   └── private-shell.routes.ts
│   │
│   └── pagemodel/
│       ├── pagemodel.service.ts
│       ├── pagemodel-api.client.ts
│       ├── pagemodel.models.ts
│       └── widget-renderer/
│
└── shared/
    ├── ui/
    ├── theme/
    └── tokens/
```

This structure is indicative. Existing project conventions may be reused if already clean.

## Dependency migration design

### Rules

- Do not use broad uncontrolled `npm update`.
- Prefer Nx-managed migration.
- Commit before migration.
- Run validation after migration.
- If migration is too large, stop and document blockers.

### Commands

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

### Validation

At minimum:

```bash
npx nx build tchalanet-web
npx nx lint tchalanet-web
```

If tests exist:

```bash
npx nx test tchalanet-web
```

If e2e exists:

```bash
npx nx e2e tchalanet-web-e2e
```

## Runtime config

The web app should not hardcode backend URLs in components.

Expected config responsibilities:

```text
RuntimeConfigService
├── apiBaseUrl
├── auth issuer/clientId/redirectUri
├── feature flags base URL if needed
├── environment name
└── optional tenant/demo mode config
```

Local fallback is acceptable for MVP, but must be centralized.

## Auth design

### Flow

```text
User clicks login
    ↓
AuthService.login()
    ↓
OIDC redirect
    ↓
AuthCallbackComponent
    ↓
token/session established
    ↓
redirect to /app
    ↓
PrivateShell
```

### Required pieces

- `AuthService`
- login/logout methods
- callback handling
- route guard for `/app`
- HTTP interceptor for bearer token
- 401/403 handling
- user/session signal or observable
- safe fallback when token is missing/expired

### Private routes

```text
/app
├── /app/dashboard
├── /app/tickets
├── /app/draws
├── /app/results
└── /app/profile
```

For this change, all routes may point to placeholder content.

## PageModel design

### API flow

```text
PageModelApiClient
    ↓
GET current public home PageModel
    ↓
PageModelService
    ↓
normalize/adapt contract
    ↓
PublicHomePage
    ↓
PublicShell
```

### Adapter requirement

Use a mapping layer so backend evolution does not break all components directly.

```text
BackendPageModelDto
        ↓
PageModelMapper
        ↓
WebPageModel
```

### Fallback behavior

If the backend call fails in local/dev:

- show a controlled fallback PageModel if configured
- or show a friendly error block
- do not crash the entire app

### Unsupported widgets

Create or preserve an `UnsupportedWidget` fallback:

```text
UnsupportedWidget
├── dev mode: show widget type and missing mapping
└── prod mode: hide or show generic fallback
```

## Public home design

The public homepage should render the saved section order:

```text
Header
Hero
Today draws / recent results
Feature cards
Lottery news
Le Tchala
Testimonials / pricing
Footer
```

Widgets can be partially implemented:

- Hero: must render
- Draws/results: render data if available, otherwise placeholder
- Feature cards: render from PageModel or fallback
- News/Tchala/testimonials/pricing: placeholder acceptable
- Footer: must render

## Header/footer design

Use current Tchalanet decisions:

### Public header

- mobile-first
- burger mobile only
- tablet/desktop nav visible
- CTA visible
- search icon hidden if disabled
- lang/theme chips
- account/login link
- use tokens, no hardcoded colors

### Footer

- public footer links:
  - À propos
  - Plans/Tarifs
  - Documentation/FAQ
  - Contact/Support
  - Mentions légales
  - Politique de confidentialité
  - CGU
  - Jeu responsable
  - Réglementation

## Private shell design

Minimal private shell:

```text
PrivateShellComponent
├── toolbar/header area if already available
├── sidebar
└── router outlet / content area
```

Sidebar items:

```text
Dashboard
Tickets
Tirages
Résultats
Profil
```

Placeholder page:

```text
Bienvenue dans l’espace privé Tchalanet.
```

No dashboard widgets are required in this change.

## i18n

Use existing translation files if available:

```text
assets/i18n/fr.json
assets/i18n/en.json
assets/i18n/ht.json
```

Use functional namespaces:

```text
nav.*
auth.*
cta.*
private.*
pagemodel.*
footer.section.*
```

Do not duplicate keys.

## Styling rules

- no hardcoded hex colors in components
- use CSS variables and tokens
- mobile-first
- CSS logical properties where practical
- avoid `!important`
- keep accessibility states visible
- respect dark mode if already implemented

## API response handling

If backend responses use a standard wrapper:

```text
ApiResponse<T>
```

centralize unwrap/error behavior in one API layer.

Components should not manually parse wrapper details everywhere.

## Acceptance behavior

### Public

- `/` loads public home.
- Header renders.
- Footer renders.
- Hero renders.
- Draw/result section renders or falls back gracefully.
- Unsupported widgets do not crash the page.

### Auth

- Login action redirects to Keycloak/OIDC.
- Callback completes.
- Authenticated user reaches `/app`.
- Missing/expired token redirects or shows auth error cleanly.
- API interceptor sends token.

### Private

- `/app` is protected.
- Private shell renders after login.
- Sidebar renders.
- Main content placeholder renders.

### Build

- dependencies install
- Nx report works
- app builds
- lint either passes or documented blockers are listed

## Out of scope details

- full tenant admin dashboards
- super admin screens
- complete POS flow in web
- mobile seller flow
- notification center
- full PageModel admin/template editor
