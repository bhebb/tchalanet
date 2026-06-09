# Design — maryaj-gratis-auto-selection-v1

## 1. CQRS : pourquoi PrepareSaleCommand

Le preview actuel (`PreviewTicketSaleQuery` → `TicketSalePreviewResult`) est
une query stateless. Dès qu'on persiste des lignes générées (la sélection
Maryaj doit être identique entre preview et confirm), ce n'est plus une query.

```text
Preview stateless = lecture/calcul (reste une Query, inchangée)
Prepare           = écrit une préparation (Command)
```

Commands cibles :

```text
PrepareSaleCommand
  -> SalePreparationView (preparationId + lignes finales)

RegenerateSalePreparationPromotionLineCommand
  -> SalePreparationView mise à jour

ConfirmPreparedSaleCommand
  -> ticket persisté
```

## 2. SalePreparation

```text
sale_preparation
  id (preparationId)
  tenant_id            (RLS — BaseTenantEntity)
  seller / session / terminal context
  status               DRAFT | CONFIRMED | EXPIRED | CANCELLED
  input_hash / cart_hash (hash serveur des inputs payants au prepare —
                          détecte la dérive du panier client et déduplique)
  paid_lines           (snapshot des lignes payantes préparées)
  generated_promotion_lines
    line_ref
    game_code
    selection
    payout_base_amount
    promotion_decision_id
    regeneration_count
  idempotency_key
  expires_at           (now + 10 min)
  created_at / confirmed_at
```

Transitions :

```text
DRAFT -> CONFIRMED | EXPIRED | CANCELLED
CONFIRMED / EXPIRED / CANCELLED -> aucun
```

Rétention :

```text
TTL DRAFT            : 10 minutes
Expiration           : paresseuse au confirm/regenerate + job périodique
EXPIRED / CANCELLED  : purge après 7 jours
CONFIRMED            : gardé 30 jours ou jusqu'à réconciliation, puis purge
                       (le ticket persisté reste la vérité — la préparation
                       n'est qu'une trace de travail)
Index                : (tenant_id, status, expires_at)
```

## 3. Flux Prepare

```text
PrepareSaleCommand
  -> construit paidLines (pipeline sell existant)
  -> EvaluatePromotionQuery
  -> effet FREE_GAME_LINE + selectionMode = AUTO_GENERATED
  -> SelectionGenerationService (RANDOM, règles jeu catalog/runtime)
  -> persiste SalePreparation (DRAFT)
  -> retourne preparationId + lignes payantes + ligne Maryaj générée
```

## 4. Regenerate

Endpoint dédié :

```http
POST /tenant/cashier/sales/preparations/{preparationId}/promotion-lines/{lineRef}/regenerate
```

Règles imposées :

```text
- préparation en DRAFT, non expirée;
- ligne origin=PROMOTION uniquement;
- regenerableBeforeConfirm = true sur l'effet;
- regeneration_count < maxRegenerationsBeforeConfirm (3);
- audit actor/session/terminal à chaque régénération;
- la nouvelle sélection remplace l'ancienne (pas d'historique de lignes).
```

## 5. Confirm

Le confirm ne reçoit **jamais** les lignes du client :

```text
ConfirmPreparedSaleCommand
  payload = preparationId + idempotencyKey, rien d'autre
  - charger la préparation par preparationId
  - vérifier tenant/seller/session/terminal cohérents
  - vérifier non expirée
  - si déjà confirmée avec le même idempotencyKey -> retourner le même ticket
  - persister exactement les lignes préparées (source unique de vérité)
  - snapshot promotion sur TicketLine
  - publier events, générer reçu
  - marquer la préparation CONFIRMED + stocker idempotencyKey/ticketId
```

Pas de réévaluation de la promotion au confirm — vérification de sécurité
minimale seulement. La vérité des lignes vient de la préparation.

## 6. TicketLine promotionnelle (snapshot)

```text
gameCode            = HT_MARYAJ_GRATUIT
origin              = PROMOTION
pricingSource       = PROMOTION
selectionSource     = AUTO_GENERATED
stakeAmount         = 0
lineTotal           = 0
payoutBaseAmount    = montant de l'effet
promotionDecisionId = décision promotion
```

`origin`, `pricingSource`, `payoutBaseAmount`, `promotionDecisionId` sont
spécifiés dans close-promotion-v1 §11 ; ce change ajoute `selectionSource`.

## 7. Validation anti-forgerie

Le confirm ne recevant que `preparationId + idempotencyKey`, le client n'a
aucun canal pour injecter ou modifier une ligne au confirm — la surface
d'attaque se réduit au prepare et au regenerate. Contrôles serveur :

```text
- le client ne peut pas ajouter une ligne gratuite lui-même;
- quantity <= effet promotion;
- stakeAmount = 0;
- payoutBaseAmount vient de PromotionEffect;
- gameCode = effet promotion;
- sélection valide selon les règles du jeu;
- origin/pricingSource/selectionSource imposés serveur;
- promotionDecisionId connu;
- tenant/session/preparation cohérents;
- régénération uniquement avant confirmation.
```

## 8. SelectionGenerationService

Dans `core.sales` (jamais `core.promotion`).

```text
generate(gameCode, strategy, purpose)
  strategy : RANDOM (V1) | LOW_EXPOSURE_RANDOM (enum prête, UNSUPPORTED)
  purpose  : PROMOTION_FREE_LINE | CASHIER_SUGGESTION
  règles   : nb numéros, range, doublons, format — depuis catalog/runtime
```

`LOW_EXPOSURE_RANDOM` est refusé **partout** en V1 malgré l'enum :
validation d'effet, activation de campagne, génération, régénération.

Réutilisable plus tard pour le bouton vendeur « Générer des numéros ».

## 9. Validation contre les non-negotiables — 2026-06-09

Design validé contre `openspec/context/10-non-negotiables.md` et les règles
ArchUnit (`tchalanet-app/src/test/.../ArchitectureTest.java`) :

- **CQRS** : `PrepareSaleCommand` / `Regenerate…Command` / `Confirm…Command`
  sont des Commands (écriture) ; `EvaluatePromotionQuery` ne persiste rien ;
  `PreviewTicketSaleQuery` reste stateless. Conforme.
- **Couches** : `SalePreparation` et `SelectionGenerationService` vivent dans
  `core.sales` (invariants money/ticket) ; le template et l'instanciation dans
  `core.promotion`. Conforme.
- **core → catalog** : la prose des non-negotiables dit « core MUST NOT depend
  on catalog », mais la règle *enforcée* (ArchUnit) autorise `core` →
  `catalog/**/api` uniquement (précédent : `core.sales` consomme déjà
  `PricingCatalog` et `catalog.game.api.model.GameCode/BetOption`).
  `SelectionGenerationService` lira les règles de jeu **via `catalog.game.api`
  exclusivement** — jamais internal/infra.
- **Hook onboarding** : `platform/` MUST NOT depend on `core/` → le futur hook
  d'instanciation automatique vit dans `features.platformadmin` (qui porte déjà
  le provisioning), **pas** dans `platform.tenant`. La commande d'instanciation
  V1 est exposée par `core.promotion`.
- **RLS / transactions** : `sale_preparation` étend `BaseTenantEntity` ;
  persistance ticket + snapshot promotion dans le même `@TchTx` au confirm.
  Conforme (voir aussi promotion_design.md §12).
