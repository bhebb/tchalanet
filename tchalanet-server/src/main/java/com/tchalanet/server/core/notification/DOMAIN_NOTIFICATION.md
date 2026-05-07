# Domaine Notification / Messaging

## 1. Rôle du domaine

Le domaine **notification** gère le besoin métier suivant :

> Permettre à Tchalanet de gérer les notifications applicatives et de router, si nécessaire, les messages externes vers `common.communication`.

Ce domaine **ne décide pas** du design UI (bannière, toast, etc.) et **n’implémente pas** les clients WhatsApp/Email/SMS lui-même.
Il décrit **quoi** notifier, **à qui**, **par quel canal**, garde la source de vérité persistée pour les notifications actionnables, et délègue le transport externe à `common.communication`.

Depuis `add-notification-core-service`, le domaine contient deux niveaux :

- `notification` : notification logique, visible ou actionnable, avec statut `UNREAD`, `READ`, `ARCHIVED`, `EXPIRED`.
- `notification_delivery` : tentative/statut par canal (`WEB`, `SMS`, `WHATSAPP`, `EMAIL`, `PUSH`).

Les notifications supportent les audiences `USER`, `ROLE`, `TENANT`, `OUTLET`, `TERMINAL`, `PLATFORM`, les sévérités `INFO`, `WARNING`, `ERROR`, `CRITICAL`, et les kinds `INFO`, `WARNING`, `ACTION_REQUIRED`, `SYSTEM_ERROR`.

Les créations passent par `CreateNotificationCommand`. Le champ `dedupeKey` est optionnel mais obligatoire pour les flows rejouables ou after-commit, par exemple les mises à jour de templates PageModel.

## 2. Modèle conceptuel

### 2.1 Concepts principaux

#### Notification persistée

- `NotificationSeverity` : niveau d’attention attendu côté UI.
- `NotificationKind` : comportement fonctionnel (`ACTION_REQUIRED` active une action explicite).
- `NotificationCategory` : espace fonctionnel (`PAGE_MODEL`, `SALES`, `SYSTEM`, etc.).
- `NotificationAudienceType` + `audienceValue` : cible logique.
- `titleKey` / `messageKey` : clés i18n canoniques ; `titleText` / `messageText` restent des fallbacks.
- `payload` : métadonnées d’action ou de diagnostic, jamais source unique pour appliquer une mutation.

#### Delivery

Les deliveries sont séparées du message logique pour permettre retries, provider metadata et transport externe progressif.
Le canal `WEB` peut être utilisé pour tracer une notification in-app sans forcer SMS/WhatsApp.

- `NotificationType`  
  Décrit le type métier de la notification :

  - `TICKET_RECEIPT` : reçu de ticket (preuve, informations pour le joueur)
  - `LIMIT_ALERT` : alerte de limite atteinte/dépassée
  - `SYSTEM_MESSAGE` : message générique de la plateforme
  - `CUSTOM` : type libre pour usages ultérieurs

- `NotificationChannel`
  Décrit le **canal fonctionnel** demandé par le domaine :

  - `WEB` : notification interne à l’application (inbox, bannière, etc.)
  - `EMAIL` : message mail
  - `SMS` : message SMS classique
  - `WHATSAPP` : message WhatsApp
  - `SLACK` : message ops/technique
  - `PUSH` : notification push (mobile / navigateur)

- `SendNotificationCommand`
  Objets qui encapsulent :
  - le type (`NotificationType`),
  - les destinataires (`NotificationRecipient`),
  - la locale (`locale`),
  - et les **données dynamiques** (`context`) qui alimentent les notifications et le transport externe.

### 2.2 Use case principal

- `SendNotificationHandler`
  - Entrée : `SendNotificationCommand`
  - Rôle :
    - valider au minimum la cohérence du type/canal/target,
    - persister ou router les notifications applicatives quand le canal est interne,
    - transformer les canaux externes via `OutboundMessageMapper`,
    - appeler `OutboundMessageGateway`.

Ce handler est appelé par d’autres domaines (ex: `sales`, `limits`) ou par des features (`verification`, `private_dashboard`).

## 3. Architecture et frontières

### 3.1 Core vs Features

