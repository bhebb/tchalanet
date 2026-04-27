# Feature: Notifications — Fonctionnel

## Rôle

Exposer les notifications utilisateur (tenant) et gérer l’ack.

## Cross-apps (Web / Mobile)

### Web (UI)

- Pages: `/app/notifications`
- Widgets: NotificationList

### Mobile (POS)

- Écrans: notifications POS

### API (contrats)

- Endpoints:
  - `GET /api/v1/tenant/notifications`
  - `POST /api/v1/tenant/notifications/{id}:ack`

## Pointeurs (near-code)

- Backend FEATURE: `tchalanet-server/src/main/java/com/tchalanet/server/features/notifications/FEATURE_NOTIFICATIONS.md`
