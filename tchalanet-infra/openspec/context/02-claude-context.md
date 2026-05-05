# Contexte court pour Claude / Codex

Tu travailles dans `tchalanet-infra`.

Lis d'abord :

```text
docs/00-infra-charter.md
docs/context/01-infra-decisions.md
```

Règles absolues :

1. Le Makefile est l'interface publique.
2. Docker Compose est le moteur.
3. Les scripts sont internes.
4. Ne démarre pas les services manuellement hors Makefile.
5. Ne supprime pas brutalement les scripts historiques : classifie d'abord.
6. Ne réintroduis pas Meilisearch, Unleash, Umami ou Mailpit dans server-v0.
7. Redis et PostgreSQL sont `back` uniquement.
8. Edge service est v0 et utilise `edge + back`.
9. Pas de `:latest` en staging/prod.
10. La VM Hetzner ne build pas l'application ; elle exécute des images GHCR versionnées.
11. Staging est disposable tant qu'il n'y a pas de client payant.
12. Les workflows doivent être sobres et manuels pour le déploiement.

Objectif de la prochaine proposal :

```text
infra-refactor-local-server-v0
```

Elle doit stabiliser :

- modes locaux
- P0 / P0+ / server-v0
- Traefik local/staging/prod
- Postgres
- Redis
- Keycloak realm template/overlays
- Makefile
- scripts utiles vs legacy
- staging Hetzner disposable
- API Docker startup

Ne pas transformer l'infra en Kubernetes, Terraform complet, DB managée ou architecture multi-VM pour l'instant.
