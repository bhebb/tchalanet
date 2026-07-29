# OpenSpec Change — Console Draws Card UX V1

## Status

Proposed — 2026-07-29

## Why

Retour utilisateur sur `ConsoleDrawsTableComponent` (la carte mobile de `console-draws-table.component.html`,
utilisée par la page Tirages admin) : trop d'informations techniques rivalisent avec l'essentiel pour un
administrateur peu familier des interfaces web. Vérifié dans le code actuel :

- L'en-tête de carte (`tch-console-draw-slot-identity`) affiche déjà le code fonctionnel en titre
  (ex. `NY-SOIR`, calculé par `title()` dans
  [console-draw-slot-identity.component.ts](../../../libs/web/console/src/lib/draw-slots/console-draw-slot-identity.component.ts))
  et le libellé humain en sous-titre — **mais** ajoute ensuite deux lignes `providerLabel()` /
  `localLabel()` (« Provider: … », « Local: … ») qui remettent du bruit technique juste en dessous.
- Le résultat affiche un libellé « RÉSULTAT » explicite dans `console-draws-table.component.html:34`
  (`<dt>Résultat</dt>`) avant le badge + les numéros.
- Un tirage sans résultat affiche le badge `Attendu` ou `Manquant`
  ([console-draw-labels.ts:78-79](../../../libs/web/console/src/lib/draws/console-draw-labels.ts)) —
  correct comme statut technique, mais pas assez explicite comme action pour un administrateur.
- Le bouton d'action principal existe déjà et varie déjà correctement selon l'état
  (`Saisir résultat` / `Voir détails` / `Vérifier`, voir `primaryAction()` dans
  [generated-draws-table.component.ts:365-386](../../../apps/admin-portal/src/app/features/draws/components/generated-draws-table/generated-draws-table.component.ts)) —
  mais toutes les actions secondaires (cycle de vie : ouvrir/fermer/verrouiller/déverrouiller/annuler/
  archiver) sont rendues comme des icon-buttons **à plat**, à égalité visuelle avec l'action principale,
  dans `console-draws-table__card-actions`.
- La carte affiche une carte de résultat toujours présente (`<dl class="…__card-facts">`), même quand
  la vente est simplement fermée sans rien à en dire.
- **Bug de layout confirmé sur staging** (capture réelle de `test.tchalanet.com/admin`, tirage
  `TX-10:00`) : dans `console-draws-table.component.scss`, le bloc `down(expanded)` déclare
  `.mat-mdc-button-base { width: 100%; }` sur `&__card-actions`. Cette classe couvre **tous** les
  boutons Material, y compris les icon-buttons de cycle de vie — chacun se retrouve donc étiré à
  100% de largeur dans un conteneur `flex-wrap: wrap`, et prend sa propre ligne. Une carte avec
  3 actions de cycle de vie (fermer/verrouiller/annuler) affiche 3 lignes vides en plus du bouton
  principal, pour un total de 4 lignes d'actions empilées.
- **Icônes seules mal comprises.** Retour utilisateur : les icônes de cycle de vie (verrou, croix,
  carré « stop ») ne portent pas un sens assez clair pour un administrateur peu familier des
  conventions d'interface — une icône seule, sans libellé, est un pari sur une culture visuelle
  qu'on ne peut pas supposer acquise.

Ce change ne touche que le rendu **carte mobile** (`down(expanded)`, sous 840px). Le rendu tableau
desktop (`console-draws-table__scroller`) garde ses colonnes Provider/Mode/Publication — un
administrateur sur desktop a la place et l'habitude de la densité tabulaire ; ce n'est pas la
plainte formulée.

## Decision (locked)

- **Hiérarchie de carte à quatre blocs, ordre fixe** : identité (code fonctionnel + libellé humain) →
  état de vente → résultat (si disponible) → action principale. Les actions secondaires quittent la
  ligne d'actions pour un menu `⋮`.
- **Provider masqué, heure locale conservée.** Le nom du provider et l'heure provider ne sont
  jamais affichés sur la carte — ils restent nécessaires (diagnostic d'un décalage horaire,
  vérification manuelle) mais vont dans `admin-generated-draws.page` → page de détail du tirage
  (`admin-draw-result-detail.page`). L'**heure locale (tenant)**, elle, reste visible sur la carte
  sous le badge d'état — un administrateur doit pouvoir situer un tirage fermé (« Fermé · 19h00 »)
  sans ouvrir le détail. Elle ne s'affiche jamais à côté d'une heure provider.
