# Notification — Domaine fonctionnel

## Rôle

Le domaine notification regroupe les messages applicatifs visibles dans un centre de notifications.

Il ne représente pas les livraisons email, SMS ou Slack. Ces canaux sont des communications externes pilotées par `platform.communication`.

## Cross-apps

### Web

- Badge non lu.
- Liste des dernières notifications.
- Centre admin tenant sous l’espace entreprise.
- Centre plateforme pour les superadmins.
- États vides, filtres, pagination, layout mobile et rendu selon la langue système.

### Mobile

- Centre de notifications POS/seller terminal quand l’audience cible des terminaux.
- Les messages externes vers terminaux restent soumis aux règles de communication et d’opt-in.

### API

- `GET /api/v1/admin/notifications`
- `GET /api/v1/admin/notifications/unread-count`
- `POST /api/v1/admin/notifications/{notificationId}/read`
- `POST /api/v1/admin/notifications/{notificationId}/dismiss`
- `GET /api/v1/platform/notifications`
- `GET /api/v1/platform/notifications/unread-count`
- `POST /api/v1/platform/notifications/{notificationId}/read`
- `POST /api/v1/platform/notifications/{notificationId}/dismiss`

## Etats utilisateur

- `read_at`: l’acteur a lu la notification.
- `dismissed_at`: l’acteur a masqué la notification.
- Les utilisateurs ne suppriment jamais globalement une notification.
- Les actions sensibles (`cancel`, `purge`, `republish`) sont réservées et auditées.

## Rétention

Les notifications sont des lignes d’inbox, pas l’historique métier durable. Les lignes lues, dismissées, expirées ou annulées sont éligibles à la purge douce selon la politique de rétention.

## Déclencheurs

Les notifications automatiques sont créées à partir d’événements métier/système ou d’une API dédiée après commit. Les contrôleurs ne déclenchent pas directement une notification.

## Pointeurs

- Backend notification: `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/notification/PLATFORM_NOTIFICATION.md`
- Backend communication: `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/communication/PLATFORM_COMMUNICATION.md`
