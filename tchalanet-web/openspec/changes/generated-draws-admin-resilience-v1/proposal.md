# Generated draws admin resilience v1

## Goal

Keep generated-draws result and lifecycle mutations on the shared async error contract,
including the result drawer's error surface.

## Decisions

- Continue using `resourceErrorVm` for the generated-draws list and `tchMutation` for writes.
- Do not replace normalized mutation feedback with feature-local raw error prose.
- Keep the result drawer as the UI owner for result-save failures and the list surface as the
  owner for lifecycle failures.

## Non-goals

- Changing generated-draws API contracts, draw lifecycle rules, or result validation.
- Reworking the existing generated-draws i18n and component structure.