- **Core / notification**

  - Gère la **décision métier** d’envoyer une notification.
  - Ne connaît pas le design UI des notifications IN_APP.
  - Ne fait pas d’appel direct vers WhatsApp/SMTP : il passe par `common.communication`.

- **Core / notification APIs**
  - Expose les endpoints tenant/admin de résumé, listing, read/archive unitaire et bulk, et diagnostics delivery.
  - La table `notification` est l’unique source de vérité pour les notifications persistées.
  - Il n’existe pas de table `user_notification` ni de BFF `features.notifications` séparé.

### 3.2 Transport externe

Les clients techniques (WhatsApp, email, SMS, Slack) sont **déportés dans le edge-service** :

- Tchalanet (Java) utilise `common.communication.api.OutboundMessageGateway`.
- L’implémentation `common.communication.edge.EdgeCommunicationGatewayAdapter` envoie une requête HMAC au edge-service :
  - `POST /internal/messages/send` avec un JSON contenant eventId, severity, title, message, recipients, context.

Le service Node se charge de :

- la sélection du provider (WhatsApp API, SMTP, SMS gateway, FCM, …),
- la gestion technique provider,
- des retries,
- du logging technique.

Tchalanet ne gère pas ces détails : ils sont considérés comme **concerns infra externalisées**.

## 4. Notifications IN_APP vs externes

- **Notifications externes** (EMAIL/SMS/WHATSAPP/SLACK) :

  - Portent le contenu principal envoyé par le edge-service.
  - Sont pilotées par `core.notification` via `OutboundMessageMapper` puis `OutboundMessageGateway`.

- **Notifications WEB / IN_APP** :
  - Peuvent être générées en même temps que les notifications externes, ou indépendamment.
  - Sont stockées dans `notification` et exposées par les controllers `core.notification.infra.web`.
  - Le rendu UI est dérivé côté frontend à partir de `severity`, `kind`, `category` et `action`, sans champ `displayType` persistant.

## 5. Interactions typiques

### 5.1 Envoi d’un reçu de ticket (TICKET_RECEIPT)

1. Le domaine `sales` valide et paie un ticket.
2. Il crée un `SendNotificationCommand` de type `TICKET_RECEIPT` + canal `WHATSAPP` (ou `EMAIL`), avec les données (`ticketNumber`, `drawDate`, `amount`, etc.).
3. Il appelle `SendNotificationHandler`.
4. `SendNotificationHandler` mappe vers `OutboundMessageRequest`.
5. `EdgeCommunicationGatewayAdapter` fait un `POST /internal/messages/send` signé HMAC vers le edge-service.
6. Le edge-service envoie le message WhatsApp ou email à l’utilisateur.

Optionnel : en parallèle, une notification IN_APP est créée pour apparaître dans le dashboard.

### 5.2 Affichage dans le dashboard

1. L’utilisateur ouvre l’application.
2. `TenantNotificationController` est appelé (`/tenant/notifications/summary` ou `/tenant/notifications`).
3. Les query handlers retournent `NotificationSummaryView` ou `NotificationItemView`.
4. Le front Angular affiche :

- les bannières globales,
- les toasts,
- et la liste complète dans un volet de notifications.

## 6. Décisions de conception

- **Séparation claire métier / technique / UI** :

  - Métier d’envoi dans `core.notification`.
  - Technique provider (WhatsApp/SMTP/SMS) dans un service Node externe.
  - Affichage UI côté front à partir du contrat `core.notification`.

- **Un port unique `OutboundMessageGateway`** pour le transport externe :

  - Tchalanet ne gère pas un port par provider ; il délègue à `common.communication` puis au edge-service.

- **Support multi-tenant et multi-locale** :

  - `NotificationRecipient` inclut `tenantId`.
  - `SendNotificationCommand` inclut `locale`.
  - Les templates sont gérés côté Node et peuvent utiliser ces informations.

- **Évolutivité** :
  - Ajout de nouveaux canaux (`VOICE_CALL`, etc.) → nouvelle valeur dans `NotificationChannel`, adaptation Node.
  - Ajout de nouveaux types (`PROMO_OFFER`, `DAILY_SUMMARY`) sans casser les invariants du domaine.
