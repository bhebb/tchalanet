# Platform Capability `platform.tenantconfig` — Tenant Configuration

> Archetype : Application Service Module. Migré depuis `core.tenantconfig`.

## 1. Rôle

Stocker et exposer les valeurs de configuration spécifiques à chaque tenant (paramètres opérationnels, feature flags, limites de configuration).

**Ce module fait** :
- Lire la configuration d'un tenant (`TenantConfigApi.get(key)`).
- Gérer le CRUD admin de la configuration tenant.
- Cacher les valeurs de configuration (changent rarement).

**Ce module ne fait pas** :
- Évaluation des limites métier (→ `core.limitpolicy`).
- Gestion des jeux disponibles par tenant (→ `platform.tenantgame`).
- Profil utilisateur (→ `platform.identity`).

## 2. Structure

```text
platform/tenantconfig/
  api/
    TenantConfigApi.java      ← get(ConfigKey), getAll()
    model/
      TenantConfigView.java
      ConfigKey.java          ← enum ou string constant
  internal/
    service/
    persistence/
    web/                      ← TenantConfigAdminController (/api/v1/admin/config)
    cache/
    config/
```

## 3. Règles

- RLS actif — la configuration est toujours scoped au tenant courant.
- Caching agressif (TTL court, eviction sur update admin).
- `core.limitpolicy` consomme `TenantConfigApi` pour les paramètres opérationnels.
