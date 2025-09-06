# ADR 0002 — Web Architecture Decisions (Nx Angular)

Date: 2025-09-06  
Status: Accepted  
Context: Setup of the web frontend (`tchalanet-web`) using Angular 20 + Nx.

---

## 1. Workspace Tooling
- **Nx Angular monorepo preset** chosen to enforce modularity and long-term scalability.
- Nx provides:
    - Generators (apps/libs with enforced conventions).
    - Module boundaries via ESLint.
    - Affected builds for fast CI/CD.
    - Plugin ecosystem (Angular, Vite, Playwright, etc.).

---

## 2. Bundler
- **esbuild** selected for dev/build speed.
- Nx supports Webpack and Vite, but esbuild is the default and simplest for Angular 20.
- Possible migration to Vite later if desired (`nx g @nx/angular:setup-vite`).

---

## 3. Test Strategy
- **Unit tests**: Vitest (fast, modern, great TS/ESM support).
- **E2E tests**: Playwright (cross-browser, modern API).
- Jest was considered, but Vitest was preferred for new codebases.

---

## 4. Component Generation Defaults
Configured in `nx.json` and `apps/web/project.json`:

- **Standalone components** only (no NgModules).
- **ChangeDetection**: OnPush by default.
- **Style**: SCSS.
- **File structure**: flat (`my-feature.component.ts`).
- **Suffix**: `.component.ts` (explicit).
- **Inline template/style**: enabled by default, can override with `--inlineTemplate=false`.
- **Prefix**: `tch` for all selectors (`<tch-home>`, `<tch-ticket-card>`).

---

## 5. Library Structure
We follow Nx’s recommended pattern:

- `libs/shared/ui` → reusable UI components (buttons, cards…).
- `libs/config` → runtime config (API base URL, theme, i18n).
- `libs/<domain>/feature-*` → feature-specific modules (tickets, tenants, users).
- `libs/<domain>/data-access` → API services, stores.
- `libs/<domain>/util-*` → pure utilities.

Boundary rules enforced with `@nx/enforce-module-boundaries` in ESLint.

---

## 6. Routing
- Routing is **standalone** (`app.routes.ts`).
- Feature pages use `--type=page` convention (e.g., `tickets.page.ts`).
- Example:
  ```ts
  export const routes: Routes = [
    { path: '', loadComponent: () => import('./home.component').then(m => m.HomeComponent) },
    { path: 'tickets', loadComponent: () => import('./tickets.page').then(m => m.TicketsPage) }
  ];

## 7. CI/CD 
GitHub Actions configured for:
- **Lint** : `nx run web:lint`
- **Test** : `nx run web:test`
- **Build** : `nx run web:build`
- **Dependency caching** : `npm ci` for faster runs.

---

## 8. Naming & Conventions

- **Component selectors** : `<tch-*>`
- **Component files** : `*.component.ts`, `*.page.ts`
- **Services** : `XxxService`
- **Stores** : `XxxStore` (signals or NgRx)
- **Routes** : kebab-case paths (`/tickets`, `/user-profile`)
