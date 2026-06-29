# Frontend map (Web + Mobile)

## Web (Angular/Nx)

- App docs : `tchalanet-web/*.md`
- Libs docs : `tchalanet-web/libs/**/README.md`

Structure active :

```text
apps/public-portal/              surface publique canonique, SSR/hydration-ready
apps/admin-portal/               surface tenant admin, déployable seule
apps/platform-portal/            surface plateforme/superadmin, déployable seule
apps/tch-portal/                 portail historique privé pendant migration
apps/web-e2e/                    Playwright e2e unique public/admin/platform
libs/api/                        contrats et infrastructure HTTP transverses
libs/core/auth/                  login partagé, session, guards et auth runtime
libs/core/i18n/                  runtime i18n partagé
libs/page-model/                 contrats, API et renderer PageModel
libs/shared-assets/              assets, logos, fonts, i18n JSON et markdown partagés
libs/shared-config/              settings runtime et feature flags
libs/web/errors/                 normalisation et présentation des erreurs web
libs/web/shell/                  layouts shell public/privé et feedback shell
libs/ui/theme/                   thème runtime
libs/ui/styles/                  primitives SCSS partagées
libs/ui/components/              composants UI réutilisables
libs/widgets/                    widgets concrets PageModel
```

Les libs sont créées seulement lorsqu’un changement y déplace un ensemble cohérent de code, définit
ses exports publics et valide ses frontières; on ne crée pas de coquilles vides.

La surface publique a été extraite de `tch-portal` vers `public-portal`. Les apps admin et platform
sont déployables indépendamment. La vente POS reste lazy-loaded dans `admin-portal` en V0; il n'y a
pas de `pos-portal` pour le moment.

Les tests e2e Web vivent dans `apps/web-e2e`, avec un dossier par surface. Le run standard Playwright
utilise Chromium et démarre les apps sur des ports locaux stables :

```text
public-portal   http://localhost:4301
admin-portal    http://localhost:4302
platform-portal http://localhost:4303
```

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
