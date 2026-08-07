## ADDED Requirements

### Requirement: Result slots are classified by effective provider capability

The system SHALL classify each result slot by effective runtime source capability before automatic
fetch or operational reminder decisions.

The classification SHALL be:

- `INACTIVE` when the global result slot is inactive.
- `AUTOMATIC` when the slot is active, has active source game metadata, and a real provider client
  is registered for its provider and explicitly reports automatic-fetch capability.
- `MANUAL` when the slot is active but has no real provider client, no active source games, or only a
  provider client that explicitly reports no-op capability.

`source_cfg` SHALL NOT be treated as proof that a slot is automatic. Provider clients SHALL expose
their capability through an explicit contract such as `supportsAutomaticFetch()` or
`ProviderClientCapability.AUTOMATIC | NO_OP`. Runtime classification SHALL NOT identify no-op
clients with `instanceof` or class-name checks.

#### Scenario: Source config without client is manual

- **GIVEN** an active result slot has `source_cfg.provider_slot_code` and active game mappings
- **AND** no `uslottery` client is registered for the slot provider
- **WHEN** the processing scheduler evaluates the slot
- **THEN** the slot is classified as `MANUAL`
- **AND** automatic fetch is not attempted for that slot/date
- **AND** the slot may be considered for manual-entry action-required reminders.

#### Scenario: Real client is automatic

- **GIVEN** an active result slot uses provider `NY`
- **AND** `NewYorkDrawResultsClient` is registered and reports automatic-fetch capability
- **AND** source config has at least one active game mapping
- **WHEN** the processing scheduler evaluates the slot
- **THEN** the slot is classified as `AUTOMATIC`
- **AND** it remains eligible for automatic fetch according to the existing fetch due window.

#### Scenario: No-op client is manual

- **GIVEN** an active result slot uses provider `MN`
- **AND** the registered client reports no-op capability
- **WHEN** the processing scheduler evaluates the slot
- **THEN** the slot is classified as `MANUAL`
- **AND** platform ops gets a manual-entry reminder when no result exists after the reminder window.

#### Scenario: Inactive slot is excluded

- **GIVEN** a result slot is inactive
- **WHEN** runtime classification runs
- **THEN** the slot is classified as `INACTIVE`
- **AND** it is excluded from automatic fetch and reminder candidate searches.

### Requirement: Action-required reminders are due after draw time

The system SHALL evaluate missing-result action-required reminders for active slots after configured
delays from the slot occurrence time.

The system SHALL use notification type `DRAW_RESULT_ACTION_REQUIRED` and reason:

- `MANUAL_ENTRY_REQUIRED` for manual slots without a result after 5 minutes.
- `AUTOMATIC_FETCH_OVERDUE` for automatic slots without a result after 60 minutes.

#### Scenario: Manual slot reminder is due five minutes after draw

- **GIVEN** a manual result slot with `drawTime=18:17` and timezone `America/Chicago`
- **AND** the draw date occurrence time is `2026-07-16T18:17:00-05:00`
- **AND** no global draw result exists for that slot/date
- **WHEN** current time is before `2026-07-16T18:22:00-05:00`
- **THEN** no reminder is sent
- **WHEN** current time is at or after `2026-07-16T18:22:00-05:00`
- **THEN** a `DRAW_RESULT_ACTION_REQUIRED` notification with reason `MANUAL_ENTRY_REQUIRED` is due
- **AND** the correlation key is
  `drawresult.action-required:manual:{resultSlotId}:{drawDate}`.

#### Scenario: Automatic provider overdue is due after one hour

- **GIVEN** an automatic result slot with `drawTime=22:45` and timezone `America/New_York`
- **AND** no global draw result exists for that slot/date
- **WHEN** current time is before `occurredAt + 60 minutes`
- **THEN** no automatic-overdue reminder is sent
- **WHEN** current time is at or after `occurredAt + 60 minutes`
- **THEN** a `DRAW_RESULT_ACTION_REQUIRED` notification with reason `AUTOMATIC_FETCH_OVERDUE` is due
- **AND** the correlation key is
  `drawresult.action-required:automatic-overdue:{resultSlotId}:{drawDate}`
- **AND** automatic provider retries continue according to the existing fetch retry policy
- **AND** the slot remains classified as `AUTOMATIC`.

#### Scenario: Existing result suppresses reminder

- **GIVEN** a result slot/date already has a global draw result
- **WHEN** the reminder scheduler evaluates the slot/date
- **THEN** no action-required reminder is sent.

#### Scenario: Reminder is idempotent

- **GIVEN** an action-required reminder has already been published for `(resultSlotId, drawDate, reason)`
- **WHEN** the scheduler evaluates the same slot/date again
- **THEN** no duplicate in-app notification or Slack message is created.

