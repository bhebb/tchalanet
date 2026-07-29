# Tasks — web-console-draws-card-ux-v1

## 1. Vérification préalable

- [ ] `grep -r "ConsoleDrawsTableComponent" apps/ libs/` pour confirmer que
      `generated-draws-table.component.ts` (admin-portal) est le seul consommateur en mode carte
      avec actions de cycle de vie. Si un autre consommateur existe, vérifier qu'il tolère le passage
      au menu `⋮` sans régression silencieuse.

## 2. En-tête de carte (identité) et heure locale

- [ ] `console-draw-slot-identity.component.ts`/`.html` : ne plus rendre `providerLabel()` (provider
      + heure provider) sur la carte mobile. Décider en implémentation entre (a) un nouvel input
      dédié (ex. `showProviderMeta`) piloté par le composant appelant, ou (b) réutiliser `density()`
      existant si son usage actuel (`compact`/`comfortable`) le permet sans effet de bord sur les
      autres consommateurs de ce composant (vérifier tous les appelants avant de réutiliser
      `density`).
- [ ] `localLabel()` (heure locale/tenant) sort de l'en-tête d'identité et se rend sous le badge
      d'état (bloc « état de vente », tâche 3) — visible pour un tirage fermé, absent quand un
      compte à rebours est déjà affiché (tirage ouvert).
- [ ] Page de détail du tirage (`admin-draw-result-detail.page`) : confirmer qu'elle continue
      d'afficher Provider et heure locale — pas un consommateur de `ConsoleDrawsTableComponent`,
      donc pas affectée par le changement ci-dessus, mais à vérifier visuellement.

## 3. État de vente et résultat

- [ ] `console-draws-table.component.html` : sous le badge d'état, afficher l'heure locale
      (`localLabel()`, ou équivalent porté par `ConsoleDrawRow`) quand il n'y a pas de compte à
      rebours à afficher (tirage fermé) ; ne pas les afficher tous les deux ensemble.
- [ ] Retirer le `<dt>Résultat</dt>` du bloc carte (`__card-facts`) — le tableau desktop garde son
      `<th>Résultat</th>`, non touché.
- [ ] Ne pas rendre le bloc `__card-facts` sur la carte quand `resultLabel`, `resultNumbers` et
      `resultHint` sont tous absents/vides (actuellement un tiret `—` de remplissage).
- [ ] `generated-draws-table.component.ts` : nouveau champ (ex. `resultActionHint` sur
      `ConsoleDrawRow`, ou calcul local) qui reformule `EXPECTED`/`MISSING` en « Résultat à saisir »
      (si `canEnterManualResult(draw)`) / « Résultat manquant » sinon — uniquement pour le rendu
      carte. Ne pas modifier `consoleDrawResultStatusLabel` (partagé avec le tableau desktop et la
      page de détail).

## 4. Actions secondaires

- [ ] `console-draws-table.component.html` (bloc carte uniquement) : remplacer le rendu à plat des
      actions de cycle de vie par un bouton `more_vert` ouvrant un `mat-menu` listant ces actions.
      L'action principale (`primaryAction()`, déjà déterminée) reste hors menu, en bouton plein.
      **Chaque item du menu affiche son icône ET son libellé texte** (`mat-icon` + texte dans le
      `mat-menu-item`, jamais l'icône seule) — retour explicite : les icônes seules ne sont pas
      assez comprises par tous les administrateurs.
- [ ] Ne pas rendre le bouton `⋮` quand aucune action de cycle de vie n'est disponible pour ce
      tirage.
- [ ] Conserver le spinner `pending` existant à la même place (action en cours).

## 5. SCSS

- [ ] **Corriger le sélecteur trop large** dans `console-draws-table.component.scss`, bloc
      `@include ui.down(expanded)` : `.mat-mdc-button-base { width: 100%; }` sous `&__card-actions`
      s'applique aujourd'hui à tous les boutons Material du conteneur, y compris les icon-buttons —
      confirmé en capture réelle sur `test.tchalanet.com/admin` (chaque icon-button de cycle de vie
      s'étirait sur sa propre ligne pleine largeur). Une fois les actions de cycle de vie passées au
      menu `⋮` (tâche 4), ce conteneur n'aura plus qu'un bouton, mais scoper la règle à une classe
      dédiée au bouton principal évite de reproduire le bug si un bouton s'y ajoute plus tard.
- [ ] Ajuster `&__card-facts` pour disparaître proprement quand vide (pas de bordure/padding
      orphelins).
- [ ] Vérifier la densité verticale gagnée après retrait de la ligne Provider et du bloc résultat
      vide — objectif : plus de cartes visibles par écran, sans changement du breakpoint lui-même.

## 6. Tests

- [ ] Vitest (`console-draw-view-models.spec.ts` ou nouveau spec dédié à la carte) : reformulation
      `EXPECTED`/`MISSING` → « Résultat à saisir » / « Résultat manquant », avec les deux branches de
      `canEnterManualResult`.
- [ ] Vitest : bloc résultat absent quand rien à afficher (pas de tiret de remplissage).
- [ ] Vitest : heure locale affichée sous le badge d'état pour un tirage fermé, absente quand un
      compte à rebours est affiché ; jamais d'heure provider sur la carte.
- [ ] Vitest : menu `⋮` présent seulement s'il y a au moins une action de cycle de vie ; action
      principale toujours seule hors menu ; chaque item du menu porte un libellé texte non vide.
- [ ] Playwright (`admin-portal-mobile`, 390×844) : ouverture du menu `⋮` sur une carte, activation
      d'une action de cycle de vie depuis le menu — vérifier qu'aucun icon-button ne s'étire plus en
      pleine largeur (régression de la tâche 5).

## 7. Vérification

- [ ] `pnpm nx run-many -t lint,test -p web-console,admin-portal`
- [ ] `pnpm nx build admin-portal`
- [ ] Vérifié en navigateur sur `test.tchalanet.com/admin` (draws), viewport mobile : identité,
      état + heure locale, résultat, action principale, menu `⋮` — comparé avant/après par capture,
      en particulier sur un tirage qui reproduisait le bug d'empilement (`TX-10:00`).
- [ ] `openspec validate --strict`
