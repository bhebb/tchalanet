# Architecture Rule — Entitlement Review Before New API/Handler

## Rule

Before implementing a new API endpoint, command handler, or business action, ask:

```text
Does this action depend on a paid feature, plan capability, or quota?
```

## If no

Document no entitlement needed if the action is obviously plan-neutral.

Examples:

- Read own profile.
- View public results.
- Basic ticket lookup already included in all plans.

## If yes

Apply the appropriate layer:

```text
HTTP boundary:
  @RequiredFeature / @RequiredQuota

Handler/application:
  Recheck for critical actions or non-HTTP entrypoints.

Page/BFF:
  Hide or disable unavailable actions.
```

## Critical actions that should recheck in handler

- terminal create
- outlet create
- user create
- mobile device binding
- offline grant/sync/sell
- promotion create/update
- payout approval workflow
- external delivery/email/SMS
- exports with row limits
