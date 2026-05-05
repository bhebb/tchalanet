# Change: infra-refactor-local-server-v0

## Why

L'infra Tchalanet doit être stabilisée avant de continuer l'API Docker, le web Docker et le premier staging. Aujourd'hui, les responsabilités sont mélangées entre Makefile, Compose, scripts, docs, Traefik, envs et services post-v0.

Sans client payant, le coût doit rester bas. Le staging Hetzner doit donc être disposable : créé au besoin, testé, sauvegardé si nécessaire, puis détruit.

## What Changes

Refactorer l'infra autour de chemins dorés simples :

```text
Makefile = interface publique
Docker Compose = moteur
scripts = briques internes
docs = parcours
```

Définir les scopes :

```text
P0 strict = Traefik + PostgreSQL + Keycloak
P0+       = P0 strict + Redis
server-v0 = Traefik + PostgreSQL + Keycloak + Redis + API + Edge + Web
post-v0   = Meilisearch + Unleash + Umami + Mailpit
```

Définir les modes :

```text
local-ide        API en IDE, infra P0/P0+ Docker
local-api-docker API en Docker, web/mobile/edge au besoin hors Docker
local-product    tout server-v0 en Docker local
server-v0        staging/prod Docker
```

Ajouter une stratégie staging Hetzner disposable :

```text
staging-create
staging-up
staging-smoke
staging-backup
staging-destroy
staging-restore-latest
```

## Impact

- Infra plus simple à opérer.
- API Docker débogable après P0 vert.
- Coût Hetzner réduit sans client.
- Staging recréable et documenté.
- Préparation cohérente pour prod plus tard.

## Non-goals

- Pas de Kubernetes.
- Pas de DB managée.
- Pas de séparation VM app/db en v0.
- Pas de Meilisearch/Unleash/Umami/Mailpit dans v0.
- Pas de production persistante tant qu'il n'y a pas client/pilote explicite.
