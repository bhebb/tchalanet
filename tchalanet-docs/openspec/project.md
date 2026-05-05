# tchalanet-docs — OpenSpec Project Context

## Périmètre

Ce OpenSpec couvre **uniquement** la documentation MkDocs.

Périmètres inclus :

- Documentation architecture et ADR
- Documentation business et flows
- Guides développeur et onboarding
- Structure MkDocs (nav, plugins, mkdocs.yml)

## Ne pas inclure ici

- Changes backend → `tchalanet-server/openspec/`
- Changes Angular → `apps/tchalanet-web/openspec/`
- Changes Flutter → `tchalanet-mobile/openspec/`
- Changes edge → `tchalanet-edge-service/openspec/`
- Changes infra → `tchalanet-infra/openspec/`
- Coordination cross-projet → `openspec/` (racine)

## Conventions d'archivage

```bash
cd tchalanet-docs
openspec archive <change-id> --yes
openspec validate --strict
```
