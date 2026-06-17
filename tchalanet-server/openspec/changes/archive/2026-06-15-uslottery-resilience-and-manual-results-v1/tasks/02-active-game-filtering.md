# Task 02 — Active Game Filtering

## Scope

`core.drawresult` internal — fetch handler + utility

## Changes

1. Add utility `activeExternalGameCodes(JsonNode sourceCfg) → List<String>`:
   - Iterates known game keys (`pick3`, `pick4`).
   - Includes game_code if `active` absent or `true`.
   - Skips if `active=false` or `game_code` missing.
   - Returns empty list if sourceCfg is null or malformed (log WARN).

2. Update `FetchExternalResultsWindowCommandHandler` loop:
   - After `slot.active` check, extract active game codes via utility.
   - If list empty → skip slot (log INFO).
   - Pass only active game codes in `ExternalResultFetchQuery`.

## Tests

- `pick3.active=false`, `pick4.active=true` → only pick4 game code included.
- Both active → both included.
- Both `active=false` → slot skipped, no provider call.
- `active` key absent → treated as true.
- `source_cfg` null or malformed → empty list, no throw.

## Acceptance

- No provider fetch call when gameCodes is empty.
- Existing slots without `active` field in source_cfg continue to work unchanged.
