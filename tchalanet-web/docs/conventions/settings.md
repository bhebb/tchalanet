# Settings Convention

> Status: DRAFT v0.2
> Scope: runtime settings, feature flags, gating, entitlements, future Unleash handoff
> Living doc — update in the same commit as any code that changes a rule here.

## Rule

Runtime settings are configuration values. Feature flags are a constrained subset of settings until a dedicated flag provider such as Unleash is introduced.

Every setting remains readable through `values`. Only explicit feature namespaces become `featureFlags`.

## Placement

```text
apps/tch-portal/src/app/core/settings/
```

Use this area for:

- public settings loading;
- private settings loading after auth;
- typed parsing of backend setting values;
- feature toggle helper methods;
- fallback behavior.

## Runtime Shape

```text
RuntimeSettings {
  values
  featureFlags
  loadedAt
}
```

`values` contains all settings by full key:

```text
<namespace>.<settingKey>
```

`featureFlags` contains only boolean settings from explicit feature namespaces:

```text
feature.*
features.*
feature_flags.*
```

## Gating lives in dedicated seams

Settings is the **source** for feature flags (`feature.*`) and exported entitlements (`entitlement.*`),
but gating logic is not documented here — it lives behind isolated seams so settings stays a plain
config store and call sites never read `settings().featureFlags` directly:

- [`feature-flags.md`](./feature-flags.md) — `FeatureFlags` seam, `*tchFeature`, `featureGuard`
  (Unleash-ready).
- [`entitlements.md`](./entitlements.md) — `EntitlementsStore`, exported subset, backend-authoritative.
- [`access.md`](./access.md) — combined `*tchCan` directive, `can` pipe, `accessGuard` (avoid `&&`).

Rule: never read `settings().featureFlags` from a component — depend on the seams above.

## Failure Behavior

Missing settings fail safely:

- unknown values return undefined or caller defaults;
- unknown feature flags return the provided default;
- API failure sets fallback state and does not crash the route.

## Anti-Patterns

Do not:

- treat every boolean setting as a feature flag;
- put business authorization in settings;
- let PageModel embed settings values;
- call settings APIs directly from feature components;
- use settings to bypass backend validation or entitlement checks.
