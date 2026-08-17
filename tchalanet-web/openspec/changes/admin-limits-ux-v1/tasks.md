# Tasks — admin-limits-ux-v1

Checkpoint obligatoire : lire ce fichier en début de session et cocher en temps réel (`[ ]` → `[x]`).

Prérequis : branche `spec/limits-contextual-config-v1` mergée dans `main`.

---

## 0. Pre-work — Tracer le write path draw_exposure

Ces tâches sont des pré-requis avant de déclarer l'exposition protection hardened.

- [x] Trouver le handler/service qui met à jour `draw_exposure` après confirmation d'un ticket
  (chercher `DrawExposureJpaEntity`, `ApplyTicketExposure`, `LimitPolicyEventsListener`).
- [x] Confirmer que `stake_total` et `sales_count` sont bien incrémentés après commit.
- [x] Confirmer le comportement idempotent / replay (que se passe-t-il si l'événement est rejoué ?).
- [x] Confirmer le comportement en cas d'annulation / voiding d'un ticket.
- [x] Confirmer que les dimensions scope dans le write path correspondent à celles du read path
  (`scope_type`, `scope_id`, `bet_type`, `selection_key` — même combinaison que `findFactsForBetTypes`).
- [x] Ajouter ou corriger des tests si des écarts sont trouvés. (aucun écart — comportement voiding intentionnel)
- [x] Documenter le résultat dans ce fichier (section Results ci-dessous).

### Results — write path

```
Entrypoint : LimitPolicyEventsListener.on(TicketPlacedEvent) — @TransactionalEventListener(phase=AFTER_COMMIT)

After-commit : ApplyTicketExposureCommandHandler (@TchTx REQUIRES_NEW) →
               ExposureProjectorAdapter.applyTicketSold() →
               increment_draw_exposure SQL function (V104).
               Scopes écrits : toujours TENANT ;
                               DRAW_CHANNEL si drawChannelId présent ;
                               SELLER_TERMINAL si sellerTerminalId présent.
               Une ligne SQL par (scope × ticketLine).
               increment_draw_exposure : UPSERT sur (tenant_id, draw_id, scope_type, scope_id,
               bet_type, selection_key) WHERE deleted_at IS NULL →
               stake_total += p_stake, sales_count += 1.

Idempotency : processedEvent.markProcessedIfAbsent("limitpolicy.exposure", eventId).
              Si l'eventId est déjà enregistré, le handler retourne sans écrire.

Voiding : Pas de handler TicketCancelledEvent dans LimitPolicyEventsListener.
          Pas de decrement_draw_exposure SQL.
          → draw_exposure n'est jamais décrémenté en cas d'annulation.
          Comportement intentionnel : plafon cumulatif worst-case (guard conservateur).
          Accepté en prod V1 ; documenter dans la spec UX ("Plafon pa nimewo" = cumul
          non-réversible sur le tiraj).

Scope match : Read path ExposureFactsReaderAdapter.snapshot() →
              findFactsForBetTypes(drawId, scopeType, scopeId, betTypes)
              + clé LimitFactsSnapshot.Key(scope, betType, selectionKey).
              Dimensions identiques au write path → ✓ aligné.
```

---

## 1. Backend — Nettoyage du catalog

### 1.1 Suppression de MAX_SALES_COUNT_PER_TICKET

- [x] Vérifier qu'aucune valeur persistée n'existe en DB staging/prod pour `MAX_SALES_COUNT_PER_TICKET`.
- [x] Supprimer `MAX_SALES_COUNT_PER_TICKET` de `RuleKey.java`. (fait dans PR #667)
- [x] Vérifier qu'aucun test ne référence ce RuleKey directement.

### 1.2 Retrait des règles du catalog admin

Pour chacune des règles suivantes, retirer du catalog d'admin (`rules.v1.json`) et de l'UX seulement.
Ne pas supprimer les évaluateurs Java existants.

- [x] `MAX_STAKE_PER_TICKET` — retiré du catalog + admin UX. (PR #667)
- [x] `MAX_LINES_PER_TICKET` — retiré du catalog + admin UX. (PR #667)
- [x] `BLOCK_BET_TYPE` — retiré du catalog + admin UX. (PR #667)
- [x] `MAX_SALES_COUNT_PER_SELECTION_PER_DRAW` — retiré du catalog + admin UX. (PR #667)
- [x] `MAX_STAKE_PER_LINE` — retiré du catalog. (PR #667)
- [x] Vérifier que les assignments persistés ne cassent pas le moteur — le moteur ignore les RuleKey inconnus. (évaluateurs conservés)
- [x] `rules.v1.json` contient uniquement les 2 règles produit avec labels créole corrects.

### 1.3 Effective limits read model

- [x] API actuelle (`/admin/policies/overview`) expose `activeLimits[]` avec : ruleKey, targetType, targetId, targetLabel, params, enabled, onBreach — suffisant pour les nouvelles vues.
- [x] Hérité vs local : inféré côté frontend depuis le scope match. Backend reste source de vérité pour la résolution.

---

## 2. Central Limits Page (`/limits`)

### 2.1 Suppression du panneau summary

- [x] Supprimer le panneau navy summary desktop. (aside `tch-card` supprimé)
- [x] Supprimer les KPI cards mobile. (même composant)
- [x] Remplacer par des stats inline compactes près du titre de page.
  → `{{ activeCount }} limit aktif · {{ blockCount }} blokaj nimewo · {{ capCount }} plafon`
- [x] Warnings exceptionnels : `actionError` / `actionNotice` visibles seulement quand présents.

### 2.2 Quick actions — réduction

- [x] Réduire à deux quick actions : `Bloke nimewo` + `Plafon pa nimewo`.
- [x] Quick actions ticket limit et seller limit supprimées.
- [x] Les deux actions ont le même niveau hiérarchique (`mat-flat-button` + `mat-stroked-button`).
- [x] `Bloke nimewo` → ouvre `BlockNumberQuickDialogComponent` (existant).
- [x] `Plafon pa nimewo` → ouvre `UpsertLimitDialogComponent.init(spec, 'TENANT', null, null)`.

### 2.3 Liste des assignments configurés

- [x] Remplacer l'accordéon groupé par une liste flat d'assignments uniquement.
- [x] Seuls les assignments configurés sont affichés (pas de règles vides).
- [x] Colonnes desktop : `RÈG | APLIKE SOU | VALÈ | PORTÉE | ETA | AKSYON`.
- [x] Actions par row : `[Modifye] [Dezaktive] […]` (delete sous `…`).
- [x] Filtres : `Tout | Santral | Kanal tiraj | Machann` (toggle group).
- [x] `Modifye` ouvre `UpsertLimitDialogComponent.init(spec, targetType, targetId, assignment)`.

### 2.4 Mobile — compact cards

- [x] Une LimitAssignment configurée = une compact card (grid single-col mobile).
- [x] Pas de scroll horizontal (overflow-x: auto sur les filtres).
- [x] Actions destructives sous `…` (MatMenu).
- [x] Action primaire `Modifye` visible directement.
- [x] Pas de cards imbriquées.

---

## 3. Generic Limit Detail (`/limits/:assignmentId`)

- [x] Créer la page/route de détail pour une LimitAssignment.
  Route `:assignmentId` ajoutée dans `admin-limits.routes.ts`.
  Données : `forkJoin(overview + listRules)` → find by assignmentId.
- [x] Contenu : display name de la règle, description, valeur, outcome, scope type, display name du scope,
  période de validité, métadonnées dans section debug rétractable.
- [x] Actions : `[Modifye] [Dezaktive] […]` avec delete sous menu.
  Après delete → redirection vers `/app/admin/limits`.
- [x] `RuleKey`, `assignmentId`, `targetType` sous `<details>` "Detay teknik" (non exposé par défaut).
- [x] Rule name dans la liste centrale = lien `[routerLink]` vers la page détail.
- [x] La liste centrale affiche un aperçu compact ; le détail affiche explication complète + section scope.

---

## 4. Contextuel — Tenant

- [x] Remplacer le bloc catalog complet dans les settings tenant par une section scope-spécifique.
  `AdminLimitsSectionComponent` réécrit — plus de `LimitPolicyBlockComponent`.
- [x] Afficher uniquement les assignments `TENANT` locaux (activés + effectifs).
- [x] `Ajoute yon limit` → ouvre `UpsertLimitDialogComponent.initAdd(specs, 'TENANT', null)`.
- [x] `Jere limit yo →` → lien vers `/app/admin/limits` (gestion centrale).

---

## 5. Contextuel — Draw Channel

- [x] Afficher les assignments `DRAW_CHANNEL` locaux dans `.ls__group` "Limit sou nivo sa a".
- [x] Afficher les limites héritées dans `.ls__group` "Limit eritye" avec provenance
  `Soti nan {{inheritedScopeLabel}}`.
- [x] Scores resolver non exposés.
- [x] `Ajoute yon limit` → ouvre dialog avec `targetType='DRAW_CHANNEL'`, `targetId=channelId`.
- [x] `Jere limit yo →` → lien vers `/app/admin/limits`.

---

## 6. Contextuel — Seller Terminal

- [x] Afficher les assignments `SELLER_TERMINAL` locaux séparément des limites héritées.
- [x] `Ajoute yon limit` → ouvre dialog avec `targetType='SELLER_TERMINAL'`, `targetId=terminalId`.
- [x] `Jere limit yo →` → lien vers `/app/admin/limits`.
- [x] Catalog/editor complet non dupliqué inline.

---

## 7. Draw Detail — Effective Limits Only

- [x] Remplacer le catalog générique éditable par une vue effective-only.
  `tch-admin-limits-section` avec `targetType="DRAW_CHANNEL"` + `effectiveAt=scheduledAt`.
- [x] Afficher uniquement les limites effectives qui s'appliquent au tirage courant.
  `isEffective()` filtre par `effectiveAt` dans le composant.
- [x] Ne pas afficher les définitions de règles non configurées.
  Nouveau composant n'affiche que des assignments réels.
- [x] Afficher la provenance/source pour chaque limite.
  Section "Limit eritye" avec label `Soti nan {{inheritedScopeLabel}}`.
- [x] Conserver la quick action `Bloke nimewo`.
  `detailActions` conservé ; `openBlockNumber()` passe `channelId`.
- [x] Conserver le lien vers la gestion du Draw Channel propriétaire.
  Lien `[routerLink]="['/app/admin/draw-channels', draw()!.drawChannelId]"` ajouté.
- [x] Le Draw n'est pas un nouveau scope persistent.
  Scope utilisé = `DRAW_CHANNEL` ; aucun scope `DRAW` créé.

---

## 8. Navigation Contextuelle / Préselection

- [x] Depuis Draw → `Bloke nimewo` : préselectionner `scope=DRAW_CHANNEL, scopeId=current channel`.
  `BlockNumberQuickDialogComponent` reçoit `channelId` → pré-sélectionne DRAW_CHANNEL.
- [x] Depuis Draw Channel → `Ajoute yon limit` : préselectionner `scope=DRAW_CHANNEL, scopeId`.
  `AdminLimitsSectionComponent.addLimit()` passe `targetType` + `targetId` à `initAdd()`.
- [x] Depuis Seller Terminal → `Ajoute yon limit` : préselectionner `scope=SELLER_TERMINAL, scopeId`.
  Même pattern — `targetType='SELLER_TERMINAL'` et `targetId=terminalId`.
- [x] Depuis Tenant → `Ajoute yon limit` : préselectionner `scope=TENANT`.
  `targetType='TENANT'`, `targetId=null`.
- [x] L'éditeur central est l'unique implémentation du formulaire.
  `UpsertLimitDialogComponent` seul dialog de création/édition.

---

## 9. Suppression du bloc catalog générique sur les pages contextuelles

- [x] Supprimer de tenant settings le bloc `Règ pou tikè / Blokaj / Risk sou nimewo` avec counts.
  `LimitPolicyBlockComponent` supprimé ; `AdminLimitsSectionComponent` réécrit.
- [x] Supprimer le même bloc de seller terminal si présent.
- [x] Supprimer de draw channel si présent.
- [x] Les remplacer par : `Limit sou nivo sa a | Limit eritye` + actions `Add / Manage`.
- [ ] Vérifier draw detail (Section 7 couvre ce cas spécifiquement).

---

## 10. Tests

### Central page — `AdminLimitsOverviewPage`

- [x] Filtrage ALL retourne tous les assignments.
- [x] Filtrage TENANT retourne uniquement les assignments TENANT.
- [x] Filtrage DRAW_CHANNEL retourne uniquement les assignments DRAW_CHANNEL.
- [x] `activeCount` compte uniquement les assignments actifs (enabled).
- [x] `blockCount` / `capCount` comptent par groupe uniquement.
- [x] `inlineStats` retourne null si aucun actif.
- [x] `inlineStats` formate correctement avec blokaj + plafon.
- [x] `inlineStats` omet les sections à zéro.

### Limit Detail page — `AdminLimitsDetailPage`

- [x] `statusTone` : success pour enabled, warning pour disabled.
- [x] `statusLabel` : Aktif / Dezaktive.
- [x] `targetLabel` : "Platfòm" pour TENANT.
- [x] `targetLabel` : targetLabel > targetCode > targetId pour les autres scopes.
- [x] `validityLabel` : Pèmanan sans dates.
- [x] `validityLabel` : inclut la date de fin.
- [x] `outcomeLabel` : BLOCK → Bloke, WARN → Avètisman.
- [x] `ruleKeyCode` provient du spec chargé.

### Section component — `AdminLimitsSectionComponent` (existants)

- [x] Filtre local assignments (enabled + effectiveAt).
- [x] Exclut inherited hors plage effectiveAt.
- [x] Format params SELECTION.
- [x] Format params CENTS (HTG).

### Responsive

- [ ] Screenshots 360 px — à faire manuellement sur staging.

---

## 11. Gates Before Done

- [x] `pnpm nx lint admin-portal` — 0 erreurs (11 warnings existants dont 10 pré-existants).
- [x] `pnpm nx test admin-portal` — 177 tests verts (35 suites).
- [x] TypeScript / build — clean (`pnpm nx build admin-portal` 0 erreurs).
- [x] Strict i18n audit — uniquement des clés `admin.limits.*` ajoutées ; 103 nouvelles clés vérifiées.
- [x] Pas de wording mixte français/créole introduit — labels créole dans le template, clés i18n françaises dans le JSON source (convention existante).

---

## 12. Definition Of Done

- [x] `Kontwòl lavant` est l'unique surface globale de gestion des limits configurées.
  Page `/limits` réécrite — liste flat d'assignments, filtre par scope, détail par assignmentId.
- [x] Tenant / Draw Channel / Seller Terminal affichent limites locales + héritées/effectives contextuelles.
  `AdminLimitsSectionComponent` réécrit — local + inherited avec provenance.
- [x] Draw affiche limites effectives seulement + raccourcis opérationnels.
  `effectiveAt=scheduledAt` filtrage + "Bloke nimewo" + lien vers le draw channel.
- [x] Navigation contextuelle préselectionne le scope approprié.
  `initAdd(specs, targetType, targetId)` + `BlockNumberQuickDialogData.channelId`.
- [x] Aucune page hors central management ne duplique le catalog/editor complet.
  `LimitPolicyBlockComponent` entièrement remplacé.
- [x] Règles retirées supprimées de l'UX/catalog admin, gérées proprement côté persistence.
  PR #667 mergé ; évaluateurs conservés, catalog réduit à 2 règles.
- [x] Limite exposition reste cumulative et correctement décrite (`Plafon pa nimewo`).
  Write path confirmé (section 0) ; quick action "Plafon pa nimewo" sur la page centrale.
- [x] Blocs summary desktop/mobile supprimés au profit de stats inline compactes.
  Stats inline `{{ activeCount }} limit aktif · {{ blockCount }} blokaj · {{ capCount }} plafon`.
- [x] Présentation mobile des limits configurées en compact cards/lists.
  Grid single-col mobile + overflow-x auto sur filtres.
- [x] Backend reste source de vérité pour la résolution effective des règles.
  Frontend consomme `/admin/policies/overview` sans recalcul.
- [x] Write path `draw_exposure` tracé et vérifié (section 0 complète).
