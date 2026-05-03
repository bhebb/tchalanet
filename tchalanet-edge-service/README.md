# Tchalanet Edge Service

Service **Node/TypeScript** sur Fastify exposant des endpoints de règles, templates et communications multi-canaux pour `tchalanet-server`.

## Stack

- **Fastify 5** · ESM · TypeScript 5.6 strict
- **tsx** pour le dev watch
- **vitest** pour les tests

## Endpoints

| Route         | Réponse                                                  |
| ------------- | -------------------------------------------------------- |
| `GET /ping`   | `{ ok: true, service: "tchalanet-edge-service" }`        |
| `GET /health` | `{ status: "UP", service: "tchalanet-edge-service" }`    |
| `GET /ready`  | `{ status: "READY", service: "tchalanet-edge-service" }` |

## Démarrage

```bash
npm install
npm run dev        # tsx watch, port 3000
```

## Autres commandes

```bash
npm test            # vitest run
npm run test:watch  # vitest watch
npm run typecheck   # tsc -p tsconfig.json --noEmit
npm run build       # compile → dist/
npm start           # node dist/main.js
```

Par défaut, le service écoute sur `HOST=0.0.0.0` / `PORT=3000`.