- **Toute action du menu `⋮` porte une icône ET un libellé texte, jamais l'icône seule.** Même
  règle pour toute action ajoutée plus tard à ce menu — une icône seule suppose une culture visuelle
  qu'on ne peut pas garantir chez tous les administrateurs.
- **Label "RÉSULTAT" retiré** : le badge (Confirmé/Provisoire/…) porte déjà l'information ; le
  libellé de section est redondant à la densité carte.
- **`Attendu`/`Manquant` remplacés sur la carte** par un message orienté action : « Résultat à
  saisir » si `canEnterManualResult` est vrai, sinon « Résultat manquant ». Les labels de statut
  bruts (`consoleDrawResultStatusLabel`) restent inchangés ailleurs (tableau desktop, page détail) —
  seule la carte mobile reformule.
- **État de vente fermé : pas de bloc résultat vide.** Si le tirage est fermé et n'a ni résultat ni
  hint, la carte n'affiche que le badge d'état — pas de tiret `—` façon tableau.
- **Actions secondaires dans un menu `mat-menu`**, ouvert par un bouton icône `more_vert`. Seule
  l'action principale (déjà calculée par `primaryAction()`) reste en bouton plein, hors menu.
- **Uniformité** : toutes les cartes (groupées par date) suivent la même structure, sans variation
  conditionnelle d'ordre entre elles.

## What Changes

- `console-draws-table.component.html` : rendu carte (`__cards`) uniquement — le rendu tableau
  (`__scroller`) n'est pas touché.
- `console-draw-slot-identity.component.ts`/`.html` : `providerLabel()` (provider + heure provider)
  ne s'affiche plus sur la carte. `localLabel()` (heure locale/tenant) reste affiché, déplacé sous
  le badge d'état plutôt que dans l'en-tête d'identité — à trancher en implémentation entre
  `density() === 'compact'`/nouvel input dédié pour piloter `providerLabel()`, et où exactement
  `localLabel()` se rend une fois sorti de l'identité. La page de détail continue d'afficher les
  deux.
- Nouveau champ sur `ConsoleDrawRow` (ou calcul dans `generated-draws-table.component.ts`) pour le
  message de résultat manquant orienté action, distinct du label de statut brut.
- Menu Material (`mat-menu`) pour les actions de cycle de vie sur la carte, chaque item avec icône +
  libellé texte ; le tableau desktop garde son rendu actuel en icon-buttons (avec `title`, pas assez
  de bruit visuel à cette densité pour justifier un menu — et la souris permet le survol, absent au
  tactile).
- **Correction du sélecteur CSS trop large** : `console-draws-table.component.scss`, bloc
  `@include ui.down(expanded)` — `.mat-mdc-button-base { width: 100%; }` sous `&__card-actions`
  scope désormais uniquement le bouton principal (ex. via une classe dédiée), pas tous les
  boutons Material du conteneur. Avec le passage des actions de cycle de vie au menu `⋮`, ce
  conteneur n'aura de toute façon plus qu'un seul bouton — mais la règle doit rester correcte si un
  futur bouton s'y ajoute.
- SCSS de `console-draws-table.component.scss` (bloc `@include ui.down(expanded)`) : suppression du
  bloc résultat vide, ajustement de densité pour gagner de la place verticale.

## Impact

- `libs/web/console` (composant partagé) et `apps/admin-portal/.../generated-draws-table` (seul
  consommateur actuel de `ConsoleDrawsTableComponent` en mode carte avec actions de cycle de vie).
  Un `grep` sur `ConsoleDrawsTableComponent` confirmera si d'autres consommateurs existent avant
  l'implémentation — si oui, leur rendu carte change aussi (même contrat, pas de branche
  spécifique prévue).
- Aucun changement de contrat backend, de route, ni du modèle `GeneratedDrawView`.
- Le rendu tableau desktop (`≥840px`) est explicitement hors périmètre.

## Non-goals

- Pas de refonte du tableau desktop.
- Pas de changement des règles métier qui déterminent `primaryAction()` ou `lifecycleActions()` —
  seule leur présentation change.
- Pas de nouveau composant de carte générique réutilisable au-delà des tirages — si le besoin
  apparaît pour tickets/vendeurs, ce sera un change séparé.
- Pas de retrait des informations techniques des exports/rapports — uniquement la carte mobile.
