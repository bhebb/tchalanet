# Domaine Session

> Status: DRAFT / MVP  
> Scope: `core.session`  
> Related:
>
> - Sales domain
> - Payout domain
> - Outlet domain
> - Terminal domain
> - Batch / Scheduler infrastructure
>
> Functional overview (MkDocs):
> `tchalanet-docs/docs/02-functional/domains/session.md`

---

# 1. Rôle du domaine

## Responsabilité principale

Le domaine `session` gère le cycle de vie des sessions POS (Point Of Sale)
utilisées par les agents/cashiers.

Une session représente un contexte opérationnel de vente :

- tenant
- outlet
- terminal
- utilisateur (agent)
- date métier (`businessDate`)
- état OPEN/CLOSED

Le domaine permet :

- l’ouverture manuelle ou automatique des sessions ;
- la fermeture manuelle ou automatique ;
- le calcul de clôture cash attendu ;
- l’exposition des vues de session utilisées par sales, payout et reporting.

---

## Ce que le domaine fait

- Ouvrir une session POS.
- Fermer une session POS.
- Associer une session à :
  - un outlet ;
  - un terminal ;
  - un utilisateur.
- Gérer les ouvertures/fermetures automatiques.
- Calculer les montants de clôture :
  - opening float ;
  - ventes ;
  - payouts ;
  - expected closing ;
  - declared closing ;
  - variance.
- Exposer des queries :
  - session courante ;
  - historique ;
  - sessions ouvertes ;
  - summaries.

---

## Ce que le domaine ne fait pas

- Ne vend pas de tickets (`sales`).
- Ne paie pas de payouts (`payout`).
- Ne calcule pas les résultats de tirage.
- Ne gère pas la comptabilité complète (`ledger` futur).
- Ne gère pas l’authentification des utilisateurs.

---

# 2. Modèle métier

## Agrégat principal

### `SalesSession`

Représente une session opérationnelle de caisse/vente.

### Champs principaux

- `id`
- `tenantId`
- `outletId`
- `terminalId`
- `openedBy`
- `openedAt`
- `businessDate`
- `status`
- `closedBy`
- `closedAt`
- `closeReason`
- `openingFloatCents`
- `expectedClosingAmountCents`
- `declaredClosingAmountCents`
- `varianceCents`

---

## Value objects / summaries

### `SalesSessionCashSummary`

Résumé financier utilisé lors de la clôture :

- opening float
- sales cash in
- payout cash out
- expected closing
- declared closing
- variance

---

## Statuts

### `SalesSessionStatus`

- `OPEN`
- `CLOSED`

Statuts futurs possibles :

- `ABORTED`
- `SUSPENDED`

---

# 3. Invariants métier

## Ouverture

- Une seule session ouverte par utilisateur et par business date.
- Une session doit être liée à :
  - un tenant ;
  - un outlet ;
  - un terminal ;
  - un utilisateur.
- Un terminal doit être actif.
- Un terminal auto-sessionnable doit avoir un `assignedUserId`.

---

## Fermeture

- Une session CLOSED ne peut plus être modifiée.
- Une session OPEN peut être fermée une seule fois.
- `closedAt` ne peut être défini qu’à la fermeture.
- Le montant attendu est calculé par le système.
- Le montant déclaré est fourni par l’utilisateur (manuel) ou null (auto-close).

---

# 4. Auto-open / Auto-close

## Principe

Le système peut ouvrir/fermer automatiquement des sessions via scheduler.

Le scheduler :

- lit les outlets auto-session-enabled ;
- trouve les terminaux éligibles ;
- ouvre/ferme les sessions nécessaires.

---

## Conditions auto-open

Un terminal est éligible si :

- terminal actif ;
- terminal assigné à un utilisateur ;
- outlet auto-session-enabled ;
- aucune session ouverte pour le business date courant.

---

## Conditions auto-close

Une session peut être auto-fermée si :

- statut OPEN ;
- outlet auto-close enabled ;
- heure de fermeture atteinte.

---

## Configuration

```yaml
tch:
  session:
    auto:
      active: true
      open-cron: '0 0 5 * * *'
      close-cron: '0 0 20 * * *'
```

