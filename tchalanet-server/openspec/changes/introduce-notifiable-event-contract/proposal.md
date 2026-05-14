# Change: introduce-notifiable-event-contract

## Why

Notification and communication capabilities need a consistent way to consume events without forcing all domains to publish generic notification/message events.

## What changes

- Keep domain-specific events.
- Add optional `NotifiableDomainEvent` contract.
- Add normalized platform intents: `NotificationIntent` and `CommunicationIntent`.
- Allow typed rules to adapt events that do not implement the optional contract.

## Impact

- New contracts in the appropriate public/common event package.
- Event mapping rules in platform capabilities.
