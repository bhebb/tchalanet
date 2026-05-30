
# Platform Capability `platform.tenantgame` — Tenant Game Settings

## Rôle

Gérer les paramètres de jeux activés et configurés par tenant (quels jeux sont disponibles, avec quels paramètres opérationnels).

**Ce module fait** :
- Stockage des associations tenant ↔ jeux activés + paramètres
- Exposition de la liste des jeux disponibles pour un tenant
- CRUD admin de l’activation/configuration des jeux par tenant

**Ce module ne fait pas** :
- Définition des jeux (voir `catalog.game`)
- Évaluation des limites de mise (voir `core.limitpolicy`)
- Calcul des odds (voir `catalog.pricing` ou `core.draw`)

## Surface API

```text
platform/tenantgame/
  api/
    TenantGameApi.java        ← enableTenantGame, disableTenantGame, resolveTenantGames, updateTenantGamePolicy
    model/
      TenantGameView.java
      TenantGameSettings.java
  internal/
    service/
    persistence/
    web/                      ← TenantGameAdminController (/api/v1/admin/games)
    cache/
    config/
```

## Intégration

- Consomme `catalog.game.api` pour les métadonnées de jeux
- RLS actif
- Caching des jeux activés (evict sur update admin)

## Règles et limitations

- Les paramètres sont propres à chaque tenant
