# What to verify before implementation merge

## Architecture

- [ ] `core.sales` imports only `core.promotion.api.*`, never `core.promotion.internal.*`.
- [ ] `core.promotion` imports only `core.sales.api.event.*` if event listener option is enabled; never `core.sales.internal.*`.
- [ ] API packages contain only externally consumed commands/queries/models.
- [ ] Handlers stay under `internal.application.*`.
- [ ] JPA/repositories/mappers stay under `internal.infra.persistence`.

## Persistence

- [ ] Every tenant table has BaseTenantEntity columns.
- [ ] Java entities extend `BaseTenantEntity`.
- [ ] `_aud` tables exist or Envers DDL generation is verified.
- [ ] RLS enabled and policies applied.
- [ ] Repositories do not manually filter `tenant_id`.
- [ ] Repositories do not manually filter `deleted_at` by default.

## Sales behavior

- [ ] SMS waiver changes charges/money only.
- [ ] Maryaj gratuit creates TicketLine origin=PROMOTION.
- [ ] Boost odds modifies oddsSnapshot/potentialPayout before save.
- [ ] `payoutBaseAmount` is used for gain calculation, not only `stakeAmount`.
- [ ] Print can display promotion lines from `ticket_line` snapshots.

## Settlement/Payout

- [ ] Settlement reads `TicketLine` snapshots and does not call PromotionConfig.
- [ ] Payout pays settled amount only.

## Events/Stats/Ledger

- [ ] TicketPlacedEvent line payload includes origin/pricingSource/promotionDecisionId in the future event migration.
- [ ] Ledger/stats can distinguish paid vs promotional exposure.
- [ ] AppliedPromotionSnapshot command is idempotent.

## Quota

- [ ] Preview does not consume quota.
- [ ] Confirmation consumes quota atomically with row lock/update.
- [ ] Exhausted quota returns a deterministic non-applied decision or sale issue.
