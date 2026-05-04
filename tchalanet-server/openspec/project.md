# tchalanet-server — OpenSpec Project Context

## Périmètre

Ce OpenSpec couvre **uniquement** le backend Java / Spring Boot.

Périmètres inclus :

- Migrations Flyway et schéma PostgreSQL
- Couches hexagonales : `common`, `catalog`, `core`, `features`
- CQRS : commands, queries, handlers, ports, adapters
- Domaines métier : draw, drawresult, sales, tickets, audit, batch, cache
- API REST admin et public
- Keycloak / OAuth2 resource server
- RLS multi-tenant
- Events domaine après commit

## Ne pas inclure ici

- Changes Angular / Nx → `apps/tchalanet-web/openspec/`
- Changes Flutter / POS → `tchalanet-mobile/openspec/`
- Changes edge / notifications → `tchalanet-edge-service/openspec/`
- Coordination cross-projet → `openspec/` (racine)

## Conventions d'archivage

```bash
cd tchalanet-server
openspec archive <change-id> --yes
openspec validate --strict
```

## Références

| Besoin               | Fichier                                           |
| -------------------- | ------------------------------------------------- |
| Architecture couches | `openspec/context/10-non-negotiables.md` (racine) |
| Nommage              | `docs/NAMING.md`                                  |
| Typed IDs            | `docs/conventions/typed_ids.md`                   |
| RLS                  | `docs/conventions/persistence/rls.md`             |
| Events               | `docs/conventions/event_model.md`                 |
| Tests                | `docs/conventions/testing.md`                     |
| Domaine métier       | `src/**/DOMAIN_*.md`                              |
