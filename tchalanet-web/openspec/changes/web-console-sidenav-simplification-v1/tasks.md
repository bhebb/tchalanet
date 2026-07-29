# Tasks — Console Sidenav Simplification V1

Décisions verrouillées dans `proposal.md`. Rien n'est implémenté — ceci détaille ce qu'il y aura à
faire une fois qu'on lance le code.

## 1. Backend — `private_shell_tenantadmin.json`

- [x] `sellers` : ajouter `"path": "/app/admin/seller-terminals"` sur l'item parent. Aucun autre
      changement — `sellers-list` a déjà `active_match: "exact"` sur cette même route.
- [x] `draws` : ajouter `"path": "/app/admin/draws"` sur l'item parent. `draws-all` a déjà
      `active_match: "exact"` sur cette même route.
- [x] `reports` : ajouter `"path": "/app/admin/reports/daily"` sur l'item parent, marquer
      `reports-daily` en `active_match: "exact"`. Retirer `reports-overview` de `children` (la
      route `/app/admin/reports/overview` n'est pas supprimée, juste plus liée depuis le menu).
- [x] `tickets` : ajouter `"path": "/app/admin/tickets"` sur l'item parent (déjà la route de
      `tickets-list`, déjà `active_match: "exact"`). Retirer `tickets-overview` de `children`.
- [x] `company` : ajouter `"path": "/app/admin/business-profile"` sur l'item parent, marquer
      `company-identity` en `active_match: "exact"`.
- [x] Corriger `nav.admin.reports_sales` (FR/EN/HT) : ne doit plus afficher « Tikè ». → FR
      « Ventes », EN « Sales », HT « Vant ».
- [x] Renommer l'un des deux libellés « Règ tiraj » (FR/EN/HT) — en fait les deux étaient
      identiques en HT seulement (FR/EN les distinguaient déjà) : `draws_channels` → HT
      « Konfigirasyon tiraj » (aligné sur FR « Configuration des tirages »), `limits_draw` → HT
      « Kanal » (aligné sur FR « Canaux », réutilise le mot déjà établi ailleurs).
- [x] Vérifier le comportement du mapper (`runtime-navigation.mapper.ts`) quand un item a `path`
      **et** `children` en même temps — vérifié : `destination` (depuis `path`) et `children`
      sont dérivés indépendamment du même `entry`, aucun ne masque l'autre. Rien à corriger.
- [x] Étendre `PrivateShellNavigationResolverTest` avec ces 5 groupes pour verrouiller le nouveau
      `path` (`fiveGroupsGainedTheirOwnDestination`, 3/3 tests verts).
- [x] Aucune route, aucune permission modifiée — uniquement `sections[].items[].path` et deux clés
      i18n.

## 2. Frontend — modèle statique (`private-navigation.model.ts`)

- [x] Miroir des 5 ajouts ci-dessus dans `TENANT_ADMIN_NAVIGATION` (fallback statique) — `sellers`/
      `draws` avaient déjà leur `destination`, propre divergence pré-existante avec le backend ;
      `reports`/`tickets`/`company` alignés (destination `/app/admin/reports/daily`, retrait de
      `tickets-overview`, `activeMatch: 'exact'` sur `company-identity`).

## 1b. Backend + modèle statique — porté à `PLATFORM_NAVIGATION` (élargissement de périmètre)

