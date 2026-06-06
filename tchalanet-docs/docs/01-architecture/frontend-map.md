# Frontend map (Web + Mobile)

## Web (Angular/Nx)

- App docs : `tchalanet-web/*.md`
- Libs docs : `tchalanet-web/libs/**/README.md`

Structure active :

```text
apps/tch-portal/                 application Angular principale
libs/api/                        contrats et infrastructure HTTP transverses
libs/shared-config/              settings runtime et feature flags
libs/ui/theme/                   thème runtime
libs/ui/styles/                  primitives SCSS partagées
libs/ui/components/              composants UI réutilisables
```

`shared-auth`, `shared-i18n`, `page-model`, `widgets`, et `web` sont les prochaines cibles
d’extraction. Une lib cible est créée lorsqu’un changement y déplace un ensemble cohérent de code,
définit ses exports publics et valide ses frontières; on ne crée pas de coquilles vides.

`page-model` précède `widgets`. Les dashboards, shells, routes et pages par rôle appartiennent à
`web`.

OpenSpec packs:

- `openspec/context/05-version-guard.md`
- `tchalanet-web/AGENTS.md`
- component changes/specs under `tchalanet-web/openspec/`

## Mobile (Flutter)

- App/router docs : `tchalanet-mobile/AGENTS.md`
- Legacy app docs, if referenced: `apps/tchalanet-mobile/`

L’alignement du design system Mobile/POS sur la référence Web actuelle est un changement futur,
propriétaire du projet Mobile. Aucun état d’implémentation Flutter n’est déduit des docs Web.

OpenSpec pack:

- `tchalanet-mobile/openspec/`

## Convention

Les détails techniques vivent près du code. Ici, on maintient uniquement la carte.
