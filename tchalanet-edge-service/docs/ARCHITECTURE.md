# Tchalanet Edge Service — Architecture

> **Statut** : NORMATIVE  
> **Scope** : `tchalanet-edge-service/`  
> **Stack** : Node.js / Fastify 5 / TypeScript strict / ESM

---

## Rôle

Service périphérique léger — reçoit des requêtes de `tchalanet-server` et les route vers des providers externes (Slack, Email via Brevo, SMS via Twilio).

`tchalanet-server` appelle `tchalanet-edge-service` via HTTP interne.  
Les domaines serveur ne connaissent pas les providers — l'edge service est la seule couche qui les connaît.

---

## Structure

```
tchalanet-edge-service/
├── src/
│   ├── routes/     ← endpoints Fastify
│   ├── services/   ← logique de routage vers providers
│   └── providers/  ← adapters Slack, Brevo, Twilio
├── tests/
└── docs/
    ├── ARCHITECTURE.md        ← ce fichier
    └── conventions/README.md  ← index des conventions
```

---

## Endpoints

| Route | Rôle |
|---|---|
| `GET /ping` | Health check |
| `POST /messages` | Envoi message multi-canal |

Authentification inter-service : HMAC sur le body.  
Voir `docs/internal-messages-hmac-curl.md` pour les exemples.

---

## Providers

| Provider | Canal | Lib |
|---|---|---|
| Slack | Notifications ops/agents | `@slack/webhook` |
| Brevo | Email | `@getbrevo/brevo` |
| Twilio | SMS | `twilio` |

---

## Conventions

Voir [`docs/conventions/README.md`](./conventions/README.md).
