# tchalanet-infra — OpenSpec Project Context

## Périmètre

Ce OpenSpec couvre **uniquement** l'infrastructure Docker / DevOps.

Périmètres inclus :

- Docker Compose (local, staging, prod)
- Traefik (routing, TLS, middlewares)
- Firebase Auth runtime configuration and emulator mode
- PostgreSQL (init, extensions, RLS bootstrap)
- Redis configuration
- Variables d'environnement et secrets Doppler
- CI/CD pipelines

## Ne pas inclure ici

- Changes backend → `tchalanet-server/openspec/`
- Changes Angular → `apps/tchalanet-web/openspec/`
- Changes Flutter → `tchalanet-mobile/openspec/`
- Changes edge → `tchalanet-edge-service/openspec/`
- Coordination cross-projet → `openspec/` (racine)

## Conventions d'archivage

```bash
cd tchalanet-infra
openspec archive <change-id> --yes
openspec validate --strict
```
