# Spec: Web Console Drawer

## ADDED Requirements

### Requirement: La navigation repliée se parcourt en deux niveaux

Sous la borne `expanded` (840px), la navigation des consoles SHALL présenter, au niveau racine, les
entrées sans enfants sous forme de lignes et les entrées avec enfants sous forme de **cartes de
catégorie**. Activer une catégorie SHALL ouvrir un panneau listant ses enfants.

Le retour du panneau SHALL ne refermer que ce niveau, laissant le drawer ouvert. Refermer le drawer
SHALL remettre la navigation au niveau racine.

À partir de `expanded`, la navigation SHALL rester déployée en un seul niveau.

#### Scenario: Un groupe ne pousse plus le reste du menu hors écran

- **WHEN** l'utilisateur ouvre le menu sur un écran étroit et active une catégorie
- **THEN** les enfants de cette catégorie s'affichent dans un panneau dédié, sans que les autres
  entrées du menu aient à être parcourues.

#### Scenario: Le retour ne referme qu'un niveau

- **WHEN** un panneau de catégorie est ouvert et que l'utilisateur active le retour
- **THEN** le menu revient à la grille des catégories et reste ouvert.

#### Scenario: Rouvrir le menu repart de la racine

- **WHEN** l'utilisateur referme le menu depuis un panneau de catégorie puis le rouvre
- **THEN** le menu s'affiche au niveau racine.

### Requirement: Les deux rendus consomment le même modèle

Le rendu replié et le rendu déployé SHALL dériver du même `NavigationSection[]`, sans donnée
supplémentaire : un item avec enfants est une catégorie, un item sans enfants est une ligne.

Les deux SHALL s'accorder sur l'entrée active, en partageant la même règle de correspondance de
route.

#### Scenario: Le menu venu du backend est rendu comme le repli statique

- **WHEN** la navigation provient du contrat runtime plutôt que du modèle statique
- **THEN** les groupes qu'elle déclare deviennent des catégories, sans configuration additionnelle.

### Requirement: L'en-tête de catégorie remplace son entrée d'atterrissage

Lorsqu'un groupe déclare une destination et qu'un de ses enfants mène à cette même route en
correspondance exacte, cet enfant SHALL NOT être listé dans le panneau : l'en-tête SHALL être le
lien vers cette route.

Un enfant qui mène à la destination du groupe **sans** correspondance exacte SHALL rester listé —
la destination n'est alors qu'un raccourci vers le premier enfant, pas une vue d'ensemble.

#### Scenario: La vue d'ensemble n'apparaît pas deux fois

- **WHEN** l'utilisateur ouvre une catégorie dont un enfant est la vue d'ensemble du groupe
- **THEN** le panneau ne liste pas cet enfant, et son titre mène à cette vue d'ensemble.

#### Scenario: Un raccourci vers le premier enfant ne le fait pas disparaître

- **WHEN** un groupe pointe vers son premier enfant sans que celui-ci soit une vue d'ensemble
- **THEN** cet enfant reste listé dans le panneau.

### Requirement: Le menu se ferme un niveau à la fois au clavier

`Escape` SHALL refermer le panneau de catégorie lorsqu'il est ouvert, et le drawer sinon.

L'ouverture d'un panneau SHALL amener le focus dans ce panneau ; sa fermeture SHALL le rendre à la
carte de catégorie qui l'avait ouvert.

#### Scenario: Deux Escape pour sortir

- **WHEN** un panneau de catégorie est ouvert et que l'utilisateur presse `Escape` deux fois
- **THEN** le premier referme le panneau et le second le drawer.
