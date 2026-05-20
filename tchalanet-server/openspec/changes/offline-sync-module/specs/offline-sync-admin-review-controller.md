# Spec — Offline Sync Admin Review Controller

## Responsibility

Expose tenant-admin or platform-admin review actions for offline submissions that require manual decision.

## Endpoints

- list review queue;
- view details;
- reject with reason;
- retry promotion if allowed;
- mark as resolved if no sale is created.

## Rules

- Admin actions must be audited.
- Controller does not directly mutate repositories.
- Admin override must be explicit and permission-protected.
