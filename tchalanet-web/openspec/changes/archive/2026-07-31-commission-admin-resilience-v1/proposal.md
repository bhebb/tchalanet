# Commission admin resilience v1

## Goal

Make the tenant commission console resilient when its overview and seller list requests
degrade independently, while keeping all visible console copy behind the admin i18n bundles.

## Decisions

- The overview and seller list are independent sections. A failure in one does not hide the
  other section.
- Every local HTTP failure is normalized through `mapHttpErrorToProblemDetail` and owned by
  the corresponding section card.
- Commission page actions keep suppressing shell feedback because the page owns their errors.
- Add local fallback copy for `en`, `fr`, and `ht` in `feature-admin`.

## Non-goals

- Changing commission rate validation or backend API contracts.
- Changing commission calculation or seller assignment semantics.
