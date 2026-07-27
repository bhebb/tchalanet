# Spec: Web Shell Navigation

## ADDED Requirements

### Requirement: Un panneau de navigation replié se comporte comme un dialogue modal

Lorsqu'un panneau de navigation est affiché en mode overlay (`down(expanded)`), il
SHALL se comporter comme un dialogue modal :

- porter `role="dialog"` et `aria-modal="true"` ;
- piéger le focus clavier tant qu'il est ouvert ;
- rendre le contenu sous-jacent `inert` ;
- verrouiller le scroll du document ;
- se fermer sur `Escape` ;
- restituer le focus à l'élément déclencheur à la fermeture.

En mode déployé (`up(expanded)`), le même panneau SHALL NOT porter de sémantique
de dialogue ni piéger le focus : c'est un complément permanent de la page.

Cette bascule sémantique SHALL être pilotée par l'état de breakpoint observé en
TypeScript, afin qu'elle suive réellement le layout.

#### Scenario: Le focus reste dans le drawer ouvert

- **WHEN** l'utilisateur ouvre le drawer console au clavier sur un viewport
  inférieur à 840px et parcourt les éléments avec Tab
- **THEN** le focus reste à l'intérieur du drawer et ne peut pas atteindre le
  contenu situé derrière le scrim.

#### Scenario: Escape ferme et rend le focus

- **WHEN** le drawer ou l'overlay de navigation est ouvert et que l'utilisateur
  presse `Escape`
- **THEN** le panneau se ferme et le focus revient sur le bouton qui l'avait ouvert.

#### Scenario: La sidebar persistante n'est pas un dialogue

- **WHEN** une page console est affichée à 1280px
- **THEN** la sidebar est exposée comme une navigation permanente, sans
  `role="dialog"`, sans `aria-modal`, et le contenu principal n'est pas `inert`.

### Requirement: Le lien de navigation actif annonce la page courante

Tout composant de navigation qui met en évidence visuellement l'élément actif
SHALL exposer cet état via `aria-current="page"` sur le lien correspondant.

#### Scenario: L'élément actif de la sidebar est annoncé

- **WHEN** l'utilisateur se trouve sur une route couverte par un élément de
  `tch-sidebar-nav`
- **THEN** le lien correspondant porte `aria-current="page"`, y compris lorsqu'il
  est actif via un alias `activeRoutes` ou une correspondance `exact`.

#### Scenario: L'élément actif du header public est annoncé

- **WHEN** l'utilisateur se trouve sur une route de la navigation publique
- **THEN** le lien correspondant de `tch-nav` porte `aria-current="page"` dans les
  deux variantes du header.

### Requirement: La navigation publique mobile passe uniquement par l'overlay

Sur les viewports inférieurs à 840px, la navigation publique SHALL être accessible
par le bouton burger et son panneau overlay. Aucune barre de navigation basse
SHALL être rendue.

L'overlay SHALL rendre l'ensemble des éléments de navigation fournis par le
contrat runtime, y compris les destinations externes, avec leur icône lorsqu'elle
est déclarée.

#### Scenario: Les destinations externes ne sont pas perdues

- **WHEN** la navigation publique contient un élément dont la destination est une
  URL externe
- **THEN** cet élément est rendu dans l'overlay et reste activable.

### Requirement: Cibles tactiles conformes dans la navigation

Tout élément de navigation activable au doigt SHALL mesurer au moins 44px de haut,
et SHALL viser `var(--tch-touch-target, 48px)` lorsqu'il est présenté en mode
overlay.

#### Scenario: Les sous-éléments de la sidebar sont utilisables au doigt

- **WHEN** le drawer console est ouvert sur un viewport mobile
- **THEN** les entrées enfants des groupes mesurent au moins `--tch-touch-target`
  de haut, alors qu'elles conservent leur densité réduite sur sidebar persistante.

### Requirement: La navigation respecte les zones sûres de l'appareil

Les surfaces de navigation ancrées aux bords de l'écran SHALL tenir compte des
`env(safe-area-inset-*)` afin de rester utilisables sur les appareils à encoche ou
à barre gestuelle.

#### Scenario: La barre supérieure évite l'encoche

- **WHEN** une page console est affichée sur un appareil déclarant un
  `safe-area-inset-top` non nul
- **THEN** les contrôles de la barre supérieure restent entièrement visibles et
  activables.
