# OpenSpec Change — Promotion Effects Integration

## Goal

Mettre en place le socle extensible des promotions V1:

- Promotion configure/décide.
- Sales applique les effets sur `TicketLine` et `TicketMoneyBreakdown`.
- Settlement utilise les snapshots.
- Payout paie le résultat.
- Ledger/stats consomment les events.

## Scope

### Included

- `core.promotion` API minimale: evaluate query, decision models, applied snapshot command.
- Persistence promotion tenantée avec audit/RLS.
- Impact `sales.ticket_line`: origin/pricingSource/selectionSource/payoutBaseAmount/promotionDecisionId.
- Event handler promotion pour snapshot appliqué depuis `TicketPlacedEvent` ou fallback command after-commit.
- Seeds exemples pour `MARYAJ_GRATUIT` / `BOULE_GRATUIT`.

### Excluded / later

- Rule engine externe.
- UI tenant admin complet.
- Ledger double-entry final.
- Quota distribué multi-node avancé au-delà du row lock DB.

## Risks

- Cycle logique `sales -> promotion` et `promotion -> sales.api.event` si listener event-driven activé.
- Quota concurrent mal protégé.
- Reprint qui lit la DB avant snapshot promotion si snapshot créé after-commit.
- Settlement qui recalcule par erreur la promotion au lieu de lire les snapshots.

## Verification checklist

- [ ] Aucune classe `core.promotion.internal` importée par `sales`.
- [ ] Aucune classe `core.sales.internal` importée par `promotion`.
- [ ] Repositories tenantés n'ont pas `WHERE tenant_id = ?`.
- [ ] Soft-delete via RLS/deleted_visibility; pas de filtre `deleted_at` en repo par défaut.
- [ ] Tables tenantées ont les colonnes BaseTenantEntity + `_aud`.
- [ ] RLS activée sur toutes les tables tenantées.
- [ ] `TicketLine` conserve `oddsSnapshot` et `payoutBaseAmount`.
- [ ] Settlement utilise snapshots, pas PromotionConfig.
- [ ] `CreateAppliedPromotionSnapshotCommand` idempotente.
- [ ] Quota confirmation utilise row lock / update atomique.
