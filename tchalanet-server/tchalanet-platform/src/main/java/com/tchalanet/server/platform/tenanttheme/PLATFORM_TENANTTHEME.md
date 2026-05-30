
# Platform Capability `platform.tenanttheme` — Tenant Theme Management

## Rôle

Gérer les overrides de thème (couleurs, logos, polices) par tenant, au-dessus des presets définis dans `catalog.theme`.

**Ce module fait** :
- Stockage et exposition des overrides de thème par tenant
- Calcul du thème effectif = preset catalog + overrides tenant
- CRUD admin des overrides

**Ce module ne fait pas** :
- Définition des presets de thème (voir `catalog.theme`)
- Rendu CSS/frontend (voir web app)

## Surface API

- `TenantThemeApi` (Java) : `getEffectiveTheme(TenantId)`
- Modèles : `TenantThemeView`, `ThemeOverride`

## Intégration

- Consomme `catalog.theme.api` pour lire les presets
- RLS actif sur les overrides
- Caching du thème effectif (TTL, evict sur update admin)

## Règles et limitations

- Les overrides sont propres à chaque tenant
