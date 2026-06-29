# Access Gate Convention

> Status: DRAFT v0.1
> Scope: combined feature + entitlement gating (directive, pipe, guard)
> Living doc — update in the same commit as any code that changes a rule here.

## Rule

When something needs **both** a feature flag and an entitlement, do not write
`*ngIf="isFeatureEnabled('x') && hasEntitlement('y')"`. Use the combined access gate (`core/access`),
which folds [`feature-flags.md`](./feature-flags.md) + [`entitlements.md`](./entitlements.md) into one
reactive decision. Either part is optional.

## API

```ts
interface AccessRequirement {
  feature?: string;         // feature flag key
  featureDefault?: boolean; // value while the flag is unresolved (default false)
  entitlement?: string;     // exported entitlement key
}

AccessService.can(req): boolean   // featureOk && entitlementOk
```

`can` is `true` when each provided part passes; an omitted part is treated as satisfied.

## Three call shapes

```text
// 1. Structural directive — show/hide an element
<a *tchCan="{ feature: 'web.payouts', entitlement: 'payouts' }">Payouts</a>
<a *tchCan="{ feature: 'web.x' }; else off">…</a>
<ng-template #off>…</ng-template>

// 2. Pipe — binding contexts (disabled, aria, @if) where a structural directive doesn't fit
<button [disabled]="!({ feature: 'web.payouts', entitlement: 'payouts' } | can)">…</button>
@if ({ feature: 'web.x' } | can) { … }

// 3. Route guard
{ path: 'payouts', canActivate: [accessGuard({ feature: 'web.payouts', entitlement: 'payouts' })] }
```

Pick the directive to add/remove DOM, the `can` pipe for attribute/`@if` bindings, the guard for
routes.

## Placement

```text
libs/core/auth/src/lib/access/   // AccessService, CanDirective, CanPipe, accessGuard
```

## Notes

- The `can` pipe is impure (re-evaluates each change detection) because the decision reads signals;
  the check itself is a cheap flag/set lookup.
- Guards run before async settings resolve — they use `featureDefault` (conservative `false` by
  default) and a missing entitlement denies. Use for soft gating; the backend stays authoritative.
- Feature-only gating still has the lighter `*tchFeature` / `featureGuard`; reach for `*tchCan` only
  when an entitlement is also involved.

## Anti-patterns

Do not:

- chain `feature && entitlement` in templates — that is exactly what this gate removes;
- use the access gate as real authorization (entitlements are backend-authoritative);
- bypass the gate by reading `FeatureFlags` / `EntitlementsStore` inline in a template.
