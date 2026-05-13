# Spec — Sell Command Handler

## Requirement

Sell command handler SHALL run after operational context is resolved or perform resolution as first application step.

## Rules

- Transactional via `@TchTx`.
- Domain/business validations remain in sales/domain services.
- Publish events after commit.
- Do not call terminal/outlet/session internals.
- Use APIs/queries only.
