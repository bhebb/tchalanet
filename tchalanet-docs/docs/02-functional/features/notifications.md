# Feature: Notifications — Fonctionnel

## Rôle

Exposer un centre de notifications persistant pour les utilisateurs tenant et les opérateurs plateforme.

Une notification est un message applicatif in-app: elle peut être lue, marquée comme lue, dismissée, republiée ou annulée selon les droits. Les emails, SMS, Slack et futures notifications push ne sont pas envoyés par cette feature; ils passent par la capacité `platform.communication`.

## Cross-apps (Web / Mobile)

### Web (UI)

- Badge global avec compteur non lu.
- Dropdown des dernières notifications.
- Centres de notifications admin tenant et plateforme avec filtres, pagination, états vides et rendu mobile.
- Clic: marque la notification lue puis navigue vers la route d’action quand elle existe.
- Dismiss: masque la notification pour l’acteur courant sans la supprimer pour les autres.

### Mobile (POS)

- Les notifications POS/seller terminal restent in-app et ciblées par audience.
- Les fanouts externes SMS/WhatsApp pour seller terminals demandent une résolution explicite des destinataires côté backend avant activation large.

### API (contrats)

- Tenant admin:
  - `GET /api/v1/admin/notifications`
  - `GET /api/v1/admin/notifications/unread-count`
  - `POST /api/v1/admin/notifications/{notificationId}/read`
  - `POST /api/v1/admin/notifications/{notificationId}/dismiss`
  - `POST /api/v1/admin/notifications/read-all`
- Platform:
  - `GET /api/v1/platform/notifications`
  - `GET /api/v1/platform/notifications/unread-count`
  - `POST /api/v1/platform/notifications/{notificationId}/read`
  - `POST /api/v1/platform/notifications/{notificationId}/dismiss`
  - `POST /api/v1/platform/notifications/read-all`

## Cycle de vie

- `read`: état par acteur; ne supprime rien.
- `dismiss`: état par acteur; marque aussi lu si nécessaire.
- `cancel`: action superadmin/système, avec raison et audit.
- `republish`: nouvelle publication; les anciens états lus/dismissés ne sont pas réinitialisés.
- `replay recipients`: ajoute seulement les destinataires manquants.
- `purge`: purge douce des lignes expirées, annulées, lues ou dismissées selon rétention.

## Langues

- Les notifications système utilisent des clés i18n et des variables.
- Les annonces manuelles doivent fournir `fr`, `en` et `ht`.
- Le rendu suit la langue système de l’acteur, puis `fr`, puis la première traduction disponible.

## Déclencheurs automatiques P0

Les déclencheurs couvrent les familles tenant/onboarding, seller terminal, ops job/gate/resource, résultats/tirages, settlement, support/contact, cache et maintenance.

Chaque déclencheur doit être idempotent via un triplet stable:

- `trigger_key`
- `source_type`
- `source_id`

## Communication externe

Quand une publication demande `EMAIL`, `SMS`, `SLACK` ou un canal externe futur, `platform.notification` publie l’événement de publication et `platform.communication` crée les messages sortants. `IN_APP` reste entièrement dans le centre de notifications.

## Pointeurs (near-code)

- Backend notification: `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/notification/PLATFORM_NOTIFICATION.md`
- Backend communication: `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/communication/PLATFORM_COMMUNICATION.md`
- OpenSpec: `openspec/changes/notifications-v0-communication-bridge/`
