# Platform Capability `platform.notification` — In-App Notifications

> Archetype : Application Service Module.

## 1. Rôle

Créer, stocker et exposer les notifications in-app pour les utilisateurs tenant.

**Ce module fait** :
- Persister des notifications (`NotificationApi.send(NotificationRequest)`).
- Exposer le feed de notifications d'un utilisateur (lu/non-lu).
- Marquer les notifications comme lues.

**Ce module ne fait pas** :
- Livraison email/SMS/push (→ `platform.communication`).
- Notifications Keycloak (→ infrastructure externe).

## 2. Structure

```text
platform/notification/
  api/
    NotificationApi.java      ← send(NotificationRequest), listForUser(UserId)
    model/
      NotificationRequest.java
      NotificationView.java
  internal/
    service/
    persistence/
    web/                      ← NotificationController (/api/v1/tenant/notifications)
    event/                    ← Listener d'events core pour déclencher notifications
    config/
```

## 3. Règles

- RLS actif.
- Écoute les events core (ex. `TicketResultedEvent`) pour créer des notifications.
- `core` ne doit pas écouter les events de ce module.
- La livraison push/email est déléguée à `platform.communication`.
