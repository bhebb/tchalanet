# Images Deployment

## Images applicatives

| Image | Source | Variable |
|---|---|---|
| API | `tchalanet-server/` (Dockerfile) | `API_IMAGE_BASE` |
| Edge service | `tchalanet-edge-service/` | `TCH_EDGE_IMAGE` |
| Web (Plan B Docker) | `tchalanet-web/Dockerfile.web` | `WEB_IMAGE_BASE` |

```bash
./scripts/docker/publish-images.sh <org> <tag> ghcr.io
```

Le script met à jour `IMAGE_TAG`, `API_IMAGE_BASE` (et `WEB_IMAGE_BASE` une fois le Dockerfile web créé).

## Déploiement

```bash
make up-staging
make up-prod
```

La stack standard charge PostgreSQL, Redis, API, edge-service et Traefik.
Firebase reste externe.

Le service `web` (portails Angular) se déploie selon le plan choisi :
- **Plan A Vercel** : déclenché par push GitHub, indépendamment de cette stack.
- **Plan B Docker** : inclus dans `up-seq.sh` après `edge-service` (à activer — voir [`openspec/changes/web-deployment-plans/tasks.md`](../../openspec/changes/web-deployment-plans/tasks.md)).
