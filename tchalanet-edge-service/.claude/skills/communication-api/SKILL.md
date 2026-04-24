---
name: communication-api
description: Use when implementing or debugging multi-channel notification dispatch in tchalanet-edge-service — covers sendNotification(), SendNotificationCommand, ChannelType, Mailgun (EMAIL), Bird (SMS/WhatsApp), LiquidJS template rendering, the /api/communications/send endpoint, and adding new channels.
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Communication API — tchalanet-edge-service

## Concept

`sendNotification()` reçoit une commande structurée, rend le template Liquid correspondant, puis délègue l'envoi au handler du canal demandé.

---

## Types principaux

```typescript
// src/core/communication-handler.ts

export type ChannelType = 'WEB' | 'SMS' | 'EMAIL' | 'WHATSAPP' | 'PUSH';

export interface SendNotificationCommand {
  tenantId: string;
  channel: ChannelType;
  provider?: string; // ex. "bird", "mailgun" — optionnel si canal unique
  templateId: string; // ex. "sms_ticket_created_fr"
  recipients: Recipient[];
  context: any; // variables Liquid injectées dans le template
  options?: {
    dryRun?: boolean; // rend le template mais n'envoie pas
    trackDelivery?: boolean;
  };
}

export interface Recipient {
  id: string;
  phone?: string; // requis pour SMS / WhatsApp
  email?: string; // requis pour EMAIL
  name?: string;
}
```

---

## Convention de nommage des templates

```
templates/
├─ sms_ticket_created_fr.liquid
├─ sms_ticket_created_en.liquid
├─ email_ticket_validated_fr.liquid
└─ web_draw_completed_fr.liquid
```

Format : `{channel}_{eventType}_{locale}.liquid`

- `channel` : `sms`, `email`, `web`, `whatsapp`, `push` (minuscule)
- `eventType` : snake_case avec underscore (ex. `ticket_created`)
- `locale` : `fr`, `en`, `ht`

---

## Canaux implémentés

### EMAIL — Mailgun (`src/channels/mailgun-email.ts`)

Config requise :

```
MAILGUN_API_KEY
MAILGUN_DOMAIN
MAILGUN_FROM        # ex. "Tchalanet <no-reply@tchalanet.com>"
```

### SMS — Bird (`src/channels/bird-sms.ts`)

Config requise :

```
BIRD_ACCESS_KEY
BIRD_WORKSPACE_ID
BIRD_SMS_CHANNEL_ID
```

### WEB (`src/channels/web-message.ts`)

Notification in-app, pas de config externe requise.

### WHATSAPP / PUSH

Définis dans `ChannelType` mais **non implémentés** — retournent `'failed'` avec `"not yet implemented"`. Ne pas les utiliser en production.

---

## Flux d'envoi

```
sendNotification(command)
  → renderTemplate(channel, templateId, context)   # LiquidJS depuis templates/
  → dispatch(channel):
      'EMAIL'     → mailgunEmail.send(recipients, subject, html)
      'SMS'       → birdSms.send(recipients, text)
      'WEB'       → webMessage.send(recipients, payload)
      'WHATSAPP'  → throw "not yet implemented"
      'PUSH'      → throw "not yet implemented"
```

---

## Rendu de templates LiquidJS

Les templates ont accès à tout l'objet `context` passé dans `SendNotificationCommand.context` :

```liquid
Bonjour {{ user.firstName }},
Votre ticket {{ ticket.code }} a été validé le {{ ticket.date | date: "%d/%m/%Y" }}.
Montant : {{ ticket.amount | money }}.
```

Pour prévisualiser un template sans envoyer :

```
GET /preview?templateId=sms_ticket_created_fr&locale=fr
```

---

## Endpoint HTTP

```
POST /api/communications/send
Authorization: (shared secret ou réseau interne)

Body:
{
  "tenantId": "ten_abc123",
  "channel": "SMS",
  "provider": "bird",
  "templateId": "sms_ticket_created_fr",
  "recipients": [{ "id": "usr_001", "phone": "+50912345678" }],
  "context": { "user": { "firstName": "Jean" }, "ticket": { "code": "TCK-001" } },
  "options": { "dryRun": false }
}

Response: { "success": true, "results": [...] }
```

---

## Ajouter un nouveau canal

1. Créer `src/channels/{canal}.ts` implémentant la même interface que `bird-sms.ts`
2. Ajouter `ChannelType` dans `communication-handler.ts`
3. Ajouter le case dans le switch de dispatch
4. Documenter les variables d'environnement requises dans `src/config/env.ts`
5. Créer les templates correspondants dans `templates/`

---

## Dry-run

Quand `options.dryRun = true` : le template est rendu et loggé, mais aucun envoi réseau n'a lieu. Utile pour valider des templates en staging.
