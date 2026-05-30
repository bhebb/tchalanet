# Platform Capability `platform.tenantgame` — Tenant Game Settings

> Archetype : Application Service Module.

## 1. Rôle

Gérer les paramètres de jeux activés et configurés par tenant (quels jeux sont disponibles, avec quels paramètres opérationnels).

**Ce module fait** :
- Stocker les associations tenant ↔ jeux activés avec leurs paramètres.
- Exposer la liste des jeux disponibles pour un tenant.
- CRUD admin de l'activation et configuration des jeux par tenant.

**Ce module ne fait pas** :
- Définition des jeux (→ `catalog.game`).
- Évaluation des limites de mise (→ `core.limitpolicy`).
- Calcul des odds (→ `catalog.pricing` ou `core.draw`).

## 2. Structure

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

## 3. Règles

- Consomme `catalog.game.api` pour les métadonnées de jeux.
- RLS actif.
- Caching des jeux activés (evict sur update admin).
