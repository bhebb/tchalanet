# Spec — PromoteOfflineSubmissionCommandHandler

## Responsibility

Promote a technically valid offline submission into a real sale/ticket through `core.sales`.

## Steps

1. Load submission.
2. Ensure status allows promotion.
3. Resolve seller operational context.
4. Build sales command/API request.
5. Execute sales command/API.
6. Mark submission `PROMOTED_TO_SALES` with ticket id/result.
7. Publish after-commit event.

## Must not do

- Write directly to sales tables.
- Bypass seller operational context.
- Suppress sales business validation errors.
