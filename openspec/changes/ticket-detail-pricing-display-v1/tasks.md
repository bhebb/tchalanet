# Tasks

- [x] Define the ticket-detail pricing view and grouping contract.
- [x] Expose persisted settlement terms in the POS detail response.
- [x] Expose persisted line result status and applied payout in the POS feature response.
- [x] Group ticket lines by game in the admin detail and render pricing through an accessible tooltip.
- [x] Add backend and web tests for override/default sources and multiple variants.
- [x] Run focused validation and update this task list.

Implementation notes:

- Winning lines display a translated applied-barème label only when `resultStatus` is `WON`.
- The tooltip always reads the immutable `pricingTerms` snapshot and never current configuration.
- Exact winning-rule identification is deferred until the backend persists an applied rule code; the
  UI must not infer it from the order of pricing terms.

Validation note: the server feature module compiles; `PosTicketMapperTest` passes; web-console
tests (66) pass. The admin-portal build is currently blocked by a reproducible esbuild internal
deadlock without TypeScript/template diagnostics. The broader E2E happy-path suite still has a
pre-existing scheduler/result timing failure where a generated draw remains `CLOSED` instead of
reaching `RESULTED`.
