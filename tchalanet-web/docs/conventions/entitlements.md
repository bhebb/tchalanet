# Entitlements Convention

> Status: DRAFT v0.1
> Scope: tenant/plan entitlements exported to the client for UI affordances
> Living doc — update in the same commit as any code that changes a rule here.

## Rule

Entitlements answer: **"is this tenant/plan allowed to use it?"** Enforcement is **authoritative on
the backend** (`@RequiredQuota`, `PlanLimitKeys`, `402/403` responses). The web holds only the
**subset we choose to export**, and only for UI affordances — show/hide a link, enable/disable a
button. The client never treats an entitlement as the real authorization.

Entitlements are **not** feature flags. See [`feature-flags.md`](./feature-flags.md).

## Placement

```text
apps/tch-portal/src/app/core/entitlement/   // EntitlementsStore
libs/shared-config/src/lib/settings/        // private settings source
```

## Runtime Path

There is currently no dedicated entitlement endpoint. Exported UI entitlements are boolean values
under `entitlement.*`, loaded through:

```http
GET /api/v1/tenant/settings/resolve
```

The path is owned by `API_PATHS.settings.tenantResolve` in `@tch/shared-config`. A future dedicated
entitlement endpoint may replace the store source, but must not change feature call sites.

## API

```ts
EntitlementsStore.has(key)        // 'payouts' or 'entitlement.payouts'
EntitlementsStore.entitlements()  // ReadonlySet<string> of granted keys (signal)
```

Reactive: backed by signals, so views update once data resolves.

## Source

Today: the `entitlement.*` runtime settings namespace (boolean values), loaded by the **private**
settings bootstrap. `entitlement.payouts = true` ⇒ `has('payouts')` is true. The prefix is stripped.

This can move to a dedicated endpoint later without changing call sites — keep all reads going through
`EntitlementsStore`.

## Usage

For a link that needs an entitlement (often together with a feature flag), use the combined gate
rather than calling `has()` in templates:

```text
*tchCan="{ feature: 'web.payouts', entitlement: 'payouts' }"   // see access.md
```

Direct `EntitlementsStore.has()` is for component logic, not template `&&` chains.

## Degradation

Because the backend is authoritative, paid UI must **degrade safely on a backend `402/403`** even if
the exported entitlement said yes (stale/missing data). The exported entitlement is a UI hint, not a
guarantee.

## Anti-patterns

Do not:

- treat a client entitlement as real authorization;
- express entitlements as `feature.*` flags;
- export the full entitlement model — export only what the UI needs;
- read the `entitlement.*` settings namespace directly — go through `EntitlementsStore`.
