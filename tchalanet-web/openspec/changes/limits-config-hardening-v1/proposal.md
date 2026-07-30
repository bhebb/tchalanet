# Limites config hardening v1

## Goal

Make the tenant limits console consistent with the web error contract and the admin i18n
conventions across the overview, global, number, draw-channel, seller-terminal, system
catalog, and upsert-dialog surfaces.

## Decisions

- Normalize every HTTP failure through `mapHttpErrorToProblemDetail` before rendering it.
- Keep the existing scope boundaries: the overview summarizes policy state, child pages edit
  assignments, and the system page documents and simulates effective rules.
- Keep backend-provided rule labels and descriptions as data; translate console actions, states,
  empty states, and field copy locally in `feature-admin`.
- Provide local fallback copy for `en`, `fr`, and `ht`.

## Non-goals

- Changing limit rule semantics or backend precedence.
- Changing the assignment API contracts.
