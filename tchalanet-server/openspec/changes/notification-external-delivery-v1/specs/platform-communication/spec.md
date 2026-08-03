## ADDED Requirements

### Requirement: Automatic platform alerts request Slack through the durable outbox

Automatic platform operations alerts MUST request both in-app and Slack delivery. The notification
publication listener MUST enqueue Slack through `platform.communication`; it MUST NOT call
edge-service directly.

#### Scenario: A platform job fails

- **WHEN** an automatic platform job-failure notification is created
- **THEN** its delivery policy includes `IN_APP` and `SLACK`
- **AND THEN** the after-commit communication listener enqueues one Slack outbound message.

### Requirement: Internal Slack delivery uses the operations webhook key

Internal Slack notifications MUST map to the edge-service `ops-alerts` channel key.

#### Scenario: The communication dispatcher sends an internal Slack notification

- **WHEN** it maps a `SLACK_INTERNAL` outbound message to edge-service
- **THEN** the recipient channel key is `ops-alerts`
- **AND THEN** edge-service resolves `SLACK_WEBHOOK_OPS_ALERTS`.

### Requirement: Tenant automatic notifications remain in-app by default

Automatic tenant-domain notifications MUST remain in-app only until an explicit external delivery
policy is configured for that notification family.

#### Scenario: A tenant draw-result notification is created

- **WHEN** a tenant-domain notification rule creates its notification
- **THEN** its delivery policy includes `IN_APP`
- **AND THEN** it does not enqueue email, SMS, WhatsApp, or Slack automatically.
