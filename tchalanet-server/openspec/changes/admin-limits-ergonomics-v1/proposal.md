# Change: admin-limits-ergonomics-v1

## Why

The tenant admin limits overview needs a backend-owned business projection. The web app should not assemble the overview by calling rule catalog and assignment endpoints directly.

## What Changes

- Add a tenantadmin policies overview endpoint.
- Keep PageModel responsible for shell/navigation, not interactive limits data.
- Keep detailed limits mutation/read endpoints in the existing limit policy API.
- Simplify tenant admin limit creation around the real business inputs:
  selection/number, limit amount or block action, scope, and duration.
- Remove potential-payout limit definitions from the V0 admin rule catalog until
  real payout calculation is exposed as an admin-facing report/simulation.
- Move long definitions and educational copy to the rules/support page, with a
  simulation tab to inspect effective rules.
- Confirm the post-outlet runtime contract: tenant, draw channel, seller
  terminal, and agent are supported scopes; outlet is not exposed in tenant
  admin limits V0.

## Impact

- New endpoint: `GET /admin/policies/overview`.
- Web overview can consume one stable BFF contract.
- Web number limits should become action-first and avoid long explanatory cards.
- Web/admin rule definitions should not surface potential-payout wording in V0.
- Draw-channel and seller-terminal limit pages must pass backend IDs, not display
  codes, when reading or writing scoped assignments.
