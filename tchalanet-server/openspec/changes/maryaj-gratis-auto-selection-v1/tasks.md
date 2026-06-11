# Tasks — maryaj-gratis-auto-selection-v1

Prérequis : close-promotion-v1 §7 (activation policy), §9 (cache runtime),
§11 (champs TicketLine). Compléter là-bas, ne pas dupliquer ici.

## 1. Documentation (sales, promotion, onboarding, guide admin)

- [x] `core/sales/DOMAIN_SALES.md` : documenter Prepare/Regenerate/Confirm, SalePreparation (TTL, statuts, idempotence), SelectionGenerationService, ligne promotionnelle snapshotée.
- [x] `core/promotion/promotion_design.md` : documenter selectionMode / generationStrategy / regenerableBeforeConfirm / maxRegenerationsBeforeConfirm et le template `DEFAULT_MARYAJ_GRATIS`.
- [x] Onboarding : `platform/tenant/PLATFORM_TENANTCONFIG.md` — instanciation template -> campagne tenant, pas de backfill automatique, hook onboarding en follow-up.
- [x] Guide admin `tchalanet-docs/docs/02-functional/guides/operator-admin-guide.md` : section Maryaj gratuit — comment **désactiver** la campagne pour un tenant qui n'en veut pas (pause/désactivation, effet immédiat sur les ventes suivantes) et comment **modifier les valeurs** (payoutBaseAmount en valeur fixe V1, quantity, éligibilité, max régénérations).
- [x] Noter dans le guide et la spec : mode multiplicateur du montant = évolution future, non supporté V1 (voir open question proposal).

## 2. Design note CQRS SalePreparation

- [x] Décision : `PrepareSaleCommand` au lieu d'une Preview Query stateful (voir `design.md`).
- [x] Valider la design note contre `openspec/context/10-non-negotiables.md` (Command Bus, transactions, RLS).

## 3. Promotion model

- [x] Ajouter sur l'effet : `selectionMode` (MANUAL | AUTO_GENERATED).
- [x] Ajouter `generationStrategy` (RANDOM | LOW_EXPOSURE_RANDOM).
- [x] Ajouter `regenerableBeforeConfirm` (boolean).
- [x] Ajouter `maxRegenerationsBeforeConfirm` (défaut 3).
- [x] Migration `V*` colonnes `promotion_rule_effect` — **demander avant création (pré-go-live)**.
- [x] Validation effet : AUTO_GENERATED exige une stratégie supportée ; LOW_EXPOSURE_RANDOM refusé partout en V1 (validation effet, activation, génération, régénération).
- [x] Exposer les nouveaux champs dans `PromotionEffectConfigView`.

## 4. Maryaj default template

- [x] Seed template `DEFAULT_MARYAJ_GRATIS` (effet FREE_GAME_LINE, gameCode HT_MARYAJ_GRATUIT, quantity 1, payoutBaseAmount 50 HTG à confirmer, AUTO_GENERATED, RANDOM, regenerable, max 3 ; éligibilité minPaidTotal > 0 + 1 ligne payante).
- [x] Commande admin interne d'instanciation pour un tenant (template -> campagne tenant ACTIVE).
- [ ] Follow-up (hors V1, noter seulement) : hook onboarding nouveau tenant ; backfill ops explicite avec dry-run pour tenants existants.

## 5. SelectionGenerationService

- [x] `SelectionGenerationService` dans `core.sales` (port + service).
- [x] `RandomSelectionGenerator` — règles jeu depuis catalog/runtime game config (nb numéros, range, doublons, format).
- [x] Enum stratégie avec `LOW_EXPOSURE_RANDOM` -> UNSUPPORTED.
- [x] `purpose` : PROMOTION_FREE_LINE | CASHIER_SUGGESTION.
- [x] Vérifier que les règles de génération de `HT_MARYAJ_GRATUIT` sont disponibles au runtime ; sinon, compléter catalog (lecture des seeds V204/V208).
- [x] Tests unitaires : sélections valides, respect range/doublons, gameCode inconnu.
- [x] Bonus : `PromotionSelectionResolver` branché sur le service — remplace le générateur hash qui produisait une sélection invalide (un seul 2D) pour MARRIAGE_2D2D (bug latent du flux FREE_GAME_LINE existant).

## 6. SalePreparation core

