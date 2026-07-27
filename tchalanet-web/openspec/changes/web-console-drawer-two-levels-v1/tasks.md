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
- [x] **Vérifié en navigateur.** Projet Playwright `admin-portal-mobile` (390×844) + émulateur
      Firebase : 6 tests verts sur une session réelle, et captures des trois états (racine, panneau
      `Limit`, sidebar à 1280px).
- [x] Les assertions attendent la fin des glissements (200ms). Sans ça, une mesure prise en cours
      d'animation lit une position intermédiaire — c'est ce qui m'a d'abord fait conclure à tort à
      un défaut d'empilement du panneau.
- [x] `inert` sur le niveau racine pendant qu'un panneau est ouvert : ses liens restaient tabulables
      derrière lui. Trouvé en enquêtant sur la fausse piste ci-dessus.

## 7. Bas de menu (retour d'usage)

- [x] **Le contrat runtime avait déjà un footer.** `RuntimeNavigationDrawer` déclare
      `topDestinations`, `sections`, `footerDestinations` et `actions` depuis l'origine ; seul
      `sections` était consommé, d'où l'entreprise et l'aide rangées parmi les activités métier.
      Ajout de `footerFromRuntimeNavigation()`.
- [x] `company` et `help` sortent de `TENANT_ADMIN_NAVIGATION` vers `TENANT_ADMIN_FOOTER`, miroir
      statique du contrat. Les deux consoles alimentent le slot `secondary` du shell, jusque-là
      inutilisé.
- [x] Zone footer du drawer, poussée en bas et séparée d'un filet. Une entrée à enfants y reste une
      entrée à enfants : « Antrepriz mwen » ouvre le même panneau à 7 entrées, elle quitte seulement
      la grille des catégories métier.
- [x] Le footer entre aussi dans la source des titres d'onglet — sinon les pages de l'entreprise
      perdaient le leur.
- [x] **`box-sizing` manquant sur `.drawer-nav`** : `height: 100%` plus 28px de padding débordait de
      la fenêtre, et la dernière entrée du bas de menu passait sous le bord (mesurée à 856px pour un
      viewport de 844). Trouvé par la capture, corrigé, et verrouillé par une assertion e2e sur la
      position de la dernière ligne.

## 8. Rang par section, pas par nombre d'enfants (retour d'usage)

Constat : « Konfigirasyon jeneral » et « Maryaj gratis » occupaient le haut du menu — la zone la
plus visible — alors que ce sont des réglages rarement rouverts. Ce n'était pas un choix : la règle
« entrée sans enfants → ligne en tête » faisait du **nombre d'enfants** le critère de rang.

- [x] `TENANT_ADMIN_NAVIGATION` passe d'**une** section à **deux** : `admin` (Tablo bò, Tèminal POS,
      Tiraj, Rapò, Tikè) et `config` (Konfigirasyon jeneral, Maryaj gratis, Jwèt, Limit, Antrepriz
      mwen). `NavigationSection` supportait déjà plusieurs sections titrées ; le menu n'en déclarait
      qu'une.
- [x] Le drawer rend **un bloc de grille par section**, avec son titre. Les entrées sans enfants
      restent des lignes, mais **dans leur bloc** — plus en tête de menu.
- [x] `company` remonte du bas de menu vers la section `config` : c'est un réglage, pas du service.
      Le bas de menu ne garde que `Èd`.
- [x] Clé i18n `nav.admin.section.config` en fr/en/ht.
- [x] Les assertions du spec de modèle cherchent une entrée **dans toutes les sections + le footer**,
      au lieu de coder en dur `NAVIGATION[0].items` — elles portent sur le contenu des entrées, pas
      sur la zone qui les héberge.

## 9. Suites

- [ ] Décider côté contrat backend si `archives` et `audit` doivent déclarer une `destination` de
      groupe, pour que leur « Apèsi » soit absorbé comme celui d'`operations`.
- [ ] **Répercuter le découpage côté backend.** `sections` et `footerDestinations` sont résolus
      indépendamment (`runtime ?? repli`). Si le backend continue d'envoyer `company` ou `help` dans
      `sections` en laissant `footerDestinations` vide, ces entrées apparaîtront **deux fois**. Le
      contrat porte aussi `topDestinations`, toujours non consommé : c'est la zone des raccourcis si
      on veut un jour distinguer le quotidien du reste sans passer par les sections.
- [ ] Appliquer le même découpage à `PLATFORM_NAVIGATION`, qui reste une section unique de neuf
      groupes.
