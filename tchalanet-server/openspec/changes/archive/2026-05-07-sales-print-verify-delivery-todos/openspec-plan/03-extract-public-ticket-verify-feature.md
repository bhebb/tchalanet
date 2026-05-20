# OpenSpec change: extract-public-ticket-verify-feature

## Why

Public ticket verification is a public/BFF exposure concern: masking, noindex, rate limit, public payout status, visibility window. It should not live as a sales domain model/handler returning domain-shaped data.

## What

- Move public verification endpoint to `features.ticketverify`.
- Keep core.sales as source of truth/read model provider.
- Add `GetPublicTicketVerificationRecordQuery` in core.sales.
- Remove/retire `TicketVerificationResult` from domain.

## Tasks

- [ ] Add core.sales query + record.
- [ ] Add feature `features.ticketverify` vertical slice.
- [ ] Add `TicketVerifyController`.
- [ ] Add `TicketVerifyService`.
- [ ] Add response models and enums.
- [ ] Add publicCode normalizer.
- [ ] Add noindex/cache-control/rate-limit.
- [ ] Tests for statuses and no internal ID leakage.

## Acceptance

- Feature does not access repositories/JPA/entities.
- Public verify never returns `ticketId`, `drawId`, `tenantId`, `addressId`, terminal UUID.
- Unknown/malformed/expired tickets return explicit statuses, never null.
