# Contexte court pour Claude / Codex

Tu travailles dans `tchalanet-infra`.

Lis d'abord :

```text
openspec/context/00-infra-charter.md
openspec/context/01-infra-decisions.md
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
9. Pas de `:latest` en staging/prod. `IMAGE_TAG` est obligatoire, sans fallback flottant.
10. La VM Hetzner ne build pas l'application ; elle exécute des images GHCR versionnées.
11. Staging est disposable tant qu'il n'y a pas de client payant.
12. Les workflows doivent être sobres et manuels pour le déploiement.

Ne pas transformer l'infra en Kubernetes, Terraform complet, DB managée ou architecture multi-VM.

Specs actives :

```text
openspec/specs/infra-local-server/spec.md
openspec/specs/mobile-distribution/spec.md
```

Changes en cours :

```text
openspec/changes/infra-git-workflows-light/
openspec/changes/infra-refactor-local-server-v0/
```
