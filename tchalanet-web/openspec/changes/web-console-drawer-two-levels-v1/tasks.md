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

## 9. Bug d'empilement desktop (retour d'usage — capture #8)

- [x] **Bug réel, introduit par ce chantier.** `ConfigurableFocusTrapFactory.create(drawer)` insère
      deux sentinelles invisibles comme enfants directs de `.workspace`, jamais détruites hors mode
      overlay (`enabled = false` désactivait le piège mais laissait les sentinelles dans le DOM).
      Sur une grille à 2 colonnes, l'auto-placement CSS Grid les compte comme des items : drawer et
      contenu se retrouvaient poussés sur deux lignes empilées au lieu d'être côte à côte —
      confirmé par mesure DOM à 1280px (`.workspace` avait 5 enfants, `content.y` = `drawer.bottom`).
- [x] Corrigé : le piège est détruit — pas seulement désactivé — dès que
      `TchBreakpointService.isWide()` devient vrai.
- [x] Deux tests Vitest de régression : `.workspace` reste à exactement 3 enfants en sidebar
      permanente, et les sentinelles disparaissent bien au franchissement de 840px.
- [x] Revérifié en session réelle : capture avant/après, `.workspace` mesuré à 3 enfants,
      drawer (256px) et contenu (1024px) à la même hauteur.

## 10. Grille de cartes remplacée par une liste plate (retour d'usage v2)

Trois itérations du drawer replié : accordéon inline → grille de cartes 2 colonnes (livrée dans ce
chantier) → **liste plate avec chevron**, retenue. La grille faisait ressembler le menu à un second
tableau de bord ; le chevron `>` communique déjà « ceci mène ailleurs » sans avoir besoin d'un
habillage différent pour les entrées à enfants.

- [x] `TchDrawerNav` : un seul rendu de ligne (`rowTemplate`) pour toute la navigation — racine,
      panneau, recherche, bas de menu. Une entrée à enfants devient un `<button>` qui ouvre le
      panneau ; sans enfants, un `<a>` qui navigue. Les deux portent icône en tuile + libellé +
      chevron `chevron_right` : le chevron est **universel**, il annonce une destination, pas un
      dépliage — ce qui permet à une ligne de tête comme « Tablo bò » de le porter aussi.
- [x] `groups` simplifié : un bloc de liste par section (`{id, titleKey, items}`), sans plus séparer
      les entrées sans enfants (lignes) de celles avec enfants (cartes) — l'unification du rendu de
      ligne rend la distinction inutile en amont.
- [x] Bas de menu et racine partagent désormais le même `rowTemplate` — le rendu spécifique
      `drawer-nav__row--button` du footer (introduit à la tâche précédente) a disparu, absorbé par
      la généralisation.
- [x] SCSS : suppression de `.drawer-nav__grid` / `.drawer-nav__card*` (~70 lignes). L'icône gagne
      une tuile arrondie (`--tch-color-surface-container-high`) commune à toutes les lignes ; le
      chevron passe en `currentColor` + opacité pour rester lisible sur une ligne active
      (fond `--tch-color-accent`) sans redéfinir sa couleur par état.
- [x] Retrait de l'input `categoriesLabel`, devenu mort : les titres viennent uniquement du
      `titleKey` de section, plus d'un intitulé de secours pour un groupe de grille sans titre.
- [x] `pagesCount` / `pagesLabelKey` conservés **uniquement** pour l'en-tête du panneau (« Limit —
      5 paj ») : la liste plate n'affiche pas de compteur par ligne, contrairement à l'ancienne
      carte.
- [x] Sept tests du spec de drawer réécrits sur `[data-testid="drawer-category"]` (le rendu bouton),
      scopés pour exclure le footer quand une section pouvait en contenir un aussi
      (`Antrepriz mwen` sur le modèle réel).