- [x] `private_shell_superadmin.json` : `tenants` → `"path": "/app/platform/tenants"` (le modèle
      statique l'avait déjà, le fragment backend non — même écart que les 5 groupes admin).
- [x] `operations` → `"path": "/app/platform/ops"` (même écart).
- [x] `audit` → `"path": "/app/platform/audit"` (nouveau : `audit-functional` exact-match, un seul
      candidat évident). Miroir ajouté dans `PLATFORM_NAVIGATION` (`destination` sur `audit`).
- [x] `archives` → `"path": "/app/platform/archives"` (même raisonnement, `archive-overview`).
      Miroir ajouté dans `PLATFORM_NAVIGATION`.
- [x] Laissés sans destination, volontairement : `dashboard` (deux enfants exact-match à égalité,
      santé technique vs commercial — aucun n'est "le" défaut), `access`, `references`,
      `support-and-content`, `tchala` (aucun enfant candidat évident).
- [x] `PrivateShellNavigationResolverTest.platformGroupsWithASingleLandingPageGainedTheirOwnDestination`
      — verrouille les 4 nouveaux `path` et l'absence de `path` sur les 5 groupes laissés tels quels.
      4/4 tests verts sur la classe.
- [x] Tests `private-shell-drawer.spec.ts` : `tenants`/`operations`/`archives`/`audit` ajoutés à la
      liste d'absorption attendue ; l'ancien test `cannot absorb anything from a group that declares
      no destination` (qui listait `archives`/`audit`) réécrit pour couvrir les 5 groupes qui
      restent réellement sans destination.
- [x] `admin-portal:build` et `platform-portal:build` : verts, aucune erreur de compilation.

## 3. Frontend — nouveau modèle d'interaction (ligne = navigation, chevron = expand/collapse)

Ce point ne concerne qu'un seul jeu de composants (`tch-drawer-nav`/`tch-sidebar-nav`), partagé par
les deux consoles : aucun travail supplémentaire n'était nécessaire pour que le point 1b en profite
aussi.

> **Partiellement retiré depuis, voir section 7.** Ce qui suit décrit la scission ligne/chevron
> telle qu'implémentée d'abord — appliquée aux deux rendus. Le retrait côté mobile (retour
> utilisateur réel) et ses raisons sont documentés en section 7 et dans `specs/web-console-sidenav/
> spec.md` ; gardé ici tel quel pour la trace de ce qui a été essayé et pourquoi.

Actuellement, taper/cliquer la ligne d'un groupe (mobile ou desktop) ouvre toujours le
panneau/accordéon — même quand le groupe a une destination propre. Sur mobile, atteindre cette
destination demande alors de taper le **titre** du panneau ouvert, sans indice visuel que ce titre
est un lien. Ce comportement change :

- [x] `tch-drawer-nav` : séparer la zone cliquable en deux — `.drawer-nav__row-link` (libellé/icône,
      navigue directement) et `.drawer-nav__row-expand` (chevron, bascule le panneau). Un groupe
      sans destination propre garde le bouton pleine ligne existant.
- [x] `tch-sidebar-nav` : même séparation pour l'accordéon desktop — `.sidebar__group-link` +
      `.sidebar__group-expand`, extraction de `childrenTemplate` partagée entre les deux branches
      pour ne pas dupliquer le rendu des enfants.
- [x] Un item avec `children.length > 0` affiche un chevron ; un item sans enfants n'en affiche
      aucun (`Tablo bò`, `Konfigirasyon jeneral`, `Maryaj gratis` restent des liens simples) —
      inchangé, déjà le cas.
- [x] Le chevron est un vrai bouton, `aria-expanded`, avec un label accessible
      (« Développer/Réduire {libellé} ») — nouvelles clés i18n `nav.private.expand`/`collapse`
      (fr/en/ht).
- [x] Une seule interaction déclenche une seule action — vérifié par test (clic libellé ne bascule
      pas le panneau ; clic chevron ne navigue pas).
- [x] Le panneau (mobile) reste accessible pour parcourir les sous-pages même quand le groupe est
      absorbé.
- [x] Un seul parent actif à la fois ; l'indicateur du sous-lien actif reste secondaire — inchangé.
- [ ] Sidebar desktop réduite (rail icône seul) : non applicable — cette sidebar n'a pas de mode
      rail aujourd'hui, rien à faire ici tant que ce mode n'existe pas.

## 4. Tests unitaires

- [x] Un item sans enfants n'affiche aucun chevron — déjà couvert, inchangé.
- [x] Un item avec enfants affiche un chevron — déjà couvert, inchangé.
- [x] Cliquer le libellé/icône d'un groupe absorbé navigue directement, sans ouvrir le panneau —
      `navigates on the row label without opening the panel`.
- [x] Cliquer le chevron bascule l'expansion sans navigation —
      `toggles the panel from the chevron without navigating`.
- [x] Une sous-route active garde son parent visuellement actif — `marks the category holding the
      active route`, adapté au nouveau conteneur.
- [x] `aria-expanded` reflète l'état ouvert/fermé — couvert par le test ci-dessus (chevron).
- [x] Régression : liste d'absorption étendue à `reports`/`company` dans
      `absorbs exactly the landing entries, and nothing else` ; `reports` retiré de la liste des
      raccourcis non absorbables dans `leaves shortcut destinations alone`.
- [x] `web-shell:test` : 74/74 verts. `PrivateShellNavigationResolverTest` : 3/3 verts.
- [ ] `tch-sidebar-nav` n'a pas de spec dédiée équivalente à `private-shell-drawer.spec.ts` pour le
      desktop — la couverture actuelle passe par les tests `private-shell-drawer.spec.ts` en mode
      `isWide(true)` (existants, non étendus ici avec les mêmes cas que mobile). À compléter si on
      veut la même profondeur de test des deux côtés.

## 5. Tests E2E (Playwright)

Réécrit pour coller au comportement réel post-#438 (la scission ligne/chevron n'existe que sur
desktop ; mobile garde le comportement pré-#437, sans filtrage du panneau). Nouveau fichier
`apps/web-e2e/src/admin-portal/sidenav-desktop.spec.ts`, sélecteurs sur `href` plutôt que sur le
texte traduit (la locale e2e n'est pas figée) :

- [x] Desktop : cliquer le libellé « Tèminal POS » ouvre directement `/app/admin/seller-terminals`
      (pas d'accordéon intermédiaire).
- [x] Desktop : cliquer le chevron de « Tèminal POS » déplie les enfants sans navigation.
- [x] Desktop : un second clic sur le chevron replie à nouveau, toujours sans naviguer.
- [x] Desktop : un groupe et son enfant actif ne partagent jamais le même aplat plein
      (`/app/admin/reports/daily`, cas exact du bug post-merge de la section 7).
- [x] Rechargement direct sur `/app/admin/limits/number` : bon sous-lien actif dans le bon groupe.
- [x] Rechargement direct sur `/app/admin/tickets/sell` : bon sous-lien actif dans le bon groupe.
- [x] Mobile (`apps/web-e2e/src/mobile/admin-drawer.spec.ts`, réécrit) : le panneau liste tous les
      enfants d'une catégorie, atterrissage compris, et le titre du panneau n'est jamais un lien.
- [ ] Non fait — nécessite des identifiants admin que je n'ai pas et ne fabrique pas moi-même
      (même position que sur la vérification visuelle live, section 6) : exécution réelle de ces
      specs. Écrites, `web-e2e:lint` vert (0 erreur), découverte confirmée via
      `playwright test --list` (24 tests dans 7 fichiers, les nouveaux y figurent). Pas de preuve
      d'exécution au-delà de ça.
- [ ] Largeurs intermédiaires (600, 839px) et dark mode : pas couvertes, hors du scope de ce retour
      (`sidenav-desktop.spec.ts` utilise le viewport desktop par défaut du projet
      `admin-portal` ; `admin-drawer.spec.ts` le viewport mobile fixe de `admin-portal-mobile`).

## 6. Vérification

- [x] `openspec validate web-console-sidenav-simplification-v1 --strict` — vert.
- [x] `admin-portal:build:development` — vert, aucune erreur de compilation.
- [x] Vérification visuelle desktop sur `test.tchalanet.com/admin` après déploiement backend
      (PR #437) — label navigue, chevron replie/déplie, `reports-sales` affiche « Vant ». Refait
      après PR #438 — surbrillance correcte, un seul aplat plein à la fois.
- [x] Vérification visuelle mobile — faite par l'utilisateur sur un vrai iPhone (capture fournie
      dans la conversation) après le retrait du filtrage du panneau : « Rapò » ouvre son panneau
      avec ses 4 pages listées, atterrissage compris. Je n'ai pas pu la refaire moi-même
      (redimensionnement de fenêtre indisponible dans cette session).
- [x] `reports-sales` n'affiche plus « Tikè » ; les deux « Règ tiraj » ne sont plus identiques —
      vérifié par lecture directe des 3 fichiers de traduction après édition.

## 7. Corrections post-merge (retour utilisateur réel, 2026-07-28/29)

PR #438, mergée après #437. Voir `specs/web-console-sidenav/spec.md` pour le détail normatif.

- [x] Sidebar desktop : un groupe actif et son enfant actif ne partagent plus le même aplat de
      couleur plein — le groupe garde la teinte discrète `.is-active-group`, l'aplat plein reste
      réservé à l'enfant qui est effectivement la page courante.
- [x] Bug de premier correctif trouvé en vérifiant en direct : retirer la règle
      `.sidebar__group-link.is-active` sans la remplacer laissait le sélecteur générique
      `a.is-active` (même spécificité, correspondance directe) continuer à s'appliquer — corrigé en
      neutralisant explicitement `background`/`color`/`font-weight` sur cette règle plutôt qu'en la
      supprimant.
- [x] Drawer mobile : retrait de la scission libellé/chevron — la ligne entière rouvre le panneau,
      comme avant PR #437. Décision utilisateur explicite après démonstration que le drawer mobile
      n'a pas d'accordéon persistant pour révéler les pages sœurs après navigation, contrairement à
      la sidebar desktop.
- [x] Panneau mobile : retrait du filtrage de l'entrée d'atterrissage (`absorbedChild()` supprimé,
      `openCategoryItems` ne filtre plus). Le titre de panneau redevient un texte simple. Décidé
      après capture d'écran utilisateur montrant que la sidebar desktop n'a jamais filtré cette
      entrée (« Lis tikè » y est une ligne normale) — les deux rendus s'accordent maintenant.
- [x] Tests : suite `absorption de l'entrée d'atterrissage` retirée (documentait un comportement qui
      n'existe plus nulle part) ; remplacée par une suite plus modeste sur « quels groupes ont une
      destination propre ». Nouveau test drawer confirmant que le panneau liste tous les enfants,
      atterrissage compris.
- [ ] Reste ouvert, non redemandé depuis : badge/sous-titre « X paj » sur les lignes racine pour
      rendre plus visible qu'un groupe ouvre une liste plutôt qu'il ne navigue — proposé, pas
      tranché.
