# CLAUDE.md — tchalanet-edge-service

> **Lire d'abord** : `../CLAUDE.md` (règles transverses, secrets, OpenSpec)

---

## Stack edge

| Élément        | Version                |
| -------------- | ---------------------- |
| Runtime        | Node.js 20.19.x        |
| Langage        | TypeScript 5.6+ strict |
| Framework      | Express 4.19           |
| Rules engine   | json-rules-engine 7    |
| Templates      | LiquidJS 10            |
| HTTP client    | Axios 1.7              |
| EMAIL          | Mailgun (`mailgun.js`) |
| SMS / WhatsApp | Bird API               |

---

## Skills edge (`tchalanet-edge-service/.claude/skills/`)

`rule-management` · `communication-api`

---

## API — 3 groupes de routes

| Route                           | Rôle                                                                     |
| ------------------------------- | ------------------------------------------------------------------------ |
| `POST /api/events`              | Reçoit un événement, charge le DecisionSet, évalue, dispatch les actions |
| `POST /api/rules/evaluate`      | Évalue un DecisionSet ad-hoc avec des facts                              |
| `POST /api/rules/validate`      | Valide la syntaxe d'un DecisionSet                                       |
| `POST /api/communications/send` | Envoie une notification sur un canal                                     |
| `GET /preview`                  | Rendu Liquid d'un template avec contexte de test                         |
| `GET /health`                   | Healthcheck                                                              |

---

## Flux principal

```
POST /api/events  { eventType, tenantId, facts }
  → loadDecisionSetForEvent(eventType)   # charge rules/{eventType}.json
  → evaluateDecisionSet(set, facts)      # json-rules-engine
  → pour chaque event send_notification
      → renderTemplate(channel, eventType, locale, context)
      → sendNotification(command)        # dispatch vers canal
```

---

## Canaux

| Canal      | Fournisseur                  | État             |
| ---------- | ---------------------------- | ---------------- |
| `WEB`      | interne (`web-message.ts`)   | ✅               |
| `EMAIL`    | Mailgun (`mailgun-email.ts`) | ✅               |
| `SMS`      | Bird (`bird-sms.ts`)         | ✅               |
| `WHATSAPP` | Bird                         | ⚠ non implémenté |
| `PUSH`     | —                            | ⚠ non implémenté |

---

## Do ✅

- TypeScript strict — zéro `any` implicite
- Routes thin — délèguent au service métier
- DecisionSet = fichier JSON dans `rules/` nommé `{eventType}.json`
- Templates Liquid dans `templates/` nommés `{channel}_{eventType}_{locale}.liquid`
- Config uniquement via variables d'environnement (`EDGE_CONFIG`)
- Canaux non implémentés retournent `'failed'` avec message explicite

## Don't ❌

- Logique métier dans les routes Express
- Hardcoder des clés API ou des URLs dans le code
- Ajouter un canal sans créer son fichier `src/channels/{canal}.ts`
- Créer des templates sans respecter la convention de nommage

---

## Commandes

```bash
npm install          # install dépendances
npm run dev          # TypeScript watch, port 4001
npm run build        # compile TypeScript → dist/
npm start            # démarre depuis dist/
```

---

## Références

| Besoin              | Fichier                             |
| ------------------- | ----------------------------------- |
| Types décision      | `src/core/decision-types.ts`        |
| Évaluation règles   | `src/core/rule-engine.ts`           |
| Dispatch canaux     | `src/core/communication-handler.ts` |
| Config env          | `src/config/env.ts`                 |
| Exemple DecisionSet | `rules/ticket.created.json`         |
