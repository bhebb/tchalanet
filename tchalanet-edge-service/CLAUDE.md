# CLAUDE.md — tchalanet-edge-service

> Lire d'abord : `../CLAUDE.md` (règles transverses, secrets, OpenSpec)

## OpenSpec local

```text
tchalanet-edge-service/openspec/
```

Toutes les changes edge (Fastify, notifications, templates, adapters) vivent ici.

Archiver via :

```bash
cd tchalanet-edge-service
openspec archive <change-id> --yes
```

## Périmètre

Ce projet est **autonome**. Ne pas inspecter ni modifier `tchalanet-server`, `apps/tchalanet-web`, `tchalanet-mobile` sauf demande explicite.

## Vérification contexte (obligatoire avant analyse ou édition)

```bash
pwd
git branch --show-current
git status --short
git log -1 --oneline
find . -maxdepth 3 -type d -name openspec
```

---

## Stack

| Élément    | Version                 |
| ---------- | ----------------------- |
| Runtime    | Node.js 20.19.x         |
| Langage    | TypeScript 5.6+ strict  |
| Framework  | Fastify 5               |
| Module     | ESM (`"type":"module"`) |
| Dev runner | tsx watch               |
| Tests      | vitest 2                |

## Structure

```
src/
  main.ts                              # entry point — dotenv + listen
  app.ts                               # buildApp() — factory Fastify
  config/env.ts                        # NODE_ENV, HOST, PORT
  plugins/error-handler.plugin.ts      # AppError → HTTP status
  common/errors/app-error.ts           # AppError class
  modules/
    ping/
      ping.routes.ts                   # GET /ping
      ping.types.ts
    health/
      health.routes.ts                 # GET /health, GET /ready
      health.types.ts
tests/
  ping.test.ts                         # vitest + app.inject()
```

## Endpoints actuels

| Route         | Réponse                                                  |
| ------------- | -------------------------------------------------------- |
| `GET /ping`   | `{ ok: true, service: "tchalanet-edge-service" }`        |
| `GET /health` | `{ status: "UP", service: "tchalanet-edge-service" }`    |
| `GET /ready`  | `{ status: "READY", service: "tchalanet-edge-service" }` |

## Commandes

```bash
npm run dev          # tsx watch — port 3000
npm run build        # tsc → dist/
npm start            # node dist/main.js
npm test             # vitest run
npm run test:watch   # vitest watch
npm run typecheck    # tsc -p tsconfig.json --noEmit
```

## Do ✅

- TypeScript strict — zéro `any` implicite
- Un module = un dossier `src/modules/{domaine}/` avec `routes.ts` + `types.ts`
- Erreurs métier via `AppError(message, statusCode)`
- Tests via `app.inject()` — pas de port réseau réel
- Config exclusivement via variables d'environnement (`.env.example` fourni)

## Don't ❌

- Logique métier dans les routes
- Hardcoder ports, URLs ou clés
- Importer `express`, `cors`, `morgan`
- Implémenter Slack/Brevo/Twilio/Redis ici avant leur change OpenSpec dédiée
