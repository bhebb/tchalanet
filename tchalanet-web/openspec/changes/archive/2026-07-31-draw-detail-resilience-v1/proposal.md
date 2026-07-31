# Draw detail resilience v1

## Why

The generated-draw detail page still presents raw hardcoded error messages for the
draw request, optional activity requests, and manual result saving.

## Scope

- Normalize page, section, and form errors through the shared web error contract.
- Keep activity and top-selection failures non-blocking for the draw detail page.
- Preserve the drawer's actionable save feedback while using normalized server copy.

## Out of scope

- Changing the draw-detail layout or API contracts.
- Translating the existing operational domain labels in this slice.
