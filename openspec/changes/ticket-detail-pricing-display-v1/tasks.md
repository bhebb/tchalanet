# Tasks

- [x] Define the ticket-detail pricing view and grouping contract.
- [x] Expose persisted settlement terms in the POS detail response.
- [x] Group ticket lines by game in the admin detail and render effective rules.
- [x] Add backend and web tests for override/default sources and multiple variants.
- [x] Run focused validation and update this task list.

Validation note: web formatting, admin-portal type-check, web-console tests (66), and
admin-portal tests (52) pass. The server module compiles, but the focused Maven test
phase is currently blocked by pre-existing missing command classes referenced by
`PosProfileServiceTest` (`UpdateSellerTerminalCommercialCommand`,
`UpdateSellerTerminalContactCommand`, and `UpdateSellerTerminalLabelCommand`).
