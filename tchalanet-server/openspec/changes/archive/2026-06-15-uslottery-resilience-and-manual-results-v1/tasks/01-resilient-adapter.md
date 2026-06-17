# Task 01 — Resilient Adapter

## Scope

`core.uslottery` + `core.drawresult` api

## Changes

1. Add `ExternalResultFetchBundle.empty(String, LocalDate, LocalTime, ZoneId)`.
2. Add `ProviderClientRegistry.find(UsLotteryProvider)` returning `Optional<UsLotteryProviderClient>`.
3. Rewrite `UsLotteryExternalResultsFetchAdapter.fetchProviderResults(query)` to fail-soft:
   - unknown/null provider → log WARN → return empty
   - empty/null gameCodes → log INFO → return empty
   - no client registered → log WARN → return empty
   - client.fetch() throws → log WARN → return empty
   - mapper throws at top-level → log WARN → return empty

## Tests

- Unknown provider string → empty bundle, no throw.
- Null/blank provider → empty bundle, no throw.
- No client registered → empty bundle, no throw.
- Client throws → empty bundle, no throw.
- Empty gameCodes → empty bundle, no throw.
- Happy path → bundle with results.

## Acceptance

- No exception escapes the adapter for any of the above cases.
- Log level: WARN for failure cases, INFO for empty-game-codes skip.
