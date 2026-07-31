# Financials report resilience v1

## Why

The financial report swallowed API errors and replaced them with a fixed message,
while also allowing the generic shell feedback to be emitted by the API call.

## Scope

- Preserve the normalized page error in the report state.
- Render the normalized title and message in the existing error panel.
- Keep the report as the owner of its page-level feedback.

## Out of scope

- Changes to financial calculations, filters, or report layout.
