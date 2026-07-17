## ADDED Requirements

### Requirement: Action-required notifications target platform operations

The notification capability SHALL support operational notification type
`DRAW_RESULT_ACTION_REQUIRED`.

It SHALL support reasons:

- `MANUAL_ENTRY_REQUIRED`
- `AUTOMATIC_FETCH_OVERDUE`

These alerts SHALL target only platform operations audiences: `SUPER_ADMIN` and operators with the
appropriate draw-result/ops permission. They SHALL NOT target seller terminals, public users, or
tenant audiences.

#### Scenario: Manual result action required

- **GIVEN** active manual slot `MN_EVE` is 5 minutes past draw time
- **AND** no global draw result exists for the slot/date
- **WHEN** the reminder command creates the alert
- **THEN** platform operations receive `DRAW_RESULT_ACTION_REQUIRED`
- **AND** reason is `MANUAL_ENTRY_REQUIRED`
- **AND** the title is equivalent to `Résultat manuel requis — Minnesota Evening du 16 juillet.`
- **AND** metadata includes result slot id, slot key, provider, draw date, expected occurred-at,
  elapsed duration, reason, suggested ops route and correlation/request id if available.

#### Scenario: Automatic provider overdue action required

- **GIVEN** active automatic slot `FL_EVE` is 60 minutes past draw time
- **AND** no global draw result exists for the slot/date
- **WHEN** the reminder command creates the alert
- **THEN** platform operations receive `DRAW_RESULT_ACTION_REQUIRED`
- **AND** reason is `AUTOMATIC_FETCH_OVERDUE`
- **AND** the title is equivalent to
  `Résultat provider non reçu après 1 heure — Florida Evening du 16 juillet.`
- **AND** tenant audiences are not notified by this alert.

### Requirement: Action-required notification expiration

Action-required notifications SHALL expire at the earlier of:

- `createdAt + 24 hours`
- next occurrence of the same result slot minus 1 minute

If no next occurrence is found, or the next occurrence is already past/invalid, the expiration SHALL
be `createdAt + 24 hours`.

The next occurrence SHALL be calculated from the same `resultSlotId`, slot timezone, draw time and
active slot calendar. It SHALL NOT be based on any tenant draw.

#### Scenario: Next occurrence controls expiration

- **GIVEN** an action-required notification is created at `2026-07-16T18:22:00-05:00`
- **AND** the next occurrence of the same result slot is in 6 hours
- **WHEN** expiration is calculated
- **THEN** `expiresAt` is one minute before that next occurrence.

#### Scenario: Twenty-four hour cap controls expiration

- **GIVEN** an action-required notification is created now
- **AND** the next occurrence of the same result slot is in 30 hours
- **WHEN** expiration is calculated
- **THEN** `expiresAt` is `createdAt + 24 hours`.

#### Scenario: Expired notification is not active

- **GIVEN** an action-required notification has expired
- **WHEN** active notifications are listed
- **THEN** the expired notification is not returned as active.

### Requirement: Action-required notifications resolve when result arrives

When `GlobalDrawResultAvailableEvent` is received, notification rules SHALL resolve open
`DRAW_RESULT_ACTION_REQUIRED` notifications matching the event `resultSlotId + drawDate`.

Both `MANUAL_ENTRY_REQUIRED` and `AUTOMATIC_FETCH_OVERDUE` notifications SHALL be resolved.

If the model supports `RESOLVED` or `DISMISSED`, the system SHOULD use that status. Otherwise it
SHALL apply immediate expiration.

#### Scenario: Result arrival resolves open alerts

- **GIVEN** an open `DRAW_RESULT_ACTION_REQUIRED` notification exists for `FL_EVE` on
  `2026-07-16`
- **WHEN** `GlobalDrawResultAvailableEvent` is consumed for the same slot/date
- **THEN** the notification is no longer active
- **AND** no new action-required Slack alert is sent after resolution.

#### Scenario: Optional resolution Slack

- **GIVEN** product policy enables resolution Slack messages
- **WHEN** an open action-required alert is resolved by result arrival
- **THEN** communication may enqueue a message equivalent to
  `Résultat reçu — Florida Evening du 16 juillet.`
- **AND** the resolution message uses its own stable correlation key.

### Requirement: Result-available notifications target affected tenant users

The notification capability SHALL create in-app notifications for tenant audiences affected by a
newly available global draw result.

Affected tenants SHALL be resolved from tenant channel configuration and tenant draws, not from the
mere existence of a global `draw_result`.

The system SHALL resolve affected tenants through a query/read API owned by the domain that owns
tenant draw-channel configuration and draw applicability. Notification rules SHALL NOT build their
own tenant list inside `core.drawresult` and SHALL NOT read repositories from another domain.

A tenant is affected only when all of the following are true:

- the result channel is configured for the tenant;
- the tenant channel is active;
- the result slot is applicable to that tenant channel;
- a matching tenant draw exists for the result slot/date;
- the matching tenant draw is not cancelled or disabled.

Provider-level subscription alone SHALL NOT be sufficient when the precise result slot is disabled
or not configured for the tenant.

#### Scenario: Result arrives for tenant draws

