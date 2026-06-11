# md3-theme-foundation

## ADDED Requirements

### Requirement: Le vocabulaire `--tch-*` a une définition canonique vérifiée

Le vocabulaire de tokens `--tch-*` (couleur, typographie, fonts, shape, densité) SHALL avoir un
manifeste canonique unique. Un test de contrat SHALL vérifier la cohérence entre le pont
`runtime-vars.scss`, le fallback `runtime-root.scss`, les cibles de `theme-token-map.ts` et la liste
documentée. Les docs `style.md`/`theme.md` SHALL pointer ce manifeste au lieu de recopier une liste.

#### Scenario: Un token couleur n'a pas de fallback

- **WHEN** un token couleur est défini dans le pont `runtime-vars.scss` mais absent du fallback
  `:root` de `runtime-root.scss`
- **THEN** le test de contrat échoue

#### Scenario: Une cible d'override n'existe pas

- **WHEN** `theme-token-map.ts` mappe une clé backend vers un `--tch-*` qui n'est pas émis
- **THEN** le test de contrat échoue

#### Scenario: Un token documenté est introuvable

- **WHEN** une convention documente un token `--tch-*` qui n'est jamais émis
- **THEN** le test de contrat échoue

### Requirement: Le pipeline de génération respecte les bonnes pratiques MD3

Le pipeline de génération (`_generate-theme.scss`, `theme-presets.scss`) SHALL produire, pour chaque
thème supporté, un thème Material Design 3 complet en mode clair et sombre couvrant couleur,
typographie, fonts, shape et densité. Les ids du registry généré SHALL correspondre aux `code` des
thèmes supportés par la plateforme.

#### Scenario: Un preset est généré

- **WHEN** `theme:generate` produit le CSS d'un thème supporté
- **THEN** il expose les rôles couleur M3 light+dark, l'échelle typographique
  (display/headline/title/body/label), la/les font(s) déclarée(s) et la densité
- **AND** son id correspond à un `code` supporté par la plateforme

#### Scenario: La plateforme possède la liste des thèmes supportés

- **WHEN** la génération frontend produit le set de thèmes
- **THEN** elle produit exactement les thèmes supportés par la plateforme
- **AND** aucun hex/CSS n'est généré par le backend

### Requirement: Les tokens non-thématiques requis sont émis

`runtime-root.scss` SHALL émettre les tokens statiques prescrits par les conventions, dont
`--tch-z-header`, `--tch-z-drawer`, `--tch-z-overlay`, `--tch-z-toast` et `--tch-color-scrim`.

#### Scenario: Un composant a besoin d'un z-index thématisé

- **WHEN** un composant lit `--tch-z-overlay` ou `--tch-color-scrim`
- **THEN** le token est défini et résolu dans le DOM sous `.tch-theme`

### Requirement: Les breakpoints ont une source unique

`_breakpoints.scss` (`sm`/`md`/`lg`/`xl`) SHALL être la source unique des bornes responsive. Le
mapping vers les paliers runtime SHALL être documenté.

#### Scenario: Une media query SCSS et le service runtime utilisent les mêmes bornes

- **WHEN** une media query SCSS et `TchBreakpointService` ciblent le même palier
- **THEN** ils dérivent de la même table de bornes documentée

### Requirement: Les frontières des libs `ui` sont outillées

`libs/ui/theme` SHALL porter des tags Nx (`type:ui`, `scope:shared`) à parité avec `libs/ui/styles`.
`libs/ui/styles` SHALL exposer des cibles `lint` et `test`. Les `depConstraints` Nx SHALL empêcher
une lib `type:ui` de dépendre de `features` ou `data-access`.

#### Scenario: Une lib `ui` tente une dépendance interdite

- **WHEN** une lib taguée `type:ui` importe une lib `features` ou `data-access`
- **THEN** `nx lint` échoue sur la contrainte de dépendance

### Requirement: La documentation reflète l'application des overrides

Les conventions `theme.md`/`style.md` SHALL documenter que les overrides du thème courant
(couleur, typographie, font, densité) sont appliqués par `ThemeDomApplier`, et SHALL refléter
fidèlement ce que `toOverrideCss` implémente réellement.

#### Scenario: La doc décrit une capacité d'override

- **WHEN** la doc décrit l'application d'un override (`fontHref`, vars `--dark:`)
- **THEN** soit le code l'implémente, soit la doc est corrigée pour ne pas le promettre
