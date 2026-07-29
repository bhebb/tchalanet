# Spec: Web Console Sidenav

## ADDED Requirements

### Requirement: Sur desktop, la ligne d'un groupe navigue, le chevron replie ou déplie

Quand un groupe de navigation a une destination propre (`path` sur l'item), sur la sidebar
permanente (`tch-sidebar-nav`, ≥840px), cliquer le libellé ou l'icône de sa ligne SHALL naviguer
directement vers cette destination, sans déplier l'accordéon. Le chevron, séparé, SHALL uniquement
basculer l'affichage des sous-pages — il SHALL NOT déclencher de navigation.

Un groupe sans destination propre SHALL conserver le comportement de catégorie pure : sa ligne
entière bascule l'accordéon, faute d'avoir un lien direct à offrir.

**Cette règle ne s'applique qu'à la sidebar desktop.** Le drawer mobile ne la reprend pas — voir
la décision ci-dessous.

#### Scenario: Un clic sur le libellé atteint la page directement

- **WHEN** l'utilisateur clique le libellé de « Tèminal POS » (destination
  `/app/admin/seller-terminals`) sur la sidebar desktop
- **THEN** la navigation a lieu immédiatement, sans déplier l'accordéon.

#### Scenario: Le chevron ne navigue jamais

- **WHEN** l'utilisateur clique uniquement le chevron de « Limit » sur la sidebar desktop
- **THEN** ses sous-pages s'affichent ou se masquent, et aucune navigation n'a lieu.

#### Scenario: Un groupe sans destination garde son comportement de catégorie pure

- **WHEN** un groupe sans `path` propre (ex. `archives` côté platform) est cliqué
- **THEN** son accordéon s'ouvre comme aujourd'hui — il n'y a rien d'autre à faire, ce groupe n'a
  pas de page à atteindre directement.

### Requirement: Le chevron desktop est un contrôle accessible distinct

Sur la sidebar desktop, le chevron d'un groupe avec destination propre SHALL être un élément
interactif propre (`button`), exposant `aria-expanded` reflétant l'état de l'accordéon, avec un
label accessible nommant l'action et le groupe concerné.

#### Scenario: Un lecteur d'écran annonce l'état du chevron

- **WHEN** un utilisateur de lecteur d'écran atteint le chevron d'un groupe replié, sur desktop
- **THEN** il entend une annonce équivalente à « Développer {libellé du groupe} », et
  `aria-expanded="false"`.

### Requirement: Sur mobile, la ligne entière ouvre le panneau

Le drawer mobile (`tch-drawer-nav`, <840px) SHALL NOT reprendre la scission libellé/chevron du
desktop : pour tout groupe ayant des enfants, taper n'importe où sur sa ligne SHALL ouvrir le
panneau listant ses sous-pages.

**Décision, pas un oubli.** Le desktop scinde parce que l'accordéon reste visible en permanence et
se rouvre automatiquement sur le groupe actif après navigation — le libellé peut donc naviguer
directement sans rien cacher. Le drawer mobile se referme entièrement après toute navigation ; sans
accordéon persistant pour révéler les pages sœurs après coup, le petit chevron aurait été la seule
façon de jamais les découvrir. Testé et confirmé confus en usage réel (retour utilisateur,
2026-07-28) : la scission a été portée sur mobile puis retirée dans la même session.

#### Scenario: Taper une ligne à enfants ouvre toujours le panneau, jamais une navigation directe

- **WHEN** l'utilisateur tape la ligne « Tiraj » dans le drawer mobile
- **THEN** le panneau de « Tiraj » s'ouvre, quel que soit l'endroit tapé sur la ligne.

### Requirement: Le panneau mobile liste tous les enfants, atterrissage compris

Le panneau du drawer mobile SHALL lister tous les enfants d'une catégorie sans filtrage, y compris
celui dont la route correspond exactement à la destination du groupe (ex. « Lis tikè » dans
« Tikè »). Le titre du panneau SHALL rester un texte simple, non interactif.

**Décision, pas un oubli.** Une version antérieure masquait cet enfant et rendait le titre du
panneau cliquable à sa place — mais un titre ressemble à du texte, pas à un lien, donc la page
d'atterrissage devenait, en pratique, injoignable pour un utilisateur qui ne savait pas taper
dessus. La sidebar desktop n'a jamais fait ce filtrage ; les deux rendus s'accordent maintenant.

#### Scenario: La page d'atterrissage a sa propre ligne dans le panneau

- **WHEN** l'utilisateur ouvre le panneau « Tikè » dans le drawer mobile
- **THEN** « Lis tikè » apparaît comme une ligne cliquable ordinaire dans la liste, au même titre
  que « Vann ».

### Requirement: Le chevron n'apparaît que si le groupe a des sous-pages

Un item de navigation SHALL afficher un chevron si et seulement si il a au moins un enfant. Un item
sans enfant (ex. `Tablo bò`, `Konfigirasyon jeneral`, `Maryaj gratis`) SHALL rester un simple lien,
sans aucun indicateur d'expansion. Vrai sur les deux rendus.

#### Scenario: Un lien simple n'a pas de chevron

- **WHEN** `Maryaj gratis` (aucun enfant) est affiché dans le menu, mobile ou desktop
- **THEN** aucun chevron n'apparaît à côté de son libellé.

### Requirement: Une seule surbrillance pleine à la fois sur la sidebar desktop

Quand un groupe et l'un de ses enfants sont actifs simultanément (destination du groupe identique à
la route de l'enfant actif — le cas courant depuis ce change), la sidebar desktop SHALL réserver
l'aplat de couleur plein au seul enfant qui est effectivement la page courante. Le libellé du groupe
SHALL recevoir uniquement la teinte discrète déjà utilisée pour signaler « la page active est dans
ce groupe », jamais le même aplat plein que l'enfant.

**Décision, pas un oubli.** Une première implémentation donnait le même aplat plein au libellé du
groupe et à son enfant actif, empilant deux blocs de couleur identiques l'un sur l'autre pour une
seule localisation — retour utilisateur direct (2026-07-28) après déploiement.

#### Scenario: Le groupe et son enfant actif ne sont jamais pleins tous les deux

- **WHEN** l'utilisateur est sur `/app/admin/reports/daily`, où « Rapò » (groupe) et « Jounen »
  (enfant) sont tous deux actifs
- **THEN** seul « Jounen » reçoit l'aplat de couleur plein ; « Rapò » reste sur la teinte discrète.
