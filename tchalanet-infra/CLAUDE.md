# CLAUDE.md — tchalanet-infra

> **Lire d'abord** : `../CLAUDE.md` (règles transverses, secrets, OpenSpec)

---

## Stack infrastructure

| Élément       | Détail                                         |
| ------------- | ---------------------------------------------- |
| Orchestration | Docker Compose v2 (modular)                    |
| Reverse proxy | Traefik v3.6.5                                 |
| CI/CD         | GitHub Actions → GHCR → Hetzner (staging/prod) |
| Auth          | Keycloak 26 (image custom GHCR)                |
| DB            | PostgreSQL 18.1 · Redis 8.4                    |
| Secrets       | **Doppler uniquement**                         |
| Feature flags | Unleash 7.4                                    |
| Search        | Meilisearch v1.11                              |

---

## Règles absolues

- **Images Docker pinées** — jamais `:latest` — toutes les versions dans `VERSIONS.md`
- **Secrets via Doppler** — jamais dans le code, les fichiers Compose, ni la doc
- `.secrets` non-committé — ignoré via `.gitignore`
- Même topologie sur tous les envs (`local`, `dev`, `staging`, `prod`) — seule la configuration diffère
- Pas de déploiement prod manuel — tout passe par GitHub Actions
- Build CI échoue sur : version drift · migrations manquantes · configs invalides

---

## Structure Compose

```
compose/           ← fichiers services modulaires
envs/common/       ← versions partagées (compose.env)
envs/<env>/        ← overrides par environnement
```

## Commandes

```bash
make up-all ENV=dev      # démarre tous les services
make down ENV=dev        # arrête
make logs ENV=dev        # logs

# Skills infra
# tchalanet-infra/.claude/skills/infrastructure
```

## Référence

`docs/OPERATIONS.md` · `docs/HETZNER.md` · `docs/DOPPLER.md`
`VERSIONS.md` (racine) — source de vérité unique pour toutes les versions
