# Task 03 — Kill Switch Ops Endpoints

## Scope

`features.ops` — new platform-only write endpoints

## Endpoints

```
POST /platform/ops/result-slots/{slotKey}/disable
POST /platform/ops/result-slots/{slotKey}/games/{gameKey}/disable
POST /platform/ops/draw-channels/{channelCode}/disable
```

`gameKey` = `pick3` or `pick4` (the source_cfg key, not the game_code value).

All three: `POST`, no request body required (or optional `reason` string).

## Behavior

- `disable result-slot` → sets `result_slot.active=false`, increments `version`, sets `updated_at`.
- `disable result-slot game` → `jsonb_set(source_cfg, '{pick3,active}', 'false')` (or pick4), increments `version`.
- `disable draw-channel` → sets `draw_channel.active=false`, increments `version`.

Each write is audited via the existing audit mechanism.

## Security

- `@PreAuthorize` on each endpoint.
- Permissions: `RESULT_SLOT_DISABLE`, `RESULT_SLOT_GAME_DISABLE`, `DRAW_CHANNEL_DISABLE`.
- Register new permissions in access control.
- No manual role checks in controller bodies.

## Tests

- Each endpoint: authorized call → row updated, audit written.
- Unauthorized call → 403.
- `gameKey` not `pick3` or `pick4` → 400.
- `slotKey` or `channelCode` not found → 404.

## Acceptance

- No new table or column.
- All writes produce an audit entry.
- Disabling an already-disabled slot/channel is idempotent (no error).