- [x] Revérifié en session réelle : capture de la liste plate, du panneau, et du desktop — le
      correctif de la tâche précédente (bug d'empilement) tient sous le nouveau rendu.

**Scope tenu** : seule la conversion grille → liste est appliquée. Le renommage des sections
(« Operasyon »/« Sistèm »), l'ajout de « Kanal tiraj » comme entrée distincte, et l'ajout de
« Profil »/« Tèm » à la navigation n'ont pas été faits — non confirmés, et la proposition v2
elle-même classe le bouton « Tèm » hors périmètre (nettoyage pré-prod, pas une story de nav).

## 11. Découpage répercuté côté backend, et un bug réel trouvé en le faisant

Retour : « il faut voir le backend pour avoir la séparation entre opérations et configuration — on
n'a pas besoin de Sistèm ». Confirme qu'aucune troisième section n'est voulue ; demande que le
découpage devienne réel, pas seulement un repli front qui ne s'active que quand le backend échoue.

- [x] **Le backend servait déjà un `navigationDrawer` non vide**, à chaque requête —
      `tchalanet-server/.../pagemodel/fragments/private/tenantadmin/private_shell_tenantadmin.json`,
      chargé par `PrivateShellNavigationResolver`. Une seule section `admin` contenant tout,
      `secondary` vide. `company`/`help` incluses.
- [x] Fragment reformé : section `admin` (dashboard, sellers, draws, reports, tickets) + section
      `config` (setup, maryaj-gratis, games, limits, company), `help` déplacé dans `secondary`.
      Diff vérifié : seul le regroupement change, aucun item/enfant ajouté ou retiré.
- [x] Nouveau test backend (`PrivateShellNavigationResolverTest`) qui charge ce fragment **réel**
      via le vrai registre + un vrai `JsonMapper`, sans mock — le seul autre test de ce resolver
      mockait les deux collaborateurs, donc rien ne l'exerçait de bout en bout. Fige les deux
      sections et le contenu de `secondary`.
- [x] Backend vérifié : `PrivateShellNavigationResolverTest` (2/2), `PageRuntimeAssemblerTest` (3/3),
      checkstyle propre, spotless appliqué.
- [x] **Bug trouvé en vérifiant** : `RuntimeNavigationDrawer` (front) déclarait `topDestinations` /
      `footerDestinations` — les noms du record Java `NavigationDrawer`. Ce record n'est construit
      nulle part (`grep` : aucun `.of(`/`new NavigationDrawer(` dans tout le backend) ; le resolver
      qui sert réellement ce payload repasse le fragment JSON tel quel, qui nomme ces champs
      `primary`/`secondary`. Conséquence : `footerFromRuntimeNavigation()` lisait un champ toujours
      `undefined` face au vrai backend — le bas de menu retombait **silencieusement** sur le repli
      statique sur **toute** requête réelle, y compris le `secondary` non vide déjà servi par
      `private_shell_superadmin.json` (`releaseNotes`). Aucune des captures précédentes ne pouvait
      le révéler : le bootstrap stubbé de e2e force `navigationDrawer: null`, donc chaque capture
      n'a jamais exercé que le repli, jamais le mapping réel.
- [x] Corrigé : `footerFromRuntimeNavigation()` lit `secondary` en premier, `footerDestinations` en
      repli — même idiome que le reste du fichier (`labelKey`/`label_key`,
      `activeMatch`/`active_match`) déjà utilisé pour composer avec les divergences de nommage.
      `sectionsFromRuntimeNavigation()` n'avait pas ce problème : `sections` est identique des deux
      côtés.
- [x] Nouveau `runtime-navigation.mapper.spec.ts` — aucun test n'existait pour ce mapper. Fige
      `secondary` comme source réelle, `footerDestinations` comme repli, et l'ordre de préférence
      entre les deux.
- [x] `PLATFORM_NAVIGATION` **non touché** : resterait une section unique de neuf groupes, non
      demandé ici ; son fragment backend a déjà un `secondary` réel (`releaseNotes`), qui
      n'atteignait pas non plus le front avant la correction du mapper.

## 12. Suites

- [ ] Décider côté contrat backend si `archives` et `audit` doivent déclarer une `destination` de
      groupe, pour que leur « Apèsi » soit absorbé comme celui d'`operations`.
- [ ] `topDestinations`/`primary` du contrat reste non consommé par le shell (le `primary()` de
      chaque app est un concept local sans rapport, ex. bandeau support-access) : zone de raccourcis
      disponible si on veut un jour distinguer le quotidien sans passer par les sections.
- [ ] Le bootstrap stubbé des specs e2e (`tenantAdminPrivateBootstrap`/`superAdminPrivateBootstrap`
      dans `apps/web-e2e/src/support/api-stub.ts`) force `navigationDrawer: null` : aucun test e2e
      n'exerce le vrai mapping runtime. Élargir le stub avec un `navigationDrawer` réaliste
      toucherait les assertions de nombreux specs existants (auth-phase1/3, business-admin-v1) —
      à faire dans un change dédié, pas ici.
- [ ] Appliquer le même découpage « quotidien / configuration » à `PLATFORM_NAVIGATION` si voulu —
      hors périmètre de ce retour, qui ne portait que sur la console admin.

## 13. Robustesse du harnais e2e (trouvé en vérifiant ce qui précède)

- [x] `apps/web-e2e/playwright.config.cts` : les trois `webServer` n'avaient pas de `timeout`
      explicite (défaut Playwright 60s). En mode `emulator`, les trois serveurs démarrent en
      parallèle et **aucun n'est réutilisé** (`reuseExistingServer` désactivé en mode émulateur —
      voir le commentaire existant sur `emulatorTargets`), donc chaque run recompile à froid les
      trois apps en même temps. Mesuré directement (serveur lancé seul, hors Playwright) : **~80s**
      pour `admin-portal` en mode émulateur — au-delà du défaut, ce qui faisait échouer la suite
      entière avec `Timed out waiting 60000ms from config.webServer`, sans qu'aucun serveur n'ait
      réellement un problème.
- [x] `timeout: 180_000` sur les trois entrées. Vérifié : `admin-portal-mobile` — 7/7 verts en une
      seule tentative, la précédente ayant échoué trois fois de suite sur ce seul timeout.
