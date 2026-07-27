# Spec: Web Responsive Baseline

## ADDED Requirements

### Requirement: Une frontière de navigation unique à 840px

Toute bascule entre navigation repliée (drawer / overlay) et navigation déployée
(sidebar persistante / nav inline) SHALL se produire à la borne M3 `expanded`
(840px), et à aucune autre valeur.

- En SCSS, la bascule SHALL s'exprimer par `ui.up(expanded)` ou `ui.down(expanded)`.
- En TypeScript, elle SHALL s'exprimer par `TchBreakpointService.isWide()`.
- Les paliers `medium`, `large` et `extra-large` MAY piloter la densité (padding,
  typographie, nombre de colonnes) mais SHALL NOT faire apparaître ou disparaître
  un moyen de navigation.

#### Scenario: Le shell public bascule à 840px et pas avant

- **WHEN** la fenêtre passe de 839px à 840px de large sur une page publique
- **THEN** le bouton burger disparaît et la navigation inline du header apparaît,
  en une seule transition.

#### Scenario: Le shell privé bascule à la même borne

- **WHEN** la fenêtre passe de 839px à 840px de large sur une page console
- **THEN** le drawer overlay laisse place à la sidebar persistante, sans qu'aucune
  autre largeur ne produise de changement de mode de navigation.

### Requirement: Aucune valeur de breakpoint littérale hors du module dédié

Les media queries des shells et des briques UI de navigation/console SHALL
utiliser les mixins de `libs/ui/styles/src/lib/_breakpoints.scss`. Une valeur en
pixels écrite directement dans un `@media (min-width:` / `(max-width:` SHALL être
refusée par la CI, sauf dans `_breakpoints.scss` lui-même.

Les composants dont les styles sont inline dans le décorateur `@Component` et qui
ont besoin d'un breakpoint SHALL externaliser leurs styles dans un fichier `.scss`
compagnon référencé par `styleUrl`, seul moyen d'accéder aux mixins.

#### Scenario: Un breakpoint littéral échoue en CI

- **WHEN** un fichier en périmètre déclare `@media (max-width: 823px)`
- **THEN** la vérification de conformité échoue en nommant le fichier fautif.

### Requirement: Tokens de design existants uniquement

Les composants SHALL référencer des tokens présents dans
`libs/ui/theme/src/registry/token-manifest.generated.ts`. Un token inventé, même
assorti d'un fallback qui masque l'erreur, SHALL être corrigé.

#### Scenario: Les cibles tactiles utilisent le token réel

- **WHEN** un composant dimensionne une cible tactile
- **THEN** il utilise `var(--tch-touch-target, 48px)` et non un nom absent du
  manifeste tel que `--tch-size-touch-target`.

### Requirement: Hauteur de viewport dynamique sur les surfaces plein écran

Les surfaces qui occupent la hauteur de la fenêtre SHALL utiliser `100dvh` et non
`100vh`, afin de ne pas déborder sous la barre d'adresse mobile.

#### Scenario: Le shell privé ne déborde pas sur mobile

- **WHEN** une page console est affichée dans un navigateur mobile dont la barre
  d'adresse est visible
- **THEN** la hauteur du shell suit le viewport réellement disponible, sans
  générer de scroll parasite.
