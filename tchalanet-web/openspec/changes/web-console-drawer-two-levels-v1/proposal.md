# OpenSpec Change — Console Drawer Two Levels V1

## Status

Proposed — 2026-07-27

## Why

Sous 840px, les consoles rendent leur menu avec le même accordéon que la sidebar permanente. Le
modèle compte 11 entrées racine côté admin, 10 côté platform, dont sept et neuf groupes
respectivement — jusqu'à 10 enfants chacun. Déplier un groupe pousse tout le reste hors écran ; la
verticale d'un téléphone ne suffit pas à ce que l'accordéon suppose acquis.

S'y ajoute une redondance : chaque groupe qui a une vue d'ensemble la répète en premier enfant
(« Apèsi »), alors que le groupe lui-même y mène déjà.

## Decision (locked)

- **Deux niveaux sous 840px** : le drawer montre les entrées sans enfants en lignes et les groupes
  en **grille de catégories**. Ouvrir une catégorie fait glisser un panneau par-dessus, dont le
  retour ne referme que ce niveau.
- **La sidebar permanente garde l'accordéon** au-dessus de 840px : la place verticale y est
  disponible, et un niveau de moins vaut mieux qu'un niveau de plus.
- **Une seule source de données.** Les deux rendus consomment le même `NavigationSection[]`, qu'il
  vienne du backend (`navigationDrawer()`) ou du repli statique. Un item avec enfants est une
  catégorie, un item sans enfants est une ligne — rien à déclarer en plus.
- **L'en-tête de catégorie absorbe l'entrée d'atterrissage** : l'enfant qui pointe vers la même
  route que le groupe **et** porte `activeMatch: 'exact'` n'est plus listé, le titre du panneau y
  mène.
- **Recherche dans le menu** au niveau racine, sur les libellés traduits de tous les niveaux.

## What Changes

- `TchDrawerNav` dans `@tch/ui/components`.
- `route-activity.ts` : la logique d'activité de route sort de `tch-sidebar-nav` pour être partagée
  par les deux rendus — sinon ils divergeraient sur ce qui est « actif ».
- `private-shell-layout` choisit le rendu selon `TchBreakpointService.isWide()`.
- `Escape` referme un niveau à la fois : panneau de catégorie, puis drawer.
- Trois clés i18n nouvelles (`nav.private.categories`, `nav.private.pages`, `nav.private.search`)
  en fr/en/ht.
- Retrait de l'entrée `density` de `tch-sidebar-nav` : elle servait à élargir les cibles tactiles
  du drawer replié, qui n'utilise plus ce composant.

## Impact

- Admin et platform uniquement — seules surfaces montant `tch-private-shell-layout`. Le portail
  public n'a qu'un niveau de navigation et n'est pas concerné.
- Aucun changement du modèle de navigation, ni des routes, ni du contrat backend.

## Non-goals

- Pas d'uniformisation du modèle : `archives` et `audit` ont un « Apèsi » mais aucune `destination`
  de groupe, leur en-tête n'a donc nulle part où mener et la ligne reste. Le menu venant du
  backend, cela se décide côté contrat, pas dans le repli statique.
- Pas de refonte visuelle au-delà de la structure.
- Pas de nouveau pattern d'onglets.