- **GIVEN** a global draw result is ingested for slot `NY_EVE` and draw date `2026-07-16`
- **AND** tenant A has the channel and slot active and has a matching non-cancelled draw
- **AND** tenant B has no matching draw
- **WHEN** notification rules process `GlobalDrawResultAvailableEvent`
- **THEN** tenant A admins and authorized tenant operators receive a result-available notification
- **AND** tenant B receives no notification.
- **AND** the notification text is equivalent to
  `Résultat disponible pour Florida Evening du 16 juillet.`
- **AND** it does not say tickets are settled or paid.

#### Scenario: Notification does not leak foreign draw metadata

- **GIVEN** a global result availability event affects multiple tenants
- **WHEN** notifications are created
- **THEN** each tenant notification metadata contains only that tenant's draw identifiers and public
  result metadata
- **AND** no foreign tenant draw id is included.

#### Scenario: Tenant without channel subscription is excluded

- **GIVEN** a global draw result exists for provider `FL` and slot `FL_EVE`
- **AND** tenant A does not have the corresponding channel configured
- **WHEN** affected tenants are resolved
- **THEN** tenant A receives no result-available notification.

#### Scenario: Tenant with disabled channel is excluded

- **GIVEN** a tenant has the corresponding channel configured but disabled
- **WHEN** a global result arrives for that channel
- **THEN** the tenant receives no result-available notification.

#### Scenario: Tenant subscribed to provider but not slot is excluded

- **GIVEN** a tenant has some `FL` provider channels active
- **AND** the specific `FL_EVE` slot is disabled or not configured for that tenant
- **WHEN** a global result arrives for `FL_EVE`
- **THEN** the tenant receives no result-available notification.

#### Scenario: Tenant without applicable draw is excluded

- **GIVEN** a tenant has the channel and slot active
- **AND** no matching tenant draw exists for the result slot/date
- **WHEN** a global result arrives for that slot/date
- **THEN** the tenant receives no result-available notification.

#### Scenario: Two subscribed tenants out of three

- **GIVEN** three tenants exist
- **AND** exactly two tenants have active applicable channels and matching non-cancelled draws
- **WHEN** a global result arrives for that slot/date
- **THEN** exactly two tenant result-available notifications are created.

#### Scenario: Override does not re-send result available

- **GIVEN** a global result already existed and tenant result-available notifications were sent
- **WHEN** `GlobalDrawResultCorrectedEvent` is consumed for an override
- **THEN** no new tenant "Result available" notification is sent.

#### Scenario: Manual result uses the same subscription filter

- **GIVEN** a first-time manual result is recorded with source `MANUAL_ENTRY`
- **WHEN** tenant result-available notifications are created
- **THEN** the same channel, slot and draw applicability filter is used as for provider results.

#### Scenario: Override correction notification uses subscription filter

- **GIVEN** an existing global result is overridden with source `MANUAL_OVERRIDE`
- **WHEN** tenant correction notifications are enabled by product policy
- **THEN** notifications are created only for subscribed tenants whose matching draw was already
  affected by or exposed to that result
- **AND** tenants without active applicable channels, slots or draws receive no correction
  notification.

### Requirement: Draw-result corrections are platform-ops notifications by default

The notification capability SHALL support a distinct internal notification for corrected draw
results when product policy enables it, targeted only to platform operations.

#### Scenario: Corrected result notification

- **GIVEN** an operator overrides an existing global draw result
- **WHEN** `GlobalDrawResultCorrectedEvent` is consumed
- **THEN** platform operations may receive an internal "Draw result corrected" notification
- **AND** tenant audiences, seller terminals and public users do not receive it by default.

Tenant correction notifications MAY be enabled separately only for subscribed tenants genuinely
affected by the correction.

### Requirement: Result notification rules are idempotent

Result-available and action-required notification rules SHALL use stable trigger/correlation keys.

Action-required correlation keys:

- `drawresult.action-required:manual:{resultSlotId}:{drawDate}`
- `drawresult.action-required:automatic-overdue:{resultSlotId}:{drawDate}`

Result-available correlation key:

- `drawresult.available:{drawResultId}:{tenantId}:{audienceType}`

Tenant correction correlation keys SHALL also be tenant-scoped and SHALL include the corrected result
identifier, tenant id and audience type.

The scheduler tick time SHALL NOT be included in any correlation key. Duplicate events SHALL be
treated as no-ops.

#### Scenario: Duplicate result-available event replay

- **GIVEN** a notification trigger already exists for a result-available audience
- **WHEN** the same global result available event is replayed
- **THEN** no duplicate notification is created.

#### Scenario: Duplicate action-required replay

- **GIVEN** a notification trigger already exists for an action-required occurrence and reason
- **WHEN** the reminder scheduler retries
- **THEN** no duplicate notification is created.

#### Scenario: Migration only if correlation is insufficient

- **GIVEN** existing platform notification correlation mechanisms cannot enforce uniqueness reliably
- **WHEN** implementation starts
- **THEN** a Flyway migration may add the minimum unique constraint needed
- **AND** no migration is added if existing mechanisms already guarantee idempotency.
