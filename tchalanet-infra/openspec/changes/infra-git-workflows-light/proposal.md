# Change: infra-git-workflows-light

## Why

Les workflows actuels mélangent plusieurs générations de CI/CD : build Maven, build/push Docker sur `main`, gestion Keycloak realm séparée, déploiements infra avec services post-v0, scan sécurité lourd, doublons Docs CI, et déploiements production anciens.

Pour un solo dev, ce modèle brûle les minutes CI, complique le diagnostic, et augmente le risque de déployer ou modifier l'infra sans intention explicite.

## What

Mettre en place un set minimal de workflows GitHub Actions sobres :

```text
.github/workflows/
├── server-pr.yml
├── web-pr.yml
├── edge-pr.yml
├── infra-check.yml
├── docs.yml
└── deploy-staging.yml
```

Principes :

- PR checks uniquement avec `paths`.
- Déploiement staging uniquement manuel via `workflow_dispatch`.
- Pas de déploiement production en v0.
- Pas de build/push automatique sur chaque push `main`.
- Pas de tag Docker `latest`.
- Un `IMAGE_TAG` commun pour API/Web/Edge en staging.
- Edge est service v0 et a son propre workflow PR léger.
- Security scan est reporté ou manuel/hebdo, non bloquant sur chaque PR.
- Le workflow Keycloak realm séparé est archivé ; Keycloak est géré par le déploiement infra + `get-realm.sh`.

## Impact

- Moins de minutes GitHub Actions consommées.
- Moins de chemins CI/CD concurrents.
- Déploiement staging plus prévisible.
- Base prête pour production manuelle plus tard, sans l'activer maintenant.

## Non-goals

- Pas de déploiement production automatique.
- Pas de pipeline mobile complet.
- Pas de security scan bloquant systématique.
- Pas de build multi-arch automatique sauf décision explicite.
