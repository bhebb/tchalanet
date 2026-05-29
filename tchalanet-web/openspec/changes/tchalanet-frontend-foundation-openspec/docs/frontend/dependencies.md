# Web Dependencies Governance

## Rule

Do not add packages to `package.json` without documenting them here.

Each dependency must answer:

- What problem does it solve?
- Is it runtime, build, test, or dev-only?
- Which capability owns it?
- What built-in alternative was considered?
- What would make us remove or replace it?

## Initial expected dependencies

| Package | Category | Owner capability | Purpose | Alternative considered | Keep / remove trigger |
|---|---|---|---|---|---|
| `@angular/material` | runtime | UI/theme | Material components and theme integration | Custom components only | Keep while Angular Material is the UI base |
| `@ngrx/store` | runtime | state | Global app state: auth/session, settings, theme, i18n status, shell | Angular signals/services only | Keep if multiple surfaces share state |
| `@ngrx/effects` | runtime | state/API | Runtime loading, auth/bootstrap effects | Services manually called by components | Keep if orchestration stays in effects |
| `@ngrx/store-devtools` | dev/runtime dev-only | state | Debug state in dev | Console logging | Dev only; never enable in prod |
| Keycloak client/wrapper | runtime | auth | OIDC login/logout/token integration | Manual OAuth flow | Keep while Keycloak is auth provider |
| Translation package or custom loader | runtime | i18n | Runtime language switching and merge | Simple JSON import only | Keep if runtime overrides are needed |
| ESLint/Angular ESLint | dev | quality | Lint Angular/TS code | TypeScript compiler only | Keep as quality gate |
| Prettier/formatter | dev | quality | Consistent formatting | Editor-only formatting | Keep if team accepts format standard |
| Husky/lint-staged or equivalent | dev | quality | Optional pre-commit fast checks | Manual lint before commit | Enable only after clean baseline |

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
