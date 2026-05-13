# Tchalanet Module Decision Tree

## Where does this code belong?

1. Pure technical primitive used by many modules, no state, no product capability?
   -> `common`

2. Reference/read-mostly data, no lifecycle, no business side effects?
   -> `catalog`

3. Transversal application capability with state/workflow but not core business-critical game/money decision?
   -> `platform`

4. Business-critical invariant/lifecycle/transaction decision for sales, draw, payout, ticket, limit, settlement, terminal/session/outlet operations?
   -> `core`

5. UI screen/flow/BFF composition?
   -> `features`

## Context terminology

- Runtime request context -> `common.context`
- Persistent user/profile/membership -> `platform.identity`
- Permissions/roles -> `platform.accesscontrol`
- Terminal/outlet/session validation for an operation -> operational context resolver
