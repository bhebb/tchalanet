# Frontend Dependencies Governance

## Rule

Do not add packages to `package.json` without documenting them here.

Each dependency must answer:

- What problem does it solve?
- Is it runtime, build, test, or dev-only?
- Which capability owns it?
- What built-in alternative was considered?
- What would make us remove or replace it?

## Current lint/tooling dependencies

| Package | Category | Owner capability | Purpose | Alternative considered | Keep / remove trigger |
|---|---|---|---|---|---|
| `eslint` | dev | quality | Runs flat-config lint checks for the Nx/Angular workspace. | TypeScript compiler only. | Keep while lint is a quality gate. |
| `@nx/eslint` | dev | quality/Nx | Provides the Nx lint executor used by `tch-portal:lint`. | Running ESLint manually. | Keep while Nx owns project targets. |
| `@nx/eslint-plugin` | dev | quality/Nx | Provides Nx flat config and module-boundary rules. | Custom ESLint-only boundaries. | Keep while Nx boundaries are enforced. |
| `angular-eslint` | dev | quality/Angular | Provides Angular and template lint rules. | Generic TypeScript lint only. | Keep for Angular-specific rules. |
| `@angular-eslint/eslint-plugin` | dev | quality/Angular | Provides Angular TypeScript lint rules imported by the legacy Web lint config. | Generic TypeScript lint only. | Remove only if Angular-specific TS rules are no longer used. |
| `@angular-eslint/eslint-plugin-template` | dev | quality/Angular | Provides Angular template lint rules imported by the legacy Web lint config. | Generic HTML lint only. | Remove only if Angular template linting is disabled. |
| `@angular-eslint/template-parser` | dev | quality/Angular | Parses Angular templates for ESLint. | No template linting. | Keep while template lint rules are active. |
| `@typescript-eslint/parser` | dev | quality/TypeScript | Parses TypeScript for flat ESLint configs, including the legacy `web-backup` config scanned by Nx. | Relying on inferred parser config. | Remove only if all ESLint configs no longer import it directly. |
| `@typescript-eslint/utils` | dev | quality/TypeScript | Supports TypeScript ESLint rule utilities. | No custom TS-aware lint utilities. | Keep while required by local lint dependencies or rules. |
| `typescript-eslint` | dev | quality/TypeScript | Provides TypeScript ESLint flat-config support. | Manual parser/plugin wiring only. | Keep while using flat TypeScript ESLint config. |
| `eslint-config-prettier` | dev | quality/format | Prevents formatting rules from conflicting with Prettier. | Manually disabling conflicting rules. | Keep while Prettier and ESLint are both used. |
| `eslint-plugin-playwright` | dev | quality/e2e | Lints Playwright e2e tests. | Generic lint rules for tests. | Keep while Playwright tests exist. |
| `eslint-plugin-simple-import-sort` | dev | quality/imports | Supports deterministic import sorting in the existing legacy Web lint config. | Prettier or manual import ordering. | Remove if import sorting is removed from all ESLint configs. |
| `eslint-plugin-unused-imports` | dev | quality/imports | Removes and flags unused imports in the existing legacy Web lint config. | TypeScript no-unused checks only. | Remove if unused import cleanup is handled elsewhere. |
| `prettier` | dev | quality/format | Formats workspace files consistently. | Editor-only formatting. | Keep if team accepts format standard. |

## Runtime dependencies

| Package | Category | Owner capability | Purpose | Alternative considered | Keep / remove trigger |
|---|---|---|---|---|---|
| `@angular/material` | runtime | UI/theme | Material components and Angular theme integration. | Custom components only. | Keep while Angular Material is the UI base. |
| `@angular/cdk` | runtime | UI/platform | CDK primitives required by Angular Material and overlays. | Hand-built overlay/a11y primitives. | Keep while Material/CDK primitives are used. |
| `@ngrx/store` | runtime | state | Provides the root app store for future auth/session/runtime slices. | Angular signals and explicit stores only. | Remove if the accepted Web state model returns to signal-only stores. |
| `@ngrx/effects` | runtime | state/API | Provides side-effect orchestration for future auth/bootstrap/runtime loads. | Services manually called by components. | Remove if runtime orchestration stays entirely in explicit services. |
| `@ngrx/router-store` | runtime | state/routing | Connects Angular Router state to the NgRx store. | Reading router state directly from Angular Router. | Remove if no store-driven routing selectors are used. |
| `@ngrx/store-devtools` | runtime dev-only | state/debug | Enables NgRx debugging in development mode only. | Console logging or Angular DevTools only. | Keep only while NgRx store remains active. |
| `@ngx-translate/core` | runtime | i18n | Runtime translation service and merge support. | Angular compile-time i18n or custom loader only. | Replace if runtime backend overrides move to a custom signal service. |
| `@ngx-translate/http-loader` | runtime | i18n | Loads local translation JSON over HTTP. | Static imports only. | Remove if local bundles are bundled or custom-loaded. |
| `keycloak-angular` | runtime | auth | Angular provider integration for Keycloak. | Manual `keycloak-js` lifecycle wiring. | Keep while Angular auth integration uses this wrapper. |
| `keycloak-js` | runtime | auth | Browser OIDC client for Keycloak login/logout/token handling. | Manual OAuth/OIDC flow. | Keep while Keycloak remains the auth provider. |

## Dependency request template

```text
Package:
Category: runtime | dev | test | build
Owner capability:
Problem solved:
Why framework built-ins are not enough:
Alternatives considered:
Risks:
Removal/replacement trigger:
```
