# AccessControl — Domaine fonctionnel

...existing code...

## Cross-apps (Web / Mobile)

### Web (UI)

- Pages / routes :
  - (ex) `/app/admin/users` — gestion des rôles/permissions
  - (ex) `/app/admin/roles` — catalogue de rôles
- Components/widgets :
  - (ex) `RoleEditor`, `PermissionMatrix`
- i18n namespaces :
  - (ex) `access.*`, `users.*`

### Mobile (POS)

- Écrans :
  - (ex) “Mon profil” — permissions visibles
- Offline/Sync :
  - (ex) restrictions d’accès si offline

### API (contrats)

- Endpoints :
  - (ex) `GET /api/v1/admin/access/roles`
  - (ex) `POST /api/v1/admin/access/assign-role`
- Notes :
  - scopes admin, `@PreAuthorize` via `hasPermission(...)`

> Source of truth :
>
> - Backend : `tchalanet-server/src/main/java/com/tchalanet/server/core/accesscontrol/DOMAIN_ACCESS_CONTROL.md`
> - Web : `apps/...` / `libs/.../README.md`
> - Mobile : (chemins quand prêts)

## Pointeurs (source of truth near-code)

- Règles et modèle backend: `99-links/_ref/server/domains/core/accesscontrol/DOMAIN_ACCESS_CONTROL.md`
