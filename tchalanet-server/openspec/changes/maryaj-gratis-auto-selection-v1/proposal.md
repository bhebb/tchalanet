# OpenSpec Change — maryaj-gratis-auto-selection-v1

## Status

Proposed

## Goal

Livrer le Maryaj gratuit automatique par tenant : la vente éligible reçoit
automatiquement une ligne `HT_MARYAJ_GRATUIT` à sélection auto-générée,
régénérable avant confirmation (max 3), figée après confirmation.

Décision centrale (héritée de close-promotion-v1) :

```text
Promotion décide l'effet.
Sales génère les numéros et matérialise la ligne.
Settlement/Payout lisent les snapshots TicketLine.
```

## Why

- Le vendeur ne doit pas valider manuellement la promo : la ligne gratuite
  apparaît automatiquement au preview si une campagne tenant est ACTIVE.
- Le ticket final doit contenir exactement les numéros vus au preview —
  d'où une préparation de vente persistée côté serveur.
- Le frontend ne doit jamais pouvoir inventer une ligne gratuite.

## What

0. **Documentation d'abord** : DOMAIN_SALES.md, promotion_design.md,
   PLATFORM_TENANTCONFIG.md (onboarding), et le guide admin
   (`tchalanet-docs/docs/02-functional/guides/operator-admin-guide.md`) —
   désactiver la campagne pour un tenant, modifier les valeurs.
1. Étendre `PromotionEffect` : `selectionMode`, `generationStrategy`,
   `regenerableBeforeConfirm`, `maxRegenerationsBeforeConfirm`.
2. Seed du template `DEFAULT_MARYAJ_GRATIS` + commande d'instanciation tenant.
3. `SelectionGenerationService` + `RandomSelectionGenerator` dans `core.sales`
   (règles de jeu depuis catalog/runtime game config).
4. `SalePreparation` : entité persistée, TTL, statuts
   `DRAFT / CONFIRMED / EXPIRED / CANCELLED`, idempotencyKey.
5. `PrepareSaleCommand` → `RegenerateSalePreparationPromotionLineCommand`
   → `ConfirmPreparedSaleCommand`.
6. Reçu / events / snapshots promotion sur TicketLine.
7. Validation anti-forgerie côté serveur.

## Décisions tranchées

```text
CQRS            : persister une préparation = Command. PrepareSaleCommand,
                  pas de Preview Query stateful. PreviewTicketSaleQuery
                  stateless peut rester pour le calcul pur.
Offline         : Maryaj gratuit automatique = online only V1. Pas de ligne
                  gratuite offline tant que device-side signed preparation
                  n'existe pas.
RNG / abus      : maxRegenerationsBeforeConfirm = 3 par préparation.
                  Audit actor/session/terminal à chaque régénération.
                  RANDOM seul exposé au vendeur ; LOW_EXPOSURE_RANDOM
                  préparé en enum, implémentation retourne UNSUPPORTED.
Provisioning    : ne pas bloquer sur le hook onboarding. D'abord seed
                  template + commande d'instanciation tenant ; hook
                  onboarding ensuite.
Tenants         : nouveaux tenants -> campagne ACTIVE par défaut (via hook,
                  follow-up). Tenants existants -> backfill ops explicite
                  avec dry-run, jamais automatique silencieux.
payoutBaseAmount: 50 HTG par défaut (à confirmer), porté par le seed/config,
                  pas hardcodé.
Éligibilité     : minPaidTotal > 0 et au moins 1 ligne payante.
TTL préparation : 10 minutes (DRAFT). Expiration paresseuse + job
                  périodique. Purge EXPIRED/CANCELLED après 7 jours ;
                  CONFIRMED gardé 30 jours ou jusqu'à réconciliation.
                  Index (tenant_id, status, expires_at).
Regenerate      : endpoint dédié (pas de re-preview).
Template        : seed versionné d'abord ; table template plateforme plus
                  tard si nécessaire.
```

## Impact

- `core.promotion` — modèle effet, validation, vues admin, template/instanciation.
- `core.sales` — SelectionGenerationService, SalePreparation, 3 commands,
  TicketLine `selectionSource`, reçu, events.
- `tchalanet-catalog` — vérification (lecture seule) que les règles de
  génération de `HT_MARYAJ_GRATUIT` sont exposées au runtime.
- `tchalanet-app` migrations — colonnes effet + table sale_preparation +
  seed template. Pré-go-live : valider chaque `V*.sql` avant création.
- Settlement / Payout — aucun changement de comportement : snapshots only
  (gardes déjà dans close-promotion-v1 §12-13).

## Dependencies

- `close-promotion-v1` : champs TicketLine (§11), activation policy (§7),
  cache runtime (§9). Ne pas dupliquer ses tâches ici — les compléter là-bas.

## Non-goals

- Implémentation `LOW_EXPOSURE_RANDOM` (enum seulement).
- Maryaj gratuit en vente offline.
- Hook onboarding + backfill automatique (follow-up explicite).
- UI mobile POS — change compagnon `tchalanet-mobile/openspec/changes/`
  une fois le contrat API figé.
- Moteur de règles promotion complet.

## Open questions

- Mode **multiplicateur** pour `payoutBaseAmount` (vs valeur fixe V1) :
  évolution évoquée pour le guide admin, non supportée V1. À trancher
  avant de figer le modèle d'effet si on veut éviter une migration de plus.
- Montant par défaut 50 HTG : à confirmer avant le seed.

## Fallback (Phase A)

Si `SalePreparation` s'avère bloquante en cours de route : V1 sécurisée sans
préparation persistée — preview génère la ligne, confirm reçoit un
previewToken/promotionLineRef signé ou serveur-validé, backend revalide tout
et persiste. Non retenu comme plan, conservé comme issue de secours.
