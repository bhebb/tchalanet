# Draw result detail resilience v1

## Why

The draw-result detail page still replaces the normalized API failure with a
hardcoded message when its result lookup fails.

## Scope

- Normalize the result-detail page load error through the shared web error contract.
- Keep the existing not-found and navigation behavior unchanged.

## Out of scope

- Changes to result rendering, combination calculations, or API contracts.
