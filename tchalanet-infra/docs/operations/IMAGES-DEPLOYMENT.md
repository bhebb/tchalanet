# Images Deployment

## Images applicatives

L'infra publie l'image API. L'edge service garde son propre pipeline Node.

```bash
./scripts/docker/publish-images.sh <org> <tag> ghcr.io
```

Le script met à jour :

- `IMAGE_TAG`
- `API_IMAGE_BASE`

## Déploiement

```bash
make up-staging
make up-prod
```

La stack standard charge PostgreSQL, Redis, API, edge-service et Traefik.
Firebase reste externe.
