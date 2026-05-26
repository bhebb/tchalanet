# Spec — Notification impacts

## ADDED Requirements

### Requirement: In-app notification for operational alerts

Operational app alerts SHALL use `platform.notification`. They SHALL NOT be sent through `platform.communication` unless an external delivery channel is explicitly required.
