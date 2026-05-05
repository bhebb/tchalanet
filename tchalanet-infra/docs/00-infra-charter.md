# Charte infra Tchalanet

## Principes

```
Makefile     = interface publique
Docker Compose = moteur
scripts/     = briques internes
docs/        = parcours
```

Ne jamais démarrer des services Docker manuellement. Toujours passer par le Makefile.

---

## Scopes de services

### P0 strict

Services minimaux pour développement IDE et validation locale.

```
Traefik
PostgreSQL
Keycloak
```

### P0+

P0 avec Redis. Requis pour l'API en mode Docker.

```
Traefik + PostgreSQL + Keycloak + Redis
```

### server-v0

Stack complète. Utilisée en staging et prod.

```
Traefik + PostgreSQL + Keycloak + Redis + API + Edge + Web
```

### post-v0

Services optionnels. Jamais requis dans server-v0.

```
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

---

## Modes

### `local-ide`

API démarrée dans IntelliJ/IDE. Docker fournit P0 (ou P0+).

```bash
make local-ide-up ENV=dev          # P0 dans Docker
make local-ide-up-redis ENV=dev    # P0+ dans Docker
make local-ide-down ENV=dev
```

### `local-api`

API en Docker. Idéal pour tester l'image Spring Boot.

```bash
make local-api-up ENV=dev
make local-api-down ENV=dev
make local-api-logs ENV=dev
```

### `local-product`

Server-v0 complet en local (API + Edge + Web dans Docker).

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
make staging-create        # Crée réseau + firewall + serveur Hetzner
make up-staging            # Démarre server-v0 sur staging
make smoke-staging         # Vérifie Keycloak, API, Edge, Web
make staging-backup        # Backup PostgreSQL local
make staging-destroy       # Backup auto + destroy serveur (confirmation requise)
make staging-restore-latest # Restaure le dernier backup
```

---

## Environnements

| Fichier       | Rôle                                       |
| ------------- | ------------------------------------------ |
| `compose.env` | Tags d'images, préfixes réseau Docker      |
| `.env.merged` | Runtime non-secret généré par merge-env.sh |
| `.secrets`    | Runtime secret — jamais committé           |

Les secrets viennent de Doppler uniquement. Voir `docs/DOPPLER-SETUP-GUIDE.md`.

---

## Traefik

- Configuration statique : `traefik/traefik.yml`
- Sources par environnement : `traefik/dynamic-src/{common,local,staging,prod}/`
- Rendu actif : `traefik/dynamic/` (un seul env à la fois)
- Rendu via : `scripts/utils/render-traefik-dynamic.sh <env>`

TLS local : mkcert (`traefik/certs/`). TLS staging/prod : Let's Encrypt.
Dashboard 8080 : local uniquement.

---

## Budget staging

Sans client : staging est disposable. Le créer au besoin, le détruire après.

```
Phase 1 : staging Hetzner créé au besoin
Phase 2 : staging + prod seulement si client/pilote
```

Stop/start ne réduit pas les coûts de façon significative. Détruire et recréer.
