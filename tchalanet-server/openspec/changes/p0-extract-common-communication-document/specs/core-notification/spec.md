# core-notification Specification

## MODIFIED Requirements

### Requirement: Notification center remains in core.notification

`core.notification` SHALL own the in-app notification center and functional notification dispatch.

#### Scenario: In-app channel

- **WHEN** `SendNotificationCommand` has a recipient with channel `IN_APP`
- **THEN** the command handler SHALL create or update an in-app notification item
- **AND** SHALL NOT call edge-service.

#### Scenario: External channels

- **WHEN** `SendNotificationCommand` has a recipient with channel `EMAIL`, `SMS`, `WHATSAPP`, or `SLACK`
- **THEN** the command handler SHALL map the notification to `OutboundMessageRequest`
- **AND** send it through `common.communication.api.OutboundMessageGateway`.

### Requirement: NotificationFlowRouter stays in notification

`NotificationFlowRouter` SHALL remain in `core.notification`.

#### Scenario: Draw result notification routing

- **WHEN** draw result events are routed
- **THEN** routing SHALL continue to use `NotificationFlowProperties`
- **AND** SHALL produce `SendNotificationCommand`
- **AND** SHALL NOT move to `common.communication`.

### Requirement: Notification channel separation

`core.notification.domain.model.NotificationChannel` MAY include `IN_APP`, but `common.communication.api.CommunicationChannel` SHALL NOT include `IN_APP`.

#### Scenario: Mapper rejects in-app

- **WHEN** the outbound mapper receives `NotificationChannel.IN_APP`
- **THEN** it SHALL reject or skip it
- **AND** it SHALL NOT produce an `OutboundMessageRequest`.
