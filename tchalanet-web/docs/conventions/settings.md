# Settings Convention

> Status: DRAFT v0.1  
> Scope: runtime settings, feature flags, future Unleash handoff

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

## Future Unleash Boundary

Feature checks should go through a small helper such as:

```text
isFeatureEnabled(key, defaultValue)
```

This keeps the call site stable if the source later changes from runtime settings to Unleash.

Do not spread direct reads of `settings().featureFlags` across pages.

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
