# apps/tchalanet-web — OpenSpec Project Context

## Périmètre

Ce OpenSpec couvre **uniquement** l'application Angular / Nx.

Périmètres inclus :

- Composants standalone Angular 20
- PageModel runtime et rendu des widgets
- NgRx store + effects + router-store
- Auth web (Keycloak OIDC, guards)
- Theming CSS variables, i18n (@ngx-translate, fr/en/ht)
- Shell privé, dashboard, pages publiques
- Libs partagées (`libs/shared/`, `libs/ui/`, `libs/web/`, `libs/i18n/`)
- Nx workspace config, targets, generators

## Ne pas inclure ici

- Changes backend Java → `tchalanet-server/openspec/`
- Changes Flutter / POS → `tchalanet-mobile/openspec/`
- Changes edge / notifications → `tchalanet-edge-service/openspec/`
- Coordination cross-projet → `openspec/` (racine)

## Conventions d'archivage

```bash
cd apps/tchalanet-web
openspec archive <change-id> --yes
openspec validate --strict
```

## Références

| Besoin               | Fichier                                        |
| -------------------- | ---------------------------------------------- |
| Conventions globales | `../../openspec/context/10-non-negotiables.md` |
| Stack Angular        | `CLAUDE.md` (ce répertoire)                    |
| Libs                 | `libs/**/README.md`                            |
