# Spec — SubmitOfflineSaleCommandHandler

## Responsibility

Receive and persist offline submissions in a temporary/offline-submission state.

## Must do

- Validate structural payload.
- Compute/check payload hash if provided.
- Deduplicate using submission id / offline ticket code / device sequence / idempotency key.
- Persist submission with status `RECEIVED` or `DUPLICATE`.
- Schedule or trigger technical validation/promotion.

## Must not do

- Create a ticket directly.
- Decide draw cutoff/pricing/limits itself.
- Call sales persistence internals.
