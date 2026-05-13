# Spec — RejectOfflineSubmissionCommandHandler

## Responsibility

Reject an offline submission with reason and audit trail.

## Rules

- Rejection is tenant-scoped.
- Rejection records actor/system info and reason code.
- Rejection is idempotent if already rejected.
- Optional notification happens after commit via event/platform.notification.