### Requirement: Result reminder scheduler is orchestration only

The scheduler SHALL run missing-result reminder orchestration between result fetch and result apply.

The processing order SHOULD be:

1. close
2. fetch
3. result-reminder
4. apply
5. settle

The scheduler SHALL only verify flags/gates, find due slot/date occurrences, dispatch commands, and
log summaries. It SHALL NOT persist notifications directly and SHALL NOT call Slack directly.

The scheduler SHALL be controlled by:

```yaml
tch:
  draw:
    scheduler:
      processing:
        result-reminder:
          active: true
          manual-start-minutes-after-draw: 5
          automatic-overdue-minutes-after-draw: 60
          max-slots-per-tick: 25
```

The scheduler SHALL use batch gate `drawresult:reminder:run`.

#### Scenario: Scheduler dispatches command

- **GIVEN** a missing-result reminder candidate is due
- **AND** `drawresult:reminder:run` gate is enabled
- **WHEN** the scheduler processes the candidate
- **THEN** it dispatches `CreateMissingResultReminderCommand`
- **AND** it does not call `NotificationApi`, `platform.notification.internal`, `CommunicationApi`,
  or any Slack provider adapter directly.

### Requirement: Missing-result reminder command revalidates state

The application handler SHALL own missing-result reminder creation.

`CreateMissingResultReminderCommand` SHALL include:

- `ResultSlotId`
- `LocalDate drawDate`
- `Instant occurredAt`
- `ResultReminderReason`
- provider code
- request/correlation id for diagnostics

The handler SHALL recompute the stable notification correlation key in backend code and recheck that
the global result is still absent before creating any notification.

#### Scenario: Result arrives between scheduler and handler

- **GIVEN** the scheduler dispatched a missing-result reminder command
- **AND** the global result is created before the handler executes
- **WHEN** the handler rechecks result existence
- **THEN** it exits as a no-op
- **AND** no notification or Slack message is created.

#### Scenario: Handler creates notification through API

- **GIVEN** a missing-result reminder is still due
- **WHEN** the handler creates the alert
- **THEN** it delegates to `NotificationApi`
- **AND** it does not import or call `platform.notification.internal`
- **AND** it does not call Slack or `CommunicationApi` directly.

### Requirement: Global result availability is published once per global result

The system SHALL publish `GlobalDrawResultAvailableEvent` when a global draw result becomes
available for the first time.

Global draw results SHALL carry `ResultSource` metadata:

- `PROVIDER` for results created by automatic provider ingestion.
- `MANUAL_ENTRY` for first-time manual entry.
- `MANUAL_OVERRIDE` for corrections/overrides of an existing result.

#### Scenario: Automatic fetch creates confirmed result

- **WHEN** automatic fetch ingests a confirmed global draw result
- **THEN** the system publishes `GlobalDrawResultAvailableEvent` containing result id, result slot id,
  slot key, draw date, occurred-at, provider and source `PROVIDER`.

#### Scenario: Manual result is recorded

- **WHEN** an authorized operator records a first-time manual draw result
- **THEN** the system publishes the same `GlobalDrawResultAvailableEvent`
- **AND** source is `MANUAL_ENTRY`.

#### Scenario: Tenant admin manual result remains provisional

- **GIVEN** a tenant owner or tenant admin records a manual result
- **WHEN** the request attempts to set `observeTrustPolicy` to `false`
- **THEN** the backend ignores that request value
- **AND** stores the result with status `PROVISIONAL`
- **AND** the result is not published as a confirmed public result.

#### Scenario: Super admin confirms a provisional manual result

- **GIVEN** a provisional manual result exists
- **WHEN** a super admin uses the platform confirmation action
- **THEN** the result becomes `CONFIRMED`
- **AND** it can be published and applied according to the normal result flow.

#### Scenario: Override corrects existing result

- **GIVEN** a global draw result already exists for a slot/date
- **WHEN** an authorized operator overrides that result
- **THEN** the system publishes `GlobalDrawResultCorrectedEvent`
- **AND** source is `MANUAL_OVERRIDE`
- **AND** it does not publish another `GlobalDrawResultAvailableEvent`.

#### Scenario: Result availability is distinct from tenant apply and settle

- **WHEN** `GlobalDrawResultAvailableEvent` is published
- **THEN** the event means the global result exists
- **AND** it does not imply tenant draws have been resulted
- **AND** it does not imply tickets have been settled or paid.

#### Scenario: Result creation resolves reminders regardless of source

- **GIVEN** an open action-required reminder exists for a result slot/date
- **WHEN** a global draw result is created for that slot/date from provider or first-time manual entry
- **THEN** the open reminder can be resolved
- **AND** resolution does not depend on whether source is `PROVIDER` or `MANUAL_ENTRY`.
