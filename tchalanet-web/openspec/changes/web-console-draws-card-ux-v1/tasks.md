# Tasks — web-console-draws-card-ux-v1

## 1. Vérification préalable

- [x] `grep -r "ConsoleDrawsTableComponent" apps/ libs/` trouve **deux** consommateurs, pas un
      seul comme supposé dans la proposition initiale : `generated-draws-table.component.ts`
      (admin-portal) et `platform-ops-draws.page.ts` (platform-portal) — tous deux avec des
      actions `variant: 'icon'` de cycle de vie. Le passage au menu `⋮`, le retrait de la ligne
      Provider, l'heure locale sous le badge d'état et la suppression du bloc résultat vide sont
      implémentés au niveau du composant partagé (`ConsoleDrawsTableComponent` /
      `ConsoleDrawSlotIdentityComponent`), donc bénéficient aux deux sans toucher leur code. Seule
      la reformulation `resultActionHint` (« Résultat à saisir »/« Résultat manquant ») est
      admin-portal-spécifique : `platform-ops-draws` ne définit jamais ce champ, donc son rendu
      résultat pour un tirage sans résultat reste inchangé (aucun badge affiché, plutôt qu'un
      tiret désormais — améliorat­ion de bord, pas une régression).

## 2. En-tête de carte (identité) et heure locale

- [x] `console-draw-slot-identity.component.ts` : nouvel input `showTechnicalMeta` (défaut
      `true`, donc aucun changement pour les autres consommateurs de ce composant : desktop table,
      page détail, matrix-slot-panel, etc.). `ConsoleDrawsTableComponent` le passe à `false`
      uniquement sur la carte mobile — `providerLabel()`/`providerNameLabel()` ne rendent plus
      rien dans ce cas.
- [x] `localLabel()` extrait en fonction pure `consoleDrawSlotLocalLabel()` dans
      `console-draw-identities.ts`, réutilisée à la fois par le composant d'identité (inchangé pour
      les autres consommateurs) et par `ConsoleDrawsTableComponent.localLabel(row)` pour le rendu
      sous le badge d'état.
- [x] Page de détail du tirage : pas un consommateur de `ConsoleDrawsTableComponent`, donc son
      affichage de Provider/heure locale (via l'identité en `showTechnicalMeta` par défaut `true`)
      est inchangé.

## 3. État de vente et résultat

- [x] `console-draws-table.component.html` : nouveau bloc `__card-state` sous l'en-tête —
      countdown si présent, sinon `localLabel(row)` (jamais les deux ensemble).
- [x] `<dt>Résultat</dt>` retiré du rendu carte ; le tableau desktop garde `<th>Résultat</th>`
      inchangé.
- [x] Le bloc résultat ne rend plus rien quand `resultActionHint`, `resultLabel`, `resultNumbers`
      et `resultHint` sont tous absents — plus de tiret `—` de remplissage sur la carte.
- [x] `ConsoleDrawRow.resultActionHint` (nouveau champ optionnel, propagé par
      `consoleDrawRowViewModel`) ; `generated-draws-table.component.ts` le calcule via
      `canEnterManualResult(draw)` pour `EXPECTED`/`MISSING` uniquement. `resultLabel`/
      `consoleDrawResultStatusLabel` restent inchangés — le tableau desktop et la page détail
      continuent de les lire directement.

## 4. Actions secondaires

- [x] Bouton `more_vert` + `mat-menu` sur la carte mobile ; l'action principale est désormais
      `row.actions[0]` par convention (déjà l'ordre construit par chaque consommateur — vérifié
      pour les deux), rendue en bouton plein hors menu. Chaque item du menu porte icône **et**
      libellé texte (`<span class="material-symbols-outlined">…</span>{{ action.label }}`, même
      convention que `seller-terminal-table.component.html`).
- [x] Le bouton `⋮` ne se rend que si `!row.pending && row.actions.length > 1`.
- [x] Le spinner `pending` reste affiché à la même place logique (juste avant l'action principale).

## 5. SCSS

- [x] `.mat-mdc-button-base { width: 100% }` (trop large, confirmé par capture réelle) remplacé par
      une classe dédiée `&__primary-action { width: 100% }`, qui ne cible que le bouton principal.
- [x] `&__card-facts`/`&__card-fact` (devenues mortes, plus utilisées par le template) supprimées ;
      nouveau `&__card-state` (flex, wrap) et `&__local-time` (texte discret, même ton que le
      countdown neutre).
- [ ] Densité verticale gagnée pas mesurée en navigateur (bloqué — voir tâche 7).

## 6. Tests

- [x] `console-draw-identities.spec.ts` : `consoleDrawSlotLocalLabel` — jamais l'heure/le fuseau
      provider, date locale omise si identique à la date provider, `null` sans donnée locale.
- [x] `generated-draws-table.component.spec.ts` (nouveau) : « Résultat à saisir » quand
      `canEnterManualResult` est vrai, « Résultat manquant » sinon (délai non atteint OU permission
      absente), `resultActionHint` non défini dès qu'un résultat existe.
- [x] `console-draws-table.component.spec.ts` (nouveau) : `primaryAction`/`secondaryActions` —
      le premier élément est toujours l'action principale, le reste va au menu, aucune action ⇒
      aucun menu ; `localLabel` ne contient jamais l'heure/le fuseau provider.
- [ ] Playwright (`admin-portal-mobile`, 390×844) — pas ajouté dans cette passe : nécessite une
      session authentifiée dans le stub e2e existant pour ce parcours de tirages, à faire dans un
      passage dédié plutôt que de complexifier ce change.

## 7. Vérification

- [x] `pnpm nx run-many -t lint,test -p web-console,admin-portal,platform-portal` — 0 erreur
      (warnings pré-existants uniquement), 64 tests verts sur `web-console`, 27 sur `admin-portal`.
- [x] `pnpm nx run-many -t build -p admin-portal,platform-portal` — les deux compilent sans erreur.
- [ ] **Vérification navigateur bloquée** : `test.tchalanet.com` et le serveur de dev local
      exigent tous deux une authentification Firebase — je n'entre pas d'identifiants (règle de
      sécurité), et je n'en ai pas reçu. Reste à faire par l'utilisateur avant merge : ouvrir
      `/app/admin/draws` sur mobile (≤839px) et comparer avec la maquette avant/après déjà partagée
      dans la conversation, en particulier sur un tirage qui reproduisait le bug d'empilement
      (`TX-10:00`).
- [x] `openspec validate --strict` — vert.
