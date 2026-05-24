# DOMAIN — Promotion

## 1. Rôle

`core.promotion` décide quelles promotions sont disponibles/applicables pour un contexte de vente ou une autre phase du cycle de vie.

Il ne crée pas directement des tickets, ne modifie pas les lignes, ne paie pas, ne settle pas.

```text
Promotion configure et décide.
Sales matérialise sur TicketLine/MoneyBreakdown.
Settlement utilise les snapshots.
Payout paie le résultat.
Ledger/stats consomment les events.
```

## 2. Concepts

### PromotionCampaign

Campagne tenant-scoped, versionnée et activable.

Exemples:

- `Maryaj gratuit par palier`
- `SMS offert avant midi`
- `Boost odds sur les boules pour les 100 premiers acheteurs`

### PromotionRule

Une règle de campagne: conditions + effet.

Exemples:

```text
WHEN paidTotal >= 500 GDES
THEN FREE_GAME_LINE MARYAJ_GRATUIT quantity=1
```

```text
WHEN paidTotal > 100 GDES AND paidLineCount(BOULE) > 5 AND before 12:00
THEN BOOST_ODDS targetGameCode=BOULE odds=60
```

### PromotionDecision

Résultat de l'évaluation pour une vente/contexte précis.

Elle est consommée par `sales`.

### AppliedPromotionSnapshot

Trace persistée de ce qui a réellement été appliqué pour un ticket.

Elle sert à l'audit, au reporting, à la réconciliation et à l'idempotence.

## 3. Types d'effets V1

```text
WAIVE_CHARGE       -> impact TicketMoneyBreakdown / charges
BOOST_ODDS         -> impact TicketLine oddsSnapshot / potentialPayoutAmount
FREE_GAME_LINE     -> ajoute TicketLine origin=PROMOTION
FREE_EXTRA_LINES   -> ajoute N TicketLine origin=PROMOTION
```

Extensions prévues:

```text
DISCOUNT_AMOUNT
DISCOUNT_PERCENT
BONUS_PAYOUT
COMMISSION_ADJUSTMENT
```

## 4. Éligibilité extensible

Les conditions peuvent couvrir:

- montant payé (`paidTotal >= 500`)
- quantité de lignes par jeu (`paidLineCount(BOULE) >= 5`)
- heure/date (`before 12:00`, jour spécial)
- zone/outlet/terminal/agent
- quota (`100 premiers acheteurs`)
- canal (`POS_ONLINE`, `POS_OFFLINE_SYNCED`, etc.)

## 5. Preview vs confirmation

- `SALE_PREVIEW`: indique ce qui semble disponible. Ne doit pas consommer de quota critique.
- `SALE_CONFIRMATION`: vérité transactionnelle. Peut réserver/consommer un quota.

Si une promotion utilise un quota concurrent (`100 premiers acheteurs`), la réservation doit être atomique pendant la confirmation.

## 6. Limites de responsabilité

Promotion ne doit pas:

- créer `TicketLine` directement;
- recalculer settlement historique;
- payer des claims;
- écrire dans les tables sales/payout/ledger.

Promotion doit:

- exposer `EvaluatePromotionQuery`;
- exposer une décision stable `PromotionDecision`;
- persister les snapshots appliqués via `CreateAppliedPromotionSnapshotCommand`;
- publier des events de promotion si nécessaire.

## 7. RLS et audit

Toutes les tables tenantées utilisent `BaseTenantEntity` côté Java et incluent en SQL:

```text
id, tenant_id, created_at, created_by, updated_at, updated_by, deleted_at, deleted_by, version
```

Repositories: pas de `WHERE tenant_id = ?` ni `WHERE deleted_at IS NULL` par défaut. RLS + `app.deleted_visibility` gouvernent la visibilité.
