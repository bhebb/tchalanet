# tchalanet-web — OpenSpec Project Context

## Périmètre

Ce OpenSpec couvre **uniquement** l'application Angular / Nx.

Périmètres inclus :

- Composants standalone Angular 22 (signals, signal forms, resource/rxResource)
- PageModel runtime et rendu des widgets
- État : signals dans les pages (pas de NgRx) — voir `docs/conventions/state-management.md`
- Auth web (Firebase, `@tch/core/auth`, guards) — voir `docs/auth-flow.md`
- Theming CSS variables, i18n (@ngx-translate, fr/en/ht)
- Shells public/privé, consoles admin/platform, pages publiques
- Libs partagées (`libs/api`, `libs/core/*`, `libs/ui/*`, `libs/web/*`, `libs/page-model`, `libs/widgets`)
- Nx workspace config, targets, generators

## Ne pas inclure ici

- Changes backend Java → `tchalanet-server/openspec/`
- Changes Flutter / POS → `tchalanet-mobile/openspec/`
- Changes edge / notifications → `tchalanet-edge-service/openspec/`
- Coordination cross-projet → `openspec/` (racine)

## Conventions d'archivage

```bash
cd tchalanet-web
openspec archive <change-id> --yes
openspec validate --strict
```

## Références

| Besoin               | Fichier                                        |
| -------------------- | ---------------------------------------------- |
| Conventions globales | `../../openspec/context/10-non-negotiables.md` |
| Stack Angular        | `CLAUDE.md` (ce répertoire)                    |
| Libs                 | `libs/**/README.md`                            |
