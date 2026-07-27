# Tasks — web-console-drawer-two-levels-v1

## 1. Socle partagé

- [x] Extraire la logique d'activité de route de `tch-sidebar-nav` vers
      `libs/ui/components/src/lib/navigation/route-activity.ts` — fonctions pures, testables sans
      routeur. Sans ça les deux rendus divergeraient sur ce qui est « actif ».
- [x] `tch-sidebar-nav` délègue à ces fonctions (refactor sans changement de comportement, couvert
      par les specs existantes).

## 2. Composant (`specs/web-console-drawer`)

- [x] `TchDrawerNav` : lignes pour les entrées sans enfants, grille de catégories pour les groupes,
      panneau glissant par catégorie, retour ne refermant que ce niveau.
- [x] Compteur de pages par catégorie ; `pagesLabelKey` prend une **clé** et non un libellé résolu,
      parce qu'il a besoin du paramètre `count` que seul le composant connaît.
- [x] Recherche au niveau racine, sur les libellés **traduits**, tous niveaux confondus.
- [x] Absorption de l'entrée d'atterrissage : enfant à la même route que le groupe **et**
      `activeMatch: 'exact'`.
- [x] `aria-current` sur l'entrée active, carte marquée quand la catégorie contient la route active.
- [x] Focus dans le panneau à l'ouverture, rendu à la carte à la fermeture.

## 3. Branchement

- [x] `private-shell-layout` : `tch-drawer-nav` sous 840px, `tch-sidebar-nav` au-dessus, piloté par
      `TchBreakpointService.isWide()`.
- [x] `Escape` referme un niveau à la fois.
- [x] Refermer le drawer remet la navigation à la racine.
- [x] Trois clés i18n en fr/en/ht : `nav.private.categories`, `nav.private.pages`,
      `nav.private.search`.
- [x] Retrait de l'entrée `density` de `tch-sidebar-nav` : introduite pour élargir les cibles
      tactiles du drawer replié, elle n'a plus d'objet puisque ce rendu ne passe plus par ce
      composant. Les lignes du drawer sont à `--tch-touch-target`.

## 4. Correction connexe

- [x] `TCH_TITLE_NAVIGATION` accepte une fonction : le titre d'onglet lisait le modèle **statique**
      pendant que le menu affichait la navigation **runtime** du backend. Les deux consoles
      fournissent désormais `sectionsFromRuntimeNavigation(bootstrap.navigationDrawer()) ?? <repli>`.

## 5. Tests

- [x] Vitest — rendu choisi selon le breakpoint, lignes vs cartes, ouverture de panneau, entrée
      absorbée, `Escape` à deux niveaux, retour à la racine, catégorie active.
- [x] Vitest — **garde sur les modèles réels** : la liste exacte des entrées absorbées
      (7 groupes) est figée, et les raccourcis vers un premier enfant (`references`, `access`,
      `support-and-content`, `tchala`, `reports`, `company`) sont vérifiés intacts. La règle étant
      structurelle, elle est silencieuse : un groupe qui gagnerait une `destination` ferait
      disparaître un enfant sans que rien ne le signale.
- [x] Adaptation des specs existantes : le heading de section et la densité n'existent plus sous
      840px, l'activation d'un item passe par `tch-drawer-nav`.

## 6. Vérification

- [x] `pnpm run test` — 16 projets verts, 68 tests sur `web-shell`.
- [x] `pnpm run lint` — vert.
- [x] Build **production** des 3 portails — vert.
- [x] `nx e2e web-e2e` — 18 tests verts.
- [x] `node tools/breakpoint-contract.mjs` — 0 violation sur 1014 fichiers.
- [ ] **Non vérifié en navigateur.** Le drawer ne s'affiche que dans une console authentifiée ; les
      specs e2e correspondantes sont `skip` sans émulateur Firebase ni identifiants. La couverture
      repose donc sur les tests unitaires de rendu, pas sur une observation directe.

## 7. Suites

- [ ] Étendre le projet e2e mobile aux consoles une fois l'émulateur disponible en local, pour
      observer le drawer dans un vrai navigateur.
- [ ] Décider côté contrat backend si `archives` et `audit` doivent déclarer une `destination` de
      groupe, pour que leur « Apèsi » soit absorbé comme celui d'`operations`.
