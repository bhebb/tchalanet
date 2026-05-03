# Tchalanet Edge Service

Service **Node/TypeScript** sur Fastify — notifications multi-canaux (Slack, Email, SMS) appelées depuis `tchalanet-server`.

## Stack

- **Fastify 5** · ESM · TypeScript 5.6 strict
- **tsx** pour le dev watch · **vitest** pour les tests
- Slack via `@slack/webhook` · Email via `@getbrevo/brevo` · SMS via `twilio`

## Endpoints système

| Route         | Réponse                                                  |
| ------------- | -------------------------------------------------------- |
| `GET /ping`   | `{ ok: true, service: "tchalanet-edge-service" }`        |
| `GET /health` | `{ status: "UP", service: "tchalanet-edge-service" }`    |
| `GET /ready`  | `{ status: "READY", service: "tchalanet-edge-service" }` |

## Endpoint notifications

| Route                               | Description             |
| ----------------------------------- | ----------------------- |
| `POST /internal/notifications/send` | Envoie une notification |

Réponse HTTP **202** — même si un canal échoue, le résultat par destinataire est retourné.

## Démarrage

```bash
cp .env.example .env   # configurer les credentials providers
npm install
npm run dev                   # tsx watch, port 3000
```

## Commandes

```bash
npm test            # vitest run (11 tests, sans appels provider réels)
npm run test:watch  # vitest watch
npm run typecheck   # tsc -p tsconfig.json --noEmit
npm run build       # compile → dist/
npm start           # node dist/main.js
```

## Configuration providers

Copier `.env.example` → `.env.local` et renseigner les variables.
**Ne jamais committer les secrets.**

### Slack

```env
SLACK_ENABLED=true
SLACK_WEBHOOK_BATCH_DRAWS=https://hooks.slack.com/services/...
SLACK_WEBHOOK_OPS_ALERTS=https://hooks.slack.com/services/...
```

Canaux disponibles : `tchalanet`, `batch-draws`, `delivery`, `ops-alerts`, `security-audit`.

### Email (Brevo)

```env
EMAIL_ENABLED=true
BREVO_API_KEY=xkeysib-...
EMAIL_FROM_NAME=Tchalanet
EMAIL_FROM_ADDRESS=no-reply@example.com
```

### SMS (Twilio)

```env
SMS_ENABLED=true
TWILIO_ACCOUNT_SID=ACxxxx
TWILIO_AUTH_TOKEN=xxxx
TWILIO_FROM=+1234567890
```

## Tests manuels curl

### Slack

```bash
curl -X POST http://localhost:3000/internal/notifications/send \
  -H 'content-type: application/json' \
  -d '{
    "eventId": "local-slack-test-001",
    "severity": "INFO",
    "title": "Tchalanet Slack test",
    "message": "Edge-service can send Slack messages.",
    "recipients": [{ "channel": "SLACK", "channelKey": "batch-draws" }]
  }'
```

### Email

```bash
curl -X POST http://localhost:3000/internal/notifications/send \
  -H 'content-type: application/json' \
  -d '{
    "eventId": "local-email-test-001",
    "severity": "INFO",
    "title": "Tchalanet Email test",
    "message": "Edge-service can send email messages.",
    "recipients": [{ "channel": "EMAIL", "to": "your-email@example.com" }]
  }'
```

### SMS

```bash
curl -X POST http://localhost:3000/internal/notifications/send \
  -H 'content-type: application/json' \
  -d '{
    "eventId": "local-sms-test-001",
    "severity": "WARN",
    "title": "Tchalanet SMS test",
    "message": "Edge-service can send SMS.",
    "recipients": [{ "channel": "SMS", "to": "+33600000000" }]
  }'
```

## Canaux futurs

- **WhatsApp** — type présent dans le contrat (`WHATSAPP`) mais pas d'adapter implémenté.
- **HMAC auth** sur `/internal/*` — à implémenter dans une prochaine change OpenSpec.
- Redis idempotency/anti-spam — change dédiée.
