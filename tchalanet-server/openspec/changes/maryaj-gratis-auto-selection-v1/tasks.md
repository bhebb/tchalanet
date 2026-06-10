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

- [ ] Entité + table `sale_preparation` (BaseTenantEntity / RLS) — migration à valider avant création.
- [ ] Statuts DRAFT / CONFIRMED / EXPIRED / CANCELLED + transitions.
- [ ] TTL 10 minutes (`expires_at`), expiration paresseuse au confirm/regenerate + job périodique d'expiration.
- [ ] Rétention : purge EXPIRED/CANCELLED après 7 jours ; CONFIRMED gardé 30 jours ou jusqu'à réconciliation, puis purge.
- [ ] Index `(tenant_id, status, expires_at)`.
- [ ] `input_hash`/`cart_hash` calculé serveur au prepare (dérive panier + déduplication).
- [ ] `idempotency_key` + `ticket_id` stockés au confirm.
- [ ] Lignes promo générées avec `line_ref` + `regeneration_count`.
- [ ] Tests : transitions invalides, expiration, purge.

## 7. PrepareSaleCommand

- [ ] `PrepareSaleCommand` + handler : paidLines -> `EvaluatePromotionQuery` -> FREE_GAME_LINE -> génération -> persist préparation -> `SalePreparationView`.
- [ ] Ordre de préparation conforme à close-promotion-v1 §11 (lines -> charges -> money -> promotion -> final money -> limits).
- [ ] Endpoint cashier (web adapter) retournant preparationId + lignes finales.
- [ ] Tenant sans campagne ACTIVE -> aucune ligne gratuite, flux inchangé.
- [ ] Tests intégration.

## 8. RegeneratePromotionLineCommand

- [ ] `RegenerateSalePreparationPromotionLineCommand` + handler.
- [ ] Endpoint dédié `POST .../preparations/{preparationId}/promotion-lines/{lineRef}/regenerate`.
- [ ] Garde-fous : DRAFT non expirée, ligne PROMOTION uniquement, regenerable=true, count < max.
- [ ] Audit actor/session/terminal par régénération.
- [ ] Remplacement de la sélection (pas d'historique).
- [ ] Tests : max dépassé -> refus, après confirm -> refus.

## 9. ConfirmPreparedSaleCommand

- [ ] `ConfirmPreparedSaleCommand` + handler : payload = preparationId + idempotencyKey uniquement (aucune ligne envoyée par le client) ; charge la préparation, vérifie contexte/expiration, persiste exactement les lignes préparées, snapshot promotion, marque CONFIRMED.
- [ ] Aucune réévaluation promotion au confirm (vérif sécurité minimale seulement).
- [ ] Validation anti-forgerie complète (voir design.md §7).
- [ ] Double confirm même idempotencyKey -> retourne le même ticket.
- [ ] Tests intégration.

## 10. Receipt / events / snapshots

- [ ] TicketLine : ajouter `selectionSource` (close-promotion-v1 §11 porte origin/pricingSource/payoutBaseAmount/promotionDecisionId).
- [ ] Reçu : ligne « Maryaj gratuit offert » + sélection + mise de base.
- [ ] Events ticket : exposer selectionSource + promotion snapshot.
- [ ] Vérifier settlement/payout : snapshots only (gardes close-promotion-v1 §12-13).

## 11. Mobile POS (change compagnon)

- [ ] Créer le change `tchalanet-mobile/openspec/changes/` une fois le contrat API figé (preview auto, bouton Régénérer si autorisé, confirm par preparationId, impression). Le POS n'applique pas la promo, il la voit.

## 12. E2E

- [ ] Tenant avec Maryaj actif -> preview contient la ligne gratuite.
- [ ] Regenerate change les numéros et incrémente le compteur.
- [ ] Confirm persiste les numéros du dernier preview.
- [ ] Reçu contient Maryaj gratuit.
- [ ] Tenant sans promo -> pas de ligne gratuite.
- [ ] Max regenerate dépassé -> refus.
- [ ] Confirm expiré -> refus.
- [ ] Double confirm même idempotencyKey -> même ticket retourné.
