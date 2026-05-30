# Frontend Foundation Rules — Web + Mobile

## Status

Normative for the initial Web Angular/Nx and Mobile Flutter/POS foundation changes.

## Core decision

Tchalanet frontend V1 starts with runtime proof, not a large abstract design system.

The validated order is:

0. backup / clean base
1. transverse contracts/types
2. minimal technical setup
3. Keycloak/auth proof
4. runtime bootstrap: settings + i18n + theme + PageModel where applicable
5. minimal reusable UI core
6. layout shells
7. public home via runtime bootstrap
8A. tenant admin Web
8B. Flutter POS
9. cashier Web only if needed for V1
10. minimal Superadmin onboarding first, full Superadmin later

## Separation of runtime capabilities

Bootstrap runtime is made of four separate capabilities:

1. settings
2. i18n
3. theme
4. PageModel

PageModel must not contain full i18n, full theme, or full settings. PageModel contains structure, widgets, actions, destinations, and i18n keys.

i18n owns translations. Theme owns tokens/presets/mode. Settings owns runtime flags/config, including simple V1 feature toggles before a future Unleash migration.

## i18n merge rule

Frontend local translations are the stable fallback.
Backend overrides are loaded at runtime and have priority when a key exists in both sources.

```text
merged = deepMerge(frontendLocal, backendOverrides)
```

## Theme rule

Tchalanet default theme is the default. Admin tenant may later choose from Material-equivalent presets. Custom theme builder is out of V1 but must remain possible.

## Dependency governance

Every dependency must have:

- purpose;
- owner area;
- runtime/build/dev category;
- alternative considered;
- reason for inclusion;
- removal/replacement trigger.

No dependency should be added only for convenience if framework primitives are enough.

## Lint / quality gate decision

Lint and format should be configured early, but not as a blocking pre-commit hook on day one if the existing workspace is not clean.

Recommended progression:

1. add commands: `lint`, `format`, `test`, `analyze` where applicable;
2. make CI/manual verification reliable;
3. after baseline is clean, add pre-commit hooks;
4. keep hooks fast: format + lint only, no long e2e/performance tests in pre-commit.

