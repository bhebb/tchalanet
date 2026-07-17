# Result Provider Notification Reminders

## Why

Some result slots are fulfilled by the `core.uslottery` automatic provider clients, while others
must be entered manually by platform/tenant operators. Today the scheduler can try to fetch every
active result slot that has source metadata, but ops does not get an explicit reminder for slots
that are known/manual, and users are not notified when a result finally arrives.

This creates an operational gap after draw time: manual slots can sit without results, and tenants
do not get a clear "result is available" signal.

## What Changes

- Define a deterministic result-slot source classification:
  - automatic when an active slot has source config and a real `uslottery` client capability is
    registered;
  - manual when no real provider client exists or the configured client explicitly reports no-op
    capability;
  - inactive slots are excluded from runtime reminders.
- Add one operational notification type, `DRAW_RESULT_ACTION_REQUIRED`, with two reasons:
  `MANUAL_ENTRY_REQUIRED` and `AUTOMATIC_FETCH_OVERDUE`.
- Add `ResultSource` metadata for global draw results: `PROVIDER`, `MANUAL_ENTRY`,
  `MANUAL_OVERRIDE`.
- Send the operational notification and internal Slack projection for manual slots missing a result
  5 minutes after draw time, and automatic slots still missing a result 60 minutes after draw time.
- Publish `GlobalDrawResultAvailableEvent` when a global draw result becomes available, resolve any
  open operational notification for that slot/date, and publish separate tenant result-available
  notifications.
- Publish `GlobalDrawResultCorrectedEvent` when an existing global result is overridden, without
  re-sending tenant result-available notifications.
- Notify affected tenant audiences only when tenant channel configuration says the tenant is
  subscribed to the exact channel/slot and a matching non-cancelled tenant draw exists.
- Keep notification persistence in `platform.notification` and external Slack delivery in
  `platform.communication`; Slack is a projection of notification state, not a second source of
  truth.

## Seed/Client Audit

Current seed and client registration audit from:

- `tchalanet-app/src/main/resources/db/migration/V204__seed_core_game_draw.sql`
- `tchalanet-core/src/main/java/com/tchalanet/server/core/uslottery/internal/infra/registry/ProviderClientRegistry.java`
- `tchalanet-core/src/main/java/com/tchalanet/server/core/uslottery/internal/infra/external/**`

| Provider | Seeded active slots | Seeded inactive slots | Registered client status | Runtime class |
| --- | ---: | ---: | --- | --- |
| NY | 2 | 0 | automatic | `NewYorkDrawResultsClient` |
| FL | 2 | 0 | automatic | `FloridaDrawResultsClient` |
| GA | 3 | 0 | automatic | `GeorgiaDrawResultsClient` |
| TX | 4 | 0 | automatic | `TexasDrawResultsClient` |
| PA | 2 | 0 | automatic | `PennsylvaniaDrawResultsClient` |
| NJ | 2 | 0 | automatic | `NewJerseyDrawResultsClient` |
| CA | 2 | 0 | automatic | `CaliforniaDrawResultsClient` |
| OH | 2 | 0 | automatic | `OhioDrawResultsClient` |
| MI | 2 | 0 | automatic | `MichiganDrawResultsClient` |
| MN | 1 | 0 | manual/no-op until implemented | `MinnesotaDrawResultsClient` returns empty |
| TN | 0 | 2 | inactive/manual if activated | no client registered |
| IL | 0 | 2 | inactive/manual if activated | no client registered |
| MO | 0 | 2 | inactive/manual if activated | no client registered |

Important nuance: all seeded slots currently carry a `source_cfg` with provider slot/game metadata.
That metadata alone is not enough to call the slot automatic. The runtime decision must also verify
that a real provider client exists and reports automatic-fetch capability. No-op clients must expose
that capability explicitly; runtime code must not detect no-op clients with `instanceof` or class
name checks.

## Impact

- Backend only for this change.
- New tests should cover the classifier, manual reminders, automatic-provider-overdue reminders,
  notification expiration/resolution, notification idempotency, and Slack bridge idempotency.
- No Flyway schema change is expected for the spec unless implementation discovers that reminder
  idempotency cannot reuse existing notification/communication correlation mechanisms.

## Non-Goals

- Do not implement new external provider clients in this change.
- Do not change result apply/settle semantics.
- Do not send Slack directly from drawresult or scheduler code.
- Do not notify for inactive slots unless an operator activates them.
- Do not notify tenants merely because automatic provider fetch is late; that is a platform-ops
  alert.
- Do not say tickets are settled in result-available tenant notifications.
- Do not re-send "result available" notifications when an existing result is corrected/overridden.
- Do not derive tenant audiences from global result existence alone or from ad hoc lists inside
  `core.drawresult`.
