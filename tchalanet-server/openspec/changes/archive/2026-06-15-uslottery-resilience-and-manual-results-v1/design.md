# Design — US Lottery Providers Resilience & Manual Results V1

## Layer placement

| Component | Layer | Module |
|---|---|---|
| `UsLotteryExternalResultsFetchAdapter` | `core.uslottery` infra | existing |
| `ProviderClientRegistry` | `core.uslottery` infra | existing |
| `ExternalResultFetchBundle.empty(...)` | `core.drawresult` api | existing class, new method |
| Fetch handler (active game filtering) | `core.drawresult` internal | existing handler |
| Kill-switch endpoints | `features.ops` | existing feature |
| Manual result permission + trust routing | `core.drawresult` internal | existing handler extension |
| New provider clients + mappers | `core.uslottery` infra | new per provider |

## Contracts

### ExternalResultFetchBundle — new factory method

```java
public static ExternalResultFetchBundle empty(
    String provider,
    LocalDate drawDate,
    LocalTime drawTime,
    ZoneId timezone
) {
    return new ExternalResultFetchBundle(provider, drawDate, drawTime, timezone, List.of(), null);
}
```

### ProviderClientRegistry — new lookup method

```java
public Optional<UsLotteryProviderClient> find(UsLotteryProvider provider) {
    return Optional.ofNullable(clients.get(provider));
}
```

### Adapter fail-soft contract

`UsLotteryExternalResultsFetchAdapter.fetchProviderResults(query)` returns `empty(...)` (never throws) when:
- provider string is null, blank, or unknown enum value
- query gameCodes is null or empty
- no client registered for the resolved provider
- client.fetch() throws
- mapper throws at top-level

Log level: `WARN` for unknown/missing/failed; `INFO` for empty game codes.

### source_cfg active game codes extraction

Utility: `activeExternalGameCodes(JsonNode sourceCfg)` in `core.drawresult`.

Rules:
- For each known slot key (`pick3`, `pick4`): extract `game_code` if `active` is absent or `true`.
- Missing `active` field → treated as `true` (backward-compatible).
- Malformed node or missing `game_code` → skip that entry, log WARN.
- Returns `List<String>` of active game codes (may be empty).

### Fetch handler loop (Slice 2)

```text
for each slotToFetch:
  if !slot.active → continue

  gameCodes = activeExternalGameCodes(slot.source_cfg)
  if gameCodes.empty → continue

  try:
    bundle = externalResultsFetchPort.fetchProviderResults(query with gameCodes)
    processBundle(bundle)
  catch:
    log WARN
    continue
```

### Kill-switch endpoints (Slice 3)

All endpoints: `POST`, platform/ops only, `@PreAuthorize` permission check, audited.

```
POST /platform/ops/result-slots/{slotKey}/disable
POST /platform/ops/result-slots/{slotKey}/games/{gameKey}/disable
POST /platform/ops/draw-channels/{channelCode}/disable
```

`gameKey` = `pick3` or `pick4` (the source_cfg key, not the game_code value).

Implementation: direct update of existing `result_slot` / `draw_channel` rows. No new table or column.

New permissions:
```
RESULT_SLOT_DISABLE
RESULT_SLOT_GAME_DISABLE
DRAW_CHANNEL_DISABLE
```

### Provider expansion contract (Slice 4)

Minimum viable provider entry:
1. Add to `UsLotteryProvider` enum.
2. Add YAML block in `application-uslottery.yml`:

```yaml
tch:
  us-lottery:
    providers:
      pa:
        enabled: true
        timezone: America/New_York
        base-url: https://...
        transport: OFFICIAL_RSS
```

If no client is implemented: provider is enabled in config but no `UsLotteryProviderClient` is registered → adapter returns empty bundle → results fall through to manual proposal.

Full implementation (client + mapper) applied where transport is reliable:
- PA: RSS → full client + mapper (first candidate).
- MO, CA, NJ: internal JSON / HTML → client + mapper if endpoint is stable.
- IL, MI, OH: enum + YAML only if transport not yet confirmed.

Mapper resilience pattern required for every new mapper:
```java
// per-entry catch: log WARN + skip; never throw from mapEntriesSafely(...)
```

### Manual result trust routing (Slice 5)

New permission: `DRAW_RESULT_MANUAL_PROPOSE` — assignable to tenant admin role.

Flow in `RecordManualDrawResultCommandHandler` (or a dedicated wrapper):

```text
caller has DRAW_RESULT_MANUAL_PROPOSE
  → load result_slot, read source_cfg.trust_policy
  → AUTO_CONFIRM_HIGH_CONFIDENCE  → DrawSource.MANUAL, status=CONFIRMED
  → REQUIRE_PLATFORM_REVIEW       → DrawSource.MANUAL, status=PROVISIONAL
  → trust_policy absent           → PROVISIONAL (safe default)

UPSERT key: (result_slot_id, occurred_at)
  → status=CONFIRMED already + force=false → reject (409 or domain exception)
  → status=PROVISIONAL already → overwrite (same key, new proposal)
```

Platform admin confirms a PROVISIONAL manual result via existing:
```
POST /platform/ops/draw-results/{id}/override   (force=true, audited)
```
or a new lightweight confirm endpoint if override semantics are too broad.

No new table. No separate proposal state machine.

## source_cfg contract (addendum)

Fields `source_type` and `trust_policy` are optional for existing slots. New provider slots should include them.

```json
{
  "provider_slot_code": "MIDDAY",
  "pick3": { "game_code": "PICK3", "active": true },
  "pick4": { "game_code": "PICK4", "active": true },
  "source_type": "OFFICIAL_RSS",
  "trust_policy": "AUTO_CONFIRM_HIGH_CONFIDENCE"
}
```

Valid `source_type`: `OFFICIAL_RSS`, `OFFICIAL_JSON`, `OFFICIAL_INTERNAL_JSON`, `OFFICIAL_HTML`, `MANUAL`
Valid `trust_policy`: `AUTO_CONFIRM_HIGH_CONFIDENCE`, `REQUIRE_PLATFORM_REVIEW`, `MANUAL_ONLY`

`trust_policy` absent in source_cfg → treat as `REQUIRE_PLATFORM_REVIEW`.

## DB (user-managed)

No Flyway migration created by the agent. Schema changes if any (e.g. source_cfg column already exists) are applied directly by the user.

`draw_result_proposal` table is out of scope for this V1.

## Platform confirm endpoint

```
POST /platform/ops/draw-results/{id}/confirm
```

**Why not `/override`:**
`/override` sets the existing result to `status=OVERRIDDEN` and creates a brand-new `draw_result` record.
That is the right path when the *numbers are wrong and need to be replaced*.

Here the numbers are already correct — a tenant admin wrote them and they are pending review.
`/confirm` just flips `PROVISIONAL → CONFIRMED` in place on the same record.
Using `/override` here would create a duplicate record with identical numbers, polluting the audit trail.

**Behavior:**
- Input: `draw_result.id` (path), optional `reason` (body).
- Effect: `status = CONFIRMED`, `updated_at = now()`, `version++`.
- Precondition: current status must be `PROVISIONAL` — any other status → 400.
- Audited.
- Permission: `DRAW_RESULT_CONFIRM` (platform only).

**Not a replacement for `/override`:**
If a platform admin disagrees with the numbers, they use `/override` as usual (creates corrected record, marks old one OVERRIDDEN).
