# Charte infra Tchalanet

## Principes

```text
Makefile        = interface publique
Docker Compose  = moteur
scripts/        = briques internes
docs/           = parcours
```

Ne jamais démarrer des services Docker manuellement. Toujours passer par le Makefile.

---

## Scopes de services

### P0 strict

Services minimaux pour développement IDE et validation locale.

```text
Traefik
PostgreSQL
Keycloak
```

### P0+

P0 avec Redis. Requis pour l'API en mode Docker.

```text
Traefik + PostgreSQL + Keycloak + Redis
```

### server-v0

Stack complète. Utilisée en staging et prod.

```text
Traefik + PostgreSQL + Keycloak + Redis + API + Edge + Web
```

### post-v0

Services optionnels. Jamais requis dans server-v0.

```text
Meilisearch, Unleash, Umami, Mailpit
```

---

## Réseaux Docker

| Service    | edge | back |
| ---------- | ---- | ---- |
| Traefik    | ✅   |      |
| Web        | ✅   |      |
| API        | ✅   | ✅   |
| Edge       | ✅   | ✅   |
| Keycloak   | ✅   | ✅   |
| PostgreSQL |      | ✅   |
| Redis      |      | ✅   |

PostgreSQL et Redis sont `back` uniquement — jamais exposés sur `edge`.

Edge service fait partie de `server-v0`. Il est exposé via Traefik sur `edge` et peut accéder à Redis/API via `back`.

---

## Modes

### `local-ide`

API démarrée dans IntelliJ/IDE. Docker fournit P0 ou P0+.

```bash
make local-ide-up ENV=dev
make local-ide-up-redis ENV=dev
make local-ide-down ENV=dev
```

### `local-api-docker`

API en Docker. Idéal pour tester l'image Spring Boot.

```bash
make local-api-up ENV=dev
make local-api-down ENV=dev
make local-api-logs ENV=dev
```

### `local-product`

Server-v0 complet en local : API + Edge + Web dans Docker.

```bash
make local-product-up ENV=dev
make local-product-down ENV=dev
```

### `server-v0`

Staging ou prod sur Hetzner. Géré par `up-staging` / `deploy-staging`.

---

## Commandes Makefile

### P0

```bash
make p0-up ENV=dev
make p0-smoke ENV=dev
make p0-down ENV=dev
```

### P0+

```bash
make p0-plus-up ENV=dev
make p0-plus-down ENV=dev
```

### Staging disposable

```bash
make staging-create
make up-staging
make smoke-staging
make staging-backup
make staging-destroy
make staging-restore-latest
```

`staging-backup` doit sauvegarder PostgreSQL hors de la VM avant destruction.

---

## Environnements

| Fichier       | Rôle                                       |
| ------------- | ------------------------------------------ |
| `compose.env` | Tags d'images, préfixes réseau Docker      |
| `.env.merged` | Runtime non-secret généré par merge-env.sh |
| `.secrets`    | Runtime secret — jamais committé           |

Les secrets viennent de Doppler uniquement.

---

## Traefik

- Configuration statique : `traefik/traefik.yml`
- Sources par environnement : `traefik/dynamic-src/{common,local,staging,prod}/`
- Rendu actif : `traefik/dynamic/` — un seul environnement à la fois
- Rendu via : `scripts/utils/render-traefik-dynamic.sh <env>`

TLS local : mkcert. TLS staging/prod : Let's Encrypt.

Dashboard 8080 : local uniquement.

---

## Images Docker

- Ne jamais utiliser `:latest` en staging/prod.
- `IMAGE_TAG` est obligatoire en staging/prod.
- La VM ne build pas le code applicatif.
- La VM exécute uniquement des images versionnées depuis GHCR.
- API, Edge et Web doivent partager le même `IMAGE_TAG` lors d'une release staging cohérente.

---

## Web / Vercel

Vercel Free peut être utilisé uniquement pour les previews/dev non-commerciales du web.

Les environnements client, staging commercial ou production doivent utiliser :

- Web Docker sur Hetzner derrière Traefik
- ou Vercel Pro plus tard

Le `server-v0` officiel inclut le Web Docker.

---

## Workflows GitHub

Workflows v0 autorisés :

- `server-pr.yml`
- `web-pr.yml`
- `edge-pr.yml`
- `infra-check.yml`
- `docs.yml`
- `deploy-staging.yml`

Les déploiements sont manuels uniquement via `workflow_dispatch`.

Aucun build Docker automatique sur chaque push main.

Les workflows historiques doivent être archivés avant suppression.

---

## Budget staging

Sans client : staging est disposable. Le créer au besoin, le détruire après.

```text
Phase 1 : staging Hetzner créé au besoin
Phase 2 : staging + prod seulement si client/pilote
```

Stop/start ne réduit pas les coûts de façon significative. Détruire et recréer.
