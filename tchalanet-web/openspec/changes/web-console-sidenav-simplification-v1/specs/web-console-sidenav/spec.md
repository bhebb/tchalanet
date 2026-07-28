# Spec: Web Console Sidenav

## ADDED Requirements

### Requirement: La ligne d'un groupe navigue, le chevron replie ou déplie

Quand un groupe de navigation a une destination propre (`path` sur l'item), cliquer ou taper le
libellé ou l'icône de sa ligne SHALL naviguer directement vers cette destination, sans ouvrir de
panneau ni d'accordéon intermédiaire. Cette règle SHALL s'appliquer identiquement au drawer mobile
(`tch-drawer-nav`, <840px) et à la sidebar permanente (`tch-sidebar-nav`, ≥840px).

Le chevron, quand un groupe a des enfants, SHALL uniquement basculer l'affichage de ses sous-pages
(panneau sur mobile, sous-liste sur desktop) — il SHALL NOT déclencher de navigation.

Un groupe sans destination propre SHALL conserver le comportement actuel : sa ligne entière ouvre
le panneau ou l'accordéon, faute d'avoir un lien direct à offrir.

#### Scenario: Un clic sur le libellé atteint la page directement

- **WHEN** l'utilisateur clique le libellé de « Tèminal POS » (destination
  `/app/admin/seller-terminals`)
- **THEN** la navigation a lieu immédiatement, sans étape intermédiaire de panneau ou d'accordéon.

#### Scenario: Le chevron ne navigue jamais

- **WHEN** l'utilisateur clique uniquement le chevron de « Limit »
- **THEN** ses sous-pages s'affichent ou se masquent, et aucune navigation n'a lieu.

#### Scenario: Un groupe sans destination garde son comportement de catégorie pure

- **WHEN** un groupe sans `path` propre (ex. `archives` côté platform) est cliqué
- **THEN** son panneau ou accordéon s'ouvre comme aujourd'hui — il n'y a rien d'autre à faire, ce
  groupe n'a pas de page à atteindre directement.

### Requirement: Le chevron n'apparaît que si le groupe a des sous-pages

Un item de navigation SHALL afficher un chevron si et seulement si il a au moins un enfant. Un item
sans enfant (ex. `Tablo bò`, `Konfigirasyon jeneral`, `Maryaj gratis`) SHALL rester un simple lien,
sans aucun indicateur d'expansion.

#### Scenario: Un lien simple n'a pas de chevron

- **WHEN** `Maryaj gratis` (aucun enfant) est affiché dans le menu
- **THEN** aucun chevron n'apparaît à côté de son libellé.

### Requirement: Le chevron est un contrôle accessible distinct

Le chevron SHALL être un élément interactif propre (`button`), exposant `aria-expanded` reflétant
l'état du panneau ou de l'accordéon, avec un label accessible nommant l'action et le groupe
concerné.

#### Scenario: Un lecteur d'écran annonce l'état du chevron

- **WHEN** un utilisateur de lecteur d'écran atteint le chevron d'un groupe replié
- **THEN** il entend une annonce équivalente à « Développer {libellé du groupe} », et
  `aria-expanded="false"`.