---

# 5. Cas d'utilisation (commands / queries)

## Commands

### Ouverture

- `OpenSalesSessionCommand`
- `OpenDueSalesSessionsCommand`

### Fermeture

- `CloseSalesSessionCommand`
- `CloseDueSalesSessionsCommand`

## Queries

### Session courante

- `GetCurrentSessionQuery`

### Summaries cash

- `GetTicketSalesSummaryBySessionQuery`
- `GetPayoutSummaryBySessionQuery`

---

# 6. Calcul de clôture cash

Le domaine session ne lit pas directement les tables sales/payout.

Le handler orchestre :

```
session
  -> QueryBus.ask(sales summary)
  -> QueryBus.ask(payout summary)
  -> SessionCashCalculator.calculate(...)
  -> session.close(...)
```

## Formule MVP

```
expectedClosing =
    openingFloat
  + ticket sales
  - payouts paid
```

## Variance

```
variance =
    declaredClosing
  - expectedClosing
```

---

# 7. Services de domaine

## SessionCashCalculator

Service de domaine pur.

### Responsabilités

- calcul expected closing ;
- calcul variance ;
- produire `SalesSessionCashSummary`.

### Contraintes

Le service :

- n'utilise pas Spring ;
- n'utilise pas QueryBus ;
- ne fait aucune lecture DB.

---

# 8. Intégration inter-domaines

## Dépendances lecture

### Sales

Lecture des summaries de vente :

- nombre tickets ;
- montants ;
- statuts.

### Payout

Lecture des summaries payouts :

- paid amount ;
- counts ;
- statuts.

### Outlet

Lecture configuration auto-session :

- timezone ;
- auto open ;
- auto close.

### Terminal

Lecture terminaux actifs et assignés.

## Règles architecture

- Pas d'écriture cross-domain.
- Les intégrations sont read-only via queries/projections.
- Les schedulers utilisent CommandBus.
- Les handlers utilisent QueryBus pour lectures cross-domain.

---

# 9. Persistence

## Table principale

`sales_session`

Table tenantée avec RLS.

Historisée via Envers (`sales_session_aud`).

### Colonnes principales

- `tenant_id`
- `outlet_id`
- `terminal_id`
- `opened_by`
- `opened_at`
- `business_date`
- `status`
- `closed_by`
- `closed_at`
- `close_reason`
- `opening_float_cents`
- `expected_closing_amount_cents`
- `declared_closing_amount_cents`
- `variance_cents`

## RLS

Isolation multi-tenant via PostgreSQL RLS.

Aucun filtre tenant applicatif.

---

# 10. Scheduling / Batch

## Scheduler

`SalesSessionAutoScheduler`

### Responsabilités

- déclencher auto-open ;
- déclencher auto-close ;
- vérifier gates/configs.

Le scheduler ne contient aucune logique métier.

## Batch gates

Utilise :

- `BatchGate`
- `@BatchScheduledJob`

---

# 11. API REST

## Endpoints principaux

### Open

```
POST /tenant/sessions/open
```

### Close

```
POST /tenant/sessions/{sessionId}/close
```

### Current session

```
GET /tenant/sessions/current
```

---

# 12. Événements

## Events publiés

- `SalesSessionOpenedEvent`
- `SalesSessionClosedEvent`

Publication after-commit uniquement.

---

# 13. Notes techniques

- Typed IDs obligatoires.
- UUID uniquement en persistence.
- Commands transactionnels via `@TchTx`.
- Queries read-only.
- `CommandBus.execute(...)`
- `QueryBus.ask(...)`
- Scheduler thin orchestration only.

---

# 14. TODO / Evolutions futures

## Ledger integration

Remplacer les calculs cash directs par ledger/reconciliation.

## Statuts avancés

Possibles futurs :

- `ABORTED`
- `LOCKED`
- `RECONCILING`

## Reporting

- variance reporting ;
- anomalies ;
- daily summaries ;
- exports.

## Notifications

Possibles notifications :

- auto-open failed ;
- auto-close failed ;
- high variance ;
- orphan terminal/session.
