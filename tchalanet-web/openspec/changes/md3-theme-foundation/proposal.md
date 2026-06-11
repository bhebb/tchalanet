# Change: md3-theme-foundation

## Why

Une analyse croisée **docs conventions ↔ code des libs `ui/`** a montré que la base design web est
saine en intention mais dérive sur des points qui la rendent peu fiable comme socle d'une capacité
de sélection de thème MD3 :

- Le vocabulaire de contrat `--tch-*` est défini à **4 endroits** (`runtime-vars.scss`,
  `runtime-root.scss`, `theme-token-map.ts`, docs `style.md`/`theme.md`) qui **divergent** ; les
  listes documentées sont des sous-ensembles incomplets et incohérents de ce qui est réellement émis.
- Double source de vérité des breakpoints (`_breakpoints.scss` `sm/md/lg/xl` vs
  `TchBreakpointService` `handset/tablet/desktop`), sans table partagée.
- Des tokens prescrits par la doc ne sont **jamais émis** (`--tch-z-*`, scrim) ; `overlay-nav` viole
  alors la règle anti-z-index-magique qu'il est censé illustrer.
- Frontières Nx non outillées (`ui/theme` sans tags ; `ui/styles` sans lint/test ; `depConstraints`
  permissifs `* → *`).
- Le pipeline de génération M3 doit respecter **toutes** les bonnes pratiques MD3 — couleur **et**
  typographie/fonts/shape/densité — et la **liste des thèmes supportés est possédée par la
  plateforme** (le backend ne génère pas de hex/CSS) : la génération frontend doit être pilotée par
  ce set supporté.

Le backend implémente déjà l'essentiel de la capacité de sélection (catalog 12 MD3 + `tchalanet`,
`GET /admin/theme/presets`, `POST /admin/theme/preset`, `GET /{public,tenant}/theme/runtime`,
`/platform/catalog/theme-presets`). Le travail de ce change est **web-only** : assainir la fondation
theme/styles et la rendre **conforme MD3** et **auto-vérifiée**, pour que la sélection (réalisée dans
les dashboards superadmin/tenant-admin) repose sur une base correcte.

## What changes

- Établir un **manifeste de tokens canonique** (couleur + typo + fonts + shape + densité) et un
  **test de contrat à 4 voies** qui échoue dès qu'une des 4 définitions de `--tch-*` diverge.
- Auditer et corriger la **conformité MD3** du pipeline de génération (`_generate-theme.scss`,
  `theme-presets.scss`) : rôles couleur light+dark, échelle typographique, fonts, densité,
  `color-scheme`, tonal palettes ; aligner les ids du registry sur les `code` supportés par la
  plateforme.
- Émettre les tokens manquants (`--tch-z-*`, `--tch-color-scrim`) dans `runtime-root.scss`.
- Réconcilier `theme-token-map.ts` : les cibles d'override (`color.onSurface`, `shape.radius.md`)
  doivent atteindre les rôles `--tch-*` réellement consommés par les composants.
- Faire de `_breakpoints.scss` (`sm/md/lg/xl`) la **source unique** et documenter le mapping vers les
  paliers runtime.
- Outiller les **frontières** : tags Nx sur `ui/theme`, cibles lint/test sur `ui/styles`,
  `depConstraints` resserrées ; réconcilier le mixin focus avec `style.md §14`.
- Mettre à jour les docs (`style.md`, `theme.md`, `ARCHITECTURE.md`, `nx-boundaries.md`) : pointer le
  manifeste, documenter la chaîne génération↔runtime↔catalog, le périmètre thème complet, et le fait
  que **les overrides du thème courant sont déjà appliqués par `ThemeDomApplier`** (réconcilier
  l'écart `fontHref`/`--dark:` que la doc promet mais que `toOverrideCss` n'implémente pas).

## Impact

- Touche **uniquement `tchalanet-web`** : `libs/ui/theme`, `libs/ui/styles`, `docs/conventions`.
- Ne modifie pas le backend : la liste des thèmes supportés et le contrat runtime/catalog sont
  consommés tels quels ; les besoins serveur (figer la liste supportée consommable par la génération)
  sont notés comme **tâches de suivi** non bloquantes.
- Ne construit pas le theme-switcher (il vit dans les dashboards superadmin/tenant-admin) ni ne
  restyle les composants `ui/components` (passe ultérieure).
- Prépare les slices suivants : génération pilotée par le set supporté + consommation du catalog,
  puis application des conventions aux composants.

## Non-goals

- Aucun nouvel endpoint backend ; aucune génération de hex/CSS côté serveur.
- Aucun theme-switcher UI dans ce change.
- Aucun restyle des composants `ui/components` ni des features.
- Aucun theme-builder custom tenant (besoin futur).
- Aucun changement arbitraire de valeurs de marque : toute modif de palette vise la fidélité au seed
  plateforme.
