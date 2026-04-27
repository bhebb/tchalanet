# Domaine Notification / Messaging

## 1. Rôle du domaine

Le domaine **notification** gère le besoin métier suivant :

> Permettre à Tchalanet d’envoyer des messages (notifications) aux utilisateurs / acteurs (vendeurs, admins, joueurs) via différents canaux (IN_APP, email, SMS, WhatsApp, push), en déléguant la partie technique à un **service externe Node**.

Ce domaine **ne décide pas** du design UI (bannière, toast, etc.) et **n’implémente pas** les clients WhatsApp/Email/SMS lui-même.  
Il décrit **quoi** envoyer, **à qui**, **par quel canal**, et expose un **port unique** pour appeler le service Node.

## 2. Modèle conceptuel

### 2.1 Concepts principaux

- `NotificationType`  
  Décrit le type métier de la notification :

  - `TICKET_RECEIPT` : reçu de ticket (preuve, informations pour le joueur)
  - `LIMIT_ALERT` : alerte de limite atteinte/dépassée
  - `SYSTEM_MESSAGE` : message générique de la plateforme
  - `CUSTOM` : type libre pour usages ultérieurs

- `NotificationChannel`  
  Décrit le **canal** via lequel le message sera envoyé :

  - `IN_APP` : notification interne à l’application (inbox, bannière, etc.)
  - `EMAIL` : message mail
  - `SMS` : message SMS classique
  - `WHATSAPP` : message WhatsApp
  - `PUSH` : notification push (mobile / navigateur)

- `NotificationTarget`  
  Représente la cible :

  - `tenantId`
  - `userId` (optionnel)
  - `recipient` : email, numéro de téléphone ou autre identifiant spécifique au canal.

- `SendNotificationCommand` / `SendNotificationPayload`  
  Objets qui encapsulent :
  - le type (`NotificationType`),
  - le canal (`NotificationChannel`),
  - la cible (`NotificationTarget`),
  - la locale (`locale`),
  - et les **données dynamiques** (`data`) qui alimentent les templates côté Node.

### 2.2 Use case principal

- `SendNotificationHandler`
  - Entrée : `SendNotificationCommand`
  - Rôle :
    - valider au minimum la cohérence du type/canal/target,
    - transformer le `command` en `SendNotificationPayload`,
    - appeler le `NotificationGatewayPort`.

Ce handler est appelé par d’autres domaines (ex: `sales`, `limits`) ou par des features (`verification`, `private_dashboard`).

## 3. Architecture et frontières

### 3.1 Core vs Features

- **Core / notification**

  - Gère la **décision métier** d’envoyer une notification.
  - Ne connaît pas le design UI des notifications IN_APP.
  - Ne fait pas d’appel direct vers WhatsApp/SMTP : il passe par un **port unique** HTTP vers le service Node.

- **Features / notifications (UI)**
  - Gère l’**affichage** des notifications IN_APP dans l’application Tchalanet.
  - Introduit la notion de `NotificationDisplayType` (BANNER, TOAST, MODAL, INBOX_ONLY, etc.).
  - Expose les endpoints :
    - `ListMyNotifications` (inbox utilisateur),
    - `MarkNotificationRead`.
  - Consomme les read-models internes (stockage des notifs IN_APP) et mapppe vers `NotificationDto`.

### 3.2 Service Node de notifications

Les clients techniques (WhatsApp, email, SMS, push) sont **déportés dans un serveur Node** séparé :

- Tchalanet (Java) n’a qu’un seul port : `NotificationGatewayPort`.
- L’implémentation `HttpNotificationGatewayAdapter` envoie une requête HTTP typée au service Node, par ex :
  - `POST /api/notifications` avec un JSON contenant type, channel, locale, recipient, data.

Le service Node se charge de :

- la sélection du provider (WhatsApp API, SMTP, SMS gateway, FCM, …),
- la gestion des templates,
- des retries,
- du logging technique.

Tchalanet ne gère pas ces détails : ils sont considérés comme **concerns infra externalisées**.

## 4. Notifications IN_APP vs externes

- **Notifications externes** (EMAIL/SMS/WHATSAPP/PUSH) :

  - Portent le contenu principal envoyé par le Node server.
  - Sont pilotées par `core.notification` via `NotificationGatewayPort`.

- **Notifications IN_APP** :
  - Peuvent être générées en même temps que les notifications externes, ou indépendamment.
  - Sont stockées dans un read-model côté Tchalanet et exposées via `features.notifications`.
  - Possèdent un `NotificationDisplayType` qui indique comment les rendre dans l’UI :
    - `BANNER` : bannière globale (ex: “Mode offline activé”, “Limite dépassée”).
    - `TOAST` : notification non bloquante.
    - `MODAL` : demande une action explicite de l’utilisateur.
    - `INBOX_ONLY` : visible uniquement dans la liste.

Le mapping entre `NotificationType` (métier) et `NotificationDisplayType` (UI) se fait dans la feature `notifications` ou côté front, en fonction des besoins.

## 5. Interactions typiques

### 5.1 Envoi d’un reçu de ticket (TICKET_RECEIPT)

1. Le domaine `sales` valide et paie un ticket.
2. Il crée un `SendNotificationCommand` de type `TICKET_RECEIPT` + canal `WHATSAPP` (ou `EMAIL`), avec les données (`ticketNumber`, `drawDate`, `amount`, etc.).
3. Il appelle `SendNotificationHandler`.
4. `SendNotificationHandler` appelle `NotificationGatewayPort.send(payload)`.
5. `HttpNotificationGatewayAdapter` fait un `POST` vers le service Node.
6. Le service Node envoie la notification WhatsApp ou email à l’utilisateur.

Optionnel : en parallèle, une notification IN_APP est créée pour apparaître dans le dashboard.

### 5.2 Affichage dans le dashboard

1. L’utilisateur ouvre l’application.
2. `ListMyNotificationsController` est appelé.
3. `ListMyNotificationsHandler` retourne les `NotificationDto` avec `displayType` (BANNER, TOAST, etc.).
4. Le front Angular affiche :

- les bannières globales,
- les toasts,
- et la liste complète dans un volet de notifications.

## 6. Décisions de conception

- **Séparation claire métier / technique / UI** :

  - Métier d’envoi dans `core.notification`.
  - Technique provider (WhatsApp/SMTP/SMS) dans un service Node externe.
  - Affichage UI dans `features.notifications`.

- **Un port unique `NotificationGatewayPort`** pour simplifier :

  - Tchalanet ne gère pas un port par provider ; il délègue à Node.

- **Support multi-tenant et multi-locale** :

  - `NotificationTarget` inclut `tenantId`.
  - `SendNotificationCommand` inclut `locale`.
  - Les templates sont gérés côté Node et peuvent utiliser ces informations.

- **Évolutivité** :
  - Ajout de nouveaux canaux (`VOICE_CALL`, etc.) → nouvelle valeur dans `NotificationChannel`, adaptation Node.
  - Ajout de nouveaux types (`PROMO_OFFER`, `DAILY_SUMMARY`) sans casser les invariants du domaine.
