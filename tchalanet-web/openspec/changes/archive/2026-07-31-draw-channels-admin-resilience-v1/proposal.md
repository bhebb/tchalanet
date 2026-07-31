# Draw channels admin resilience v1

## Goal

Make the tenant draw-channel page and configuration dialog consume the shared web error
contract consistently while preserving the current provider configuration flow.

## Decisions

- Provider loading remains a page-owned error because the provider grid cannot render without
  the provider response.
- Provider configuration save failures remain owned by the configuration dialog section.
- Keep the existing local `en`, `fr`, and `ht` draw-channel copy and only change error mapping.

## Non-goals

- Replacing the current mock data access with the future backend endpoint.
- Changing provider, slot, cutoff, or result-acquisition semantics.
