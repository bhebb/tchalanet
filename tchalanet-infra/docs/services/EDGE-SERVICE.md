# Edge Service — Config Docker (infra)

Service de notifications multi-canaux (Slack, Email, SMS) appelé par le backend Spring.
Code source : `tchalanet-edge-service/` · Image buildée localement depuis `Dockerfile`.

---

## Démarrage rapide

```bash
# Build + démarrage (première fois ou après changement de code)
make rebuild-edge ENV=dev

# Démarrage seul (image déjà buildée)
make up-edge ENV=dev

# Arrêt
make down-edge ENV=dev

# Logs
make logs-edge-service ENV=dev

# Stack complète avec API + Edge + Web
make local-product-up ENV=dev
```

---

## Prérequis

1. `make env-merge ENV=dev` doit avoir été exécuté (génère `envs/dev/.env.merged`).
2. Les réseaux Docker `edge-dev` et `back-dev` doivent exister :
   ```bash
   make networks ENV=dev
   ```
3. `EDGE_HMAC_SECRET` doit être défini dans `envs/dev/.secrets` (voir ci-dessous).

---

## Fichier compose

[`compose/docker-compose-edge-service.yml`](../compose/docker-compose-edge-service.yml)

- Build depuis `../../tchalanet-edge-service/Dockerfile` (multi-stage, Node 22 Alpine)
- Port 3000 exposé uniquement sur le réseau Docker `back` (pas publié sur l'hôte)
- Healthcheck sur `GET /ready`
- Le backend (API) l'atteint via `http://edge-service:3000` sur le réseau `back`

---

## Variables d'environnement

### Template

Copier les valeurs manquantes depuis [`envs/common/edge-service.env`](../envs/common/edge-service.env) dans `envs/dev/.secrets`.

### Obligatoire

| Variable          | Où                  | Description                                             |
| ----------------- | ------------------- | ------------------------------------------------------- |
| `EDGE_HMAC_SECRET`| `envs/dev/.secrets` | Secret HMAC partagé avec le backend. Valeur longue (≥32 octets). |

### Backend → Edge (côté API)

Ces variables doivent être dans `envs/common/api.env` ou `envs/<env>/.env` :

| Variable               | Valeur Docker                  | Description                              |
| ---------------------- | ------------------------------ | ---------------------------------------- |
| `TCH_EDGE_BASE_URL`    | `http://edge-service:3000`     | URL interne Docker vers edge-service     |
| `TCH_EDGE_HMAC_SECRET` | `${EDGE_HMAC_SECRET}`          | Doit correspondre à `EDGE_HMAC_SECRET`   |

### Providers (optionnels)

Tous désactivés par défaut (`*_ENABLED=false`). Activer uniquement les canaux utilisés.

| Variable                    | Défaut  | Description                    |
| --------------------------- | ------- | ------------------------------ |
| `SLACK_ENABLED`             | `false` | Active le canal Slack          |
| `SLACK_WEBHOOK_TCHALANET`   | -       | Webhook Slack canal principal  |
| `SLACK_WEBHOOK_BATCH_DRAWS` | -       | Webhook tirages en lot         |
| `SLACK_WEBHOOK_DELIVERY`    | -       | Webhook livraison              |
| `SLACK_WEBHOOK_OPS_ALERTS`  | -       | Webhook alertes ops            |
| `SLACK_WEBHOOK_SECURITY_AUDIT` | -    | Webhook audit sécurité         |
| `EMAIL_ENABLED`             | `false` | Active le canal Email          |
| `EMAIL_PROVIDER`            | `brevo` | Fournisseur email              |
| `BREVO_API_KEY`             | -       | Clé API Brevo                  |
| `EMAIL_FROM_NAME`           | `Tchalanet` | Nom expéditeur             |
| `EMAIL_FROM_ADDRESS`        | `no-reply@example.com` | Adresse expéditeur |
| `SMS_ENABLED`               | `false` | Active le canal SMS            |
| `SMS_PROVIDER`              | `twilio`| Fournisseur SMS                |
| `TWILIO_ACCOUNT_SID`        | -       | SID compte Twilio              |
| `TWILIO_AUTH_TOKEN`         | -       | Token auth Twilio              |
| `TWILIO_FROM`               | -       | Numéro Twilio expéditeur       |

---

## Setup dev local — secrets

Ajouter dans `envs/dev/.secrets` (créer si absent, ne jamais committer) :

```bash
# Secret HMAC partagé edge ↔ backend
EDGE_HMAC_SECRET=<valeur-aléatoire-longue>

# Slack (optionnel en dev)
# SLACK_ENABLED=true
# SLACK_WEBHOOK_OPS_ALERTS=https://hooks.slack.com/services/...
```

Générer un secret :
```bash
openssl rand -base64 32
```

---

## Vérifier que le service tourne

```bash
# État du conteneur
docker ps --filter name=edge-service

# Health check
curl http://localhost:3000/ready   # si port exposé en dev-local
# ou depuis l'intérieur du réseau :
docker exec tchalanet-edge-service-dev wget -qO- http://127.0.0.1:3000/ready
```

---

## Architecture réseau

```
[API Spring]  ──(back-dev)──▶  [edge-service:3000]
                               │
                               ├── Slack webhooks (HTTPS sortant)
                               ├── Brevo API     (HTTPS sortant)
                               └── Twilio API    (HTTPS sortant)
```

Edge-service n'est pas exposé publiquement (pas de Traefik). Le backend l'appelle directement via le réseau Docker interne `back`.

---

## Voir aussi

- Code source et config providers : [`tchalanet-edge-service/README.md`](../../tchalanet-edge-service/README.md)
- Exemples curl signés HMAC : [`tchalanet-edge-service/docs/internal-messages-hmac-curl.md`](../../tchalanet-edge-service/docs/internal-messages-hmac-curl.md)
- Compose file : [`compose/docker-compose-edge-service.yml`](../compose/docker-compose-edge-service.yml)
