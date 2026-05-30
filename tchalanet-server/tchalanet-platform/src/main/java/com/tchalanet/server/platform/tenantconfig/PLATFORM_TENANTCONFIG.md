
# Platform Capability `platform.tenantconfig` — Tenant Configuration

## Rôle

Stocker et exposer les valeurs de configuration spécifiques à chaque tenant (paramètres opérationnels, feature flags, limites de configuration).

**Ce module fait** :
- Lecture de la config d’un tenant (`TenantConfigApi.get(key)`, `getAll()`)
- CRUD admin de la config tenant
- Caching des valeurs (TTL court, eviction sur update)

**Ce module ne fait pas** :
- Évaluation des limites métier (voir `core.limitpolicy`)
- Gestion des jeux par tenant (voir `platform.tenantgame`)
- Profil utilisateur (voir `platform.identity`)

## Surface API

- `TenantConfigApi` (Java) : `get`, `getAll`
- Modèles : `TenantConfigView`, `ConfigKey`

## Intégration

- RLS actif (config toujours scoped au tenant courant)
- Caching agressif
- Consommé par `core.limitpolicy` pour les paramètres opérationnels

## Règles et limitations

- Les valeurs changent rarement, eviction sur update admin
