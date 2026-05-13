# Platform Capability `platform.communication` — Email / SMS / Push Delivery

> Archetype : Application Service Module.

## 1. Rôle

Délivrer des messages vers des canaux externes (email, SMS, push mobile) à partir de templates.

**Ce module fait** :
- Envoyer un email / SMS / push depuis un template et des variables.
- Gérer les templates de messages (CRUD admin).
- Tracer les envois (statut, timestamp, canal).

**Ce module ne fait pas** :
- Notifications in-app (→ `platform.notification`).
- Contenu métier des messages (les callers fournissent les variables).

## 2. Structure

```text
platform/communication/
  api/
    CommunicationApi.java     ← send(CommunicationRequest)
    model/
      CommunicationRequest.java  ← templateKey, variables, recipient, channel
      Channel.java               ← EMAIL | SMS | PUSH
  internal/
    service/
    adapter/                  ← EmailAdapter, SmsAdapter, PushAdapter
    persistence/              ← DeliveryLogJpaEntity
    web/                      ← TemplateAdminController (/api/v1/platform/templates)
    config/
```

## 3. Règles

- Adapter externe = côté `internal/adapter/` uniquement, jamais exposé via api/.
- Retry et fallback gérés dans l'adapter, pas dans le caller.
- Log de livraison persisté pour audit.
- `platform.notification` peut déléguer à ce module pour les push/email.
