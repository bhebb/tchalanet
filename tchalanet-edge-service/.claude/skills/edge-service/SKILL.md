---
name: edge-service
description: >
  Use when writing or reviewing code in tchalanet-edge-service — covers the Node.js/TypeScript Express service for rules evaluation (json-rules-engine), Liquid template rendering, and multi-channel notification dispatch (EMAIL, SMS, WhatsApp, Push, Web).
---

# Edge Service — Conventions

> Source de vérité : `tchalanet-edge-service/README.md`
> Stack : `tchalanet-edge-service/package.json`

## Rôle

Service Node.js/TypeScript dédié, appelé par `tchalanet-server` via HTTP interne.

Responsabilités :

1. **Évaluation de règles JSON** — json-rules-engine 7
2. **Rendu de templates Liquid** — LiquidJS 10
3. **Envoi de communications multi-canaux** — WEB, SMS, EMAIL, WHATSAPP, PUSH

---

## Stack

| Élément        | Version                 |
| -------------- | ----------------------- |
| Runtime        | Node.js **20.19.x**     |
| Langage        | TypeScript **5.6+**     |
| Framework HTTP | Express **4.19**        |
| Rules engine   | json-rules-engine **7** |
| Templates      | LiquidJS **10**         |
| HTTP client    | Axios **1.7**           |
| Logging        | Morgan                  |

---

## Architecture

```
tchalanet-edge-service/
├─ src/
│  ├─ routes/        ← endpoints Express (thin)
│  ├─ services/      ← logique métier par domaine
│  │  ├─ rules/      ← évaluation règles JSON
│  │  ├─ templates/  ← rendu Liquid
│  │  └─ channels/   ← envoi par canal (email, sms, push, web, whatsapp)
│  ├─ config/        ← configuration (dotenv)
│  └─ index.ts       ← entrypoint (port 4001)
├─ package.json
└─ tsconfig.json
```

---

## Règles de code

- **TypeScript strict** — pas de `any` implicite
- **Express thin** — routes délèguent aux services
- **Un endpoint = un service** (règles, templates, envoi)
- Pas de logique métier dans les routes
- Configuration via variables d'environnement (dotenv)

---

## Évaluation de règles

```typescript
import { Engine } from 'json-rules-engine';

const engine = new Engine();
engine.addRule({
  conditions: {
    all: [{ fact: 'amount', operator: 'greaterThan', value: 1000 }],
  },
  event: { type: 'high-value-sale' },
});

const { events } = await engine.run(facts);
```

---

## Templates Liquid

```typescript
import { Liquid } from 'liquidjs';

const engine = new Liquid();
const result = await engine.parseAndRender(
  'Bonjour {{ prenom }}, votre ticket {{ code }} est validé.',
  { prenom: 'Jean', code: 'TCK-001' },
);
```

---

## Canaux supportés

| Canal      | Usage                      |
| ---------- | -------------------------- |
| `WEB`      | Notifications in-app       |
| `EMAIL`    | Emails transactionnels     |
| `SMS`      | SMS courts                 |
| `WHATSAPP` | Messages WhatsApp Business |
| `PUSH`     | Notifications push mobile  |

---

## Démarrage local

```bash
cd tchalanet-edge-service
npm install
npm run dev        # TypeScript watch, port 4001
```

---

## Intégration avec tchalanet-server

- Appelé via HTTP depuis `tchalanet-server`
- Endpoints définis dans `tchalanet-server` (via `XxxClientPort` / `XxxGatewayPort`)
- Port par défaut : **4001**
- Authentification : variable d'environnement (shared secret ou réseau interne)