- [x] Entité + table `sale_preparation` (+ `sale_preparation_promotion_line`) — migration V223, RLS + index + grants.
- [x] Statuts DRAFT / CONFIRMED / EXPIRED / CANCELLED + transitions (`SalePreparationStateMachine`).
- [x] TTL 10 minutes (`expires_at`) + job périodique (`SalePreparationRetentionScheduler`, ShedLock, per-tenant RLS binding). Check paresseux au confirm/regenerate -> slices 8-9.
- [x] Rétention : purge EXPIRED/CANCELLED à 7 j ; CONFIRMED à 30 j (lien réconciliation = simple borne 30 j en V1, voir design).
- [x] Index `(tenant_id, status, expires_at)` + unique partiel `(tenant_id, idempotency_key)`.
- [ ] `input_hash`/`cart_hash` calculé serveur au prepare (dérive panier + déduplication).
- [ ] `idempotency_key` + `ticket_id` stockés au confirm.
- [x] Lignes promo générées avec `line_ref` + `regeneration_count` (+ regenerable/max_regenerations snapshotés sur la ligne).
- [x] Tests : transitions invalides (state machine 3/3). Expiration/purge couverts par contraintes SQL + queries dédiées ; e2e en slice 12.

## 7. PrepareSaleCommand

- [x] `PrepareSaleCommand` + handler : réutilise `SalePreparationOrchestrator` (mode FINAL) -> persist préparation -> `SalePreparationView`.
- [x] Ordre de préparation conforme à close-promotion-v1 §11 (porté par l'orchestrateur existant).
- [x] Endpoint cashier `POST /tenant/sales/preparations` (terminal proof gate, retourne preparationId + lignes finales).
- [x] Tenant sans campagne ACTIVE -> aucune ligne gratuite (décision null => zéro ligne promo), flux inchangé.
- [ ] Tests intégration (DB/e2e — slice 12).

## 8. RegeneratePromotionLineCommand

- [x] `RegenerateSalePreparationPromotionLineCommand` + handler.
- [x] Endpoint dédié `POST /tenant/sales/preparations/{preparationId}/promotion-lines/{lineRef}/regenerate`.
- [x] Garde-fous : DRAFT non expirée (expiration paresseuse), ligne PROMOTION uniquement, regenerable=true, count < max.
- [x] Audit actor/session/terminal par régénération (log structuré).
- [x] Remplacement de la sélection (pas d'historique).
- [x] Tests : max dépassé -> refus, après confirm -> refus, non-regenerable -> refus, expirée -> refus + EXPIRED (5/5).

## 9. ConfirmPreparedSaleCommand

- [x] `ConfirmPreparedSaleCommand` + handler : payload = preparationId + idempotencyKey uniquement ; rejoue le pipeline sell avec sélections épinglées (voir design §5) ; marque CONFIRMED + ticketId.
- [x] Aucune régénération au confirm ; sélections épinglées depuis la préparation (décision design §5 : pipeline sell rejoué pour la sécurité money/limits).
- [x] Validation anti-forgerie : le client n'envoie jamais de lignes au confirm ; pipeline sell + applier valident sélection/montants côté serveur.
- [x] Double confirm même idempotencyKey -> même ticketId (handler) + @RequireIdempotency au web layer.
- [x] Tests handler 5/5 (épinglage sélections, replay idempotent, clé différente -> conflit, expirée -> EXPIRED, vente rejetée -> DRAFT conservé). Tests intégration DB/e2e -> slice 12.

## 10. Receipt / events / snapshots

- [x] TicketLine : `selectionSource` existe déjà (origin/pricingSource/promotionDecisionId aussi — close-promotion-v1 §11 livré en réalité, son tasks.md est en retard).
- [x] Reçu : déjà implémenté — `TicketReceiptGameLinesFormatter` affiche la ligne + marqueur `Promotion: <label>` (i18n `PROMOTION_FREE_GAME_LINE`, seed V210) ; montants stake/payout par colonne (payout = payoutBaseAmount × odds).
- [x] Events ticket : déjà exposés — `TicketLinePlacedItem` porte origin/selectionSource/payoutBaseAmount et `TicketPlacedEvent` embarque la `PromotionDecision`.
- [x] Vérifié : `TicketWinningCalculator` paie `potentialPayoutAmount` snapshoté ; aucun appel `EvaluatePromotionQuery` dans settlement/payout.

## 11. Mobile POS (change compagnon)

- [x] Créer le change compagnon : `tchalanet-mobile/openspec/changes/maryaj-gratis-pos-v1/` (proposal + tasks, contrat API figé inclus). Implémentation mobile = sessions /mobile-task dédiées.

## 12. E2E

- [ ] Tenant avec Maryaj actif -> preview contient la ligne gratuite.
- [ ] Regenerate change les numéros et incrémente le compteur.
- [ ] Confirm persiste les numéros du dernier preview.
- [ ] Reçu contient Maryaj gratuit.
- [ ] Tenant sans promo -> pas de ligne gratuite.
- [ ] Max regenerate dépassé -> refus.
- [ ] Confirm expiré -> refus.
- [ ] Double confirm même idempotencyKey -> même ticket retourné.
