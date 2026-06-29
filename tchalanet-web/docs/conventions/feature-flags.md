# Feature Flags Convention

> Status: DRAFT v0.1
> Scope: feature management, isolation seam, future Unleash handoff
> Living doc — update in the same commit as any code that changes a rule here.

## Rule

Feature flags answer one question: **"is this capability built / turned on?"** They are resolved
through the **`FeatureFlags` seam** (`core/feature`), never by reading settings directly. This keeps
feature management isolated so a future provider swap (e.g. Unleash) rebinds one token and touches no
call site.

Flags are **not** entitlements. "Is this tenant/plan allowed to use it?" is an entitlement — see
[`entitlements.md`](./entitlements.md). Never overload a `feature.*` flag to mean "paid".

## The seam

```text
FeatureFlags            // abstract token — the only thing call sites depend on
  SettingsFeatureFlags  // default impl: reads the `feature.*` runtime settings namespace
```

Bound in `app.config.ts`:

```ts
{ provide: FeatureFlags, useExisting: SettingsFeatureFlags }
```

`SettingsFeatureFlags` is the **only** place that knows flags come from settings. To move to Unleash:
add an `UnleashFeatureFlags implements FeatureFlags` and rebind the token. Done.

## Placement

```text
libs/shared-config/src/lib/feature/   // FeatureFlags seam + impl
```

The structural directive and route guard live in `core/runtime`:

```text
*tchFeature="'web.public.demo_enabled'"          // show/hide content
*tchFeature="'web.x'; default: true; else tpl"
featureGuard('web.x', { redirectTo: '/forbidden' })  // route gate
```

For a feature + entitlement combination, use the combined gate in [`access.md`](./access.md), not a
template `&&`.

## Source: settings namespace

Today flags are the boolean settings under `feature.*` / `features.*` / `feature_flags.*`, loaded by
the public/private runtime bootstrap (see [`settings.md`](./settings.md)). Because settings are a
signal, gated views re-render once settings resolve.

## Resolution timing

A guard evaluated before settings load falls back to its `default`. For routes that must hard-gate a
not-ready feature, use a conservative `false`.

## Anti-patterns

Do not:

- read `settings().featureFlags` (or `RuntimeSettingsStore.isFeatureEnabled`) from a component —
  depend on `FeatureFlags`, `*tchFeature`, or `featureGuard`;
- use a feature flag to express a paid/entitlement gate;
- scatter `&&` of flag + entitlement across templates — use `*tchCan` / `can` pipe.
