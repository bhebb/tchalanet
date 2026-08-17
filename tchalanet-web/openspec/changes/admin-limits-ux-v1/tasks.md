# Tasks — admin-limits-ux-v1

Checkpoint obligatoire : lire ce fichier en début de session et cocher en temps réel (`[ ]` → `[x]`).

Prérequis : branche `spec/limits-contextual-config-v1` mergée dans `main`.

---

## 0. Pre-work — Tracer le write path draw_exposure

Ces tâches sont des pré-requis avant de déclarer l'exposition protection hardened.

- [ ] Trouver le handler/service qui met à jour `draw_exposure` après confirmation d'un ticket
  (chercher `DrawExposureJpaEntity`, `ApplyTicketExposure`, `LimitPolicyEventsListener`).
- [ ] Confirmer que `stake_total` et `sales_count` sont bien incrémentés après commit.
- [ ] Confirmer le comportement idempotent / replay (que se passe-t-il si l'événement est rejoué ?).
- [ ] Confirmer le comportement en cas d'annulation / voiding d'un ticket.
- [ ] Confirmer que les dimensions scope dans le write path correspondent à celles du read path
  (`scope_type`, `scope_id`, `bet_type`, `selection_key` — même combinaison que `findFactsForBetTypes`).
- [ ] Ajouter ou corriger des tests si des écarts sont trouvés.
- [ ] Documenter le résultat dans ce fichier (section Results ci-dessous).

### Results — write path (à remplir)

```
Entrypoint : …
After-commit : …
Idempotency : …
Voiding : …
Scope match : …
```

---

## 1. Backend — Nettoyage du catalog

### 1.1 Suppression de MAX_SALES_COUNT_PER_TICKET

- [ ] Vérifier qu'aucune valeur persistée n'existe en DB staging/prod pour `MAX_SALES_COUNT_PER_TICKET`.
- [ ] Supprimer `MAX_SALES_COUNT_PER_TICKET` de `RuleKey.java`.
- [ ] Vérifier qu'aucun test ne référence ce RuleKey directement.

### 1.2 Retrait des règles du catalog admin

Pour chacune des règles suivantes, retirer du catalog d'admin (`rules.v1.json`) et de l'UX seulement.
Ne pas supprimer les évaluateurs Java existants.

- [ ] `MAX_STAKE_PER_TICKET` — retirer du catalog + admin UX ; documenter comportement assignments persistés.
- [ ] `MAX_LINES_PER_TICKET` — retirer du catalog + admin UX ; documenter comportement assignments persistés.
- [ ] `BLOCK_BET_TYPE` — retirer du catalog + admin UX ; documenter comportement assignments persistés.
- [ ] `MAX_SALES_COUNT_PER_SELECTION_PER_DRAW` — retirer du catalog + admin UX.
- [ ] Décider (avec product) si `MAX_STAKE_PER_LINE` reste exposé dans l'admin UX.
  Si non, retirer du catalog ; si oui, conserver avec wording métier.
- [ ] Vérifier que les assignments persistés pour ces règles ne cassent pas le moteur d'évaluation
  (le moteur doit ignorer gracieusement les assignments avec un RuleKey sans évaluateur).
- [ ] Documenter les règles placeholders intentionnellement non implémentées
  (`MAX_TICKET_COUNT_PER_AGENT_PER_WINDOW`, `MAX_STAKE_PER_AGENT_PER_DRAW`, `MAX_STAKE_PER_OUTLET_PER_DRAW`).

### 1.3 Effective limits read model

- [ ] Vérifier si l'API actuelle expose : règle gagnante, scope gagnant, display name du scope,
  hérité vs local, valeur configurée, outcome.
- [ ] Si manquant, ajouter uniquement les champs read-model/BFF nécessaires aux nouvelles vues.
- [ ] La résolution reste backend-owned. Ne pas reproduire `ScopeScoreTable` en frontend.

---

## 2. Central Limits Page (`/limits`)

### 2.1 Suppression du panneau summary

- [ ] Supprimer le panneau navy summary desktop.
- [ ] Supprimer les KPI cards mobile.
- [ ] Remplacer par des stats inline compactes près du titre de page.
  Exemple : `3 limit aktif · 1 blokaj nimewo · 2 plafon`
- [ ] Garder les warnings exceptionnels visibles uniquement quand action requise.

### 2.2 Quick actions — réduction

- [ ] Réduire à deux quick actions : `Bloke nimewo` + `Plafon pa nimewo`.
- [ ] Supprimer les quick actions pour les familles de règles retirées du produit.
- [ ] Les deux actions ont le même niveau hiérarchique visuel.
- [ ] `Bloke nimewo` → crée/configure `BLOCK_SELECTION_PER_DRAW`.
- [ ] `Plafon pa nimewo` → crée/configure `MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW`.
- [ ] Wording `Plafon pa nimewo` : `Limite kantite total ki ka vann sou yon nimewo pou yon tiraj.`

### 2.3 Liste des assignments configurés

- [ ] Remplacer l'accordéon catalog actuel par une liste d'assignments uniquement.
- [ ] Ne pas afficher les définitions de règles non configurées avec `Pa gen règ`.
- [ ] Colonnes desktop : `RÈG | APLIKE SOU | VALÈ | SOUS / PORTÉE | ETA | AKSYON`.
- [ ] Actions par row : `[Modifye] [Dezaktive] […]` (delete sous `…`).
- [ ] Filtres : `Tout | Santral | Kanal tiraj | Machann / Seller terminal` (+ Agent si pertinent produit).
- [ ] Recherche sur nom de canal / terminal quand utile.
- [ ] La liste est le point central d'ajout/édition/suppression de limits.

### 2.4 Mobile — compact cards

- [ ] Une LimitAssignment configurée = une compact card.
- [ ] Pas de scroll horizontal sur table.
- [ ] Actions destructives sous `…`.
- [ ] Action primaire `Modifye` visible directement.
- [ ] Pas de cards imbriquées.
- [ ] Pattern identique au pattern Draw Channels mobile.

---

## 3. Generic Limit Detail (`/limits/:assignmentId`)

- [ ] Créer la page/route de détail pour une LimitAssignment.
- [ ] Contenu : display name de la règle, valeur, statut, outcome, scope type, display name du scope,
  période de validité si applicable, métadonnées origine.
- [ ] Actions : `[Modifye] [Dezaktive] […]`.
- [ ] Ne pas exposer `RuleKey` interne ni `ScopeScore` sauf sous debug/support details.
- [ ] La liste centrale affiche un aperçu compact ; le détail affiche l'explication complète.

---

## 4. Contextuel — Tenant

- [ ] Remplacer le bloc catalog complet dans les settings tenant par une section scope-spécifique.
- [ ] Afficher uniquement les assignments configurés au scope `TENANT`.
- [ ] `Ajoute yon limit` → central creation, scope TENANT préselectionné.
- [ ] `Gade tout limit yo →` → central management filtré sur TENANT.
- [ ] Optionnel : explication hérité/effectif uniquement là où c'est utile.

---

## 5. Contextuel — Draw Channel

- [ ] Afficher les assignments `DRAW_CHANNEL` locaux.
- [ ] Afficher les limites héritées/effectives séparément avec provenance explicite
  (`Soti nan Santral` / `Override sou kanal sa a`).
- [ ] Ne pas exposer les scores resolver.
- [ ] `Ajoute yon limit` → central creation, scope=DRAW_CHANNEL, channel préselectionné.
- [ ] `Jere limit kanal la →` → central management filtré par ce canal.

---

## 6. Contextuel — Seller Terminal

- [ ] Afficher les assignments locaux du terminal séparément des limites héritées/effectives.
- [ ] `Ajoute yon limit` → central creation, scope=SELLER_TERMINAL, terminal préselectionné.
- [ ] `Jere limit machin sa a →` → central management filtré par ce terminal.
- [ ] Ne pas dupliquer le catalog/editor complet inline.

---

## 7. Draw Detail — Effective Limits Only

- [ ] Remplacer le catalog générique éditable par une vue effective-only.
- [ ] Afficher uniquement les limites effectives qui s'appliquent au tirage courant.
- [ ] Ne pas afficher les définitions de règles non configurées.
- [ ] Afficher la provenance/source pour chaque limite.
- [ ] Conserver la quick action `Bloke nimewo`.
- [ ] Conserver le lien vers la gestion du Draw Channel propriétaire.
- [ ] Le Draw n'est pas un nouveau scope persistent.

---

## 8. Navigation Contextuelle / Préselection

- [ ] Depuis Draw → `Bloke nimewo` : préselectionner
  `rule=BLOCK_SELECTION_PER_DRAW, scope=DRAW_CHANNEL, scopeId=current draw channel`.
- [ ] Depuis Draw Channel → `Ajoute yon limit` : préselectionner
  `scope=DRAW_CHANNEL, scopeId=current channel`.
- [ ] Depuis Seller Terminal → `Ajoute yon limit` : préselectionner
  `scope=SELLER_TERMINAL, scopeId=current terminal`.
- [ ] Depuis Tenant → `Ajoute yon limit` : préselectionner `scope=TENANT`.
- [ ] Préserver la navigation de retour contextuelle là où utile.
- [ ] L'éditeur central est l'unique implémentation du formulaire.

---

## 9. Suppression du bloc catalog générique sur les pages contextuelles

- [ ] Supprimer de tenant settings le bloc `Règ pou tikè / Blokaj / Risk sou nimewo` avec counts.
- [ ] Supprimer le même bloc de seller terminal si présent.
- [ ] Supprimer de draw detail si présent.
- [ ] Supprimer de draw channel si présent.
- [ ] Les remplacer par : `Configured here | Inherited/effective` + actions `Add / Manage`.

---

## 10. Tests

### Central page

- [ ] Assignment tenant affiché.
- [ ] Assignment draw-channel affiché.
- [ ] Assignment terminal affiché.
- [ ] Filtrage par scope.
- [ ] Edit.
- [ ] Disable.
- [ ] Delete.
- [ ] Empty state.
- [ ] Error state.

### Navigation contextuelle

- [ ] Draw → Block number avec channel préselectionné.
- [ ] Draw → Gestion channel limits.
- [ ] Draw Channel → Ajout limit avec scope préselectionné.
- [ ] Seller Terminal → Ajout limit avec terminal préselectionné.
- [ ] Tenant → Ajout limit tenant.
- [ ] Comportement return context.

### Vues effectives

- [ ] Règle locale.
- [ ] Règle tenant héritée.
- [ ] Override plus spécifique.
- [ ] Plusieurs RuleKeys depuis scopes différents.
- [ ] Labels de provenance corrects.

### Responsive

- [ ] 360 px page centrale.
- [ ] 360 px limites tenant.
- [ ] 360 px limites draw channel.
- [ ] 360 px limites seller terminal.
- [ ] 360 px limites effectives draw detail.
- [ ] Screenshots desktop pour toutes les surfaces concernées.

---

## 11. Gates Before Done

- [ ] `pnpm nx lint admin-portal` — clean.
- [ ] `pnpm nx test admin-portal` — green.
- [ ] TypeScript / build — clean.
- [ ] Strict i18n audit — uniquement des clés `admin.limits.*` ajoutées, pas de clés brutes.
- [ ] Pas de wording mixte français/créole introduit.

---

## 12. Definition Of Done

- [ ] `Kontwòl lavant` est l'unique surface globale de gestion des limits configurées.
- [ ] Tenant / Draw Channel / Seller Terminal affichent limites locales + héritées/effectives contextuelles.
- [ ] Draw affiche limites effectives seulement + raccourcis opérationnels.
- [ ] Navigation contextuelle préselectionne le scope approprié.
- [ ] Aucune page hors central management ne duplique le catalog/editor complet.
- [ ] Règles retirées supprimées de l'UX/catalog admin, gérées proprement côté persistence.
- [ ] Limite exposition reste cumulative et correctement décrite (`Plafon pa nimewo`).
- [ ] Blocs summary desktop/mobile supprimés au profit de stats inline compactes.
- [ ] Présentation mobile des limits configurées en compact cards/lists.
- [ ] Backend reste source de vérité pour la résolution effective des règles.
- [ ] Write path `draw_exposure` tracé et vérifié (section 0 complète).
