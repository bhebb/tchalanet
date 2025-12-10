Ce document décrit clairement :

Le rôle du domaine stats / aggregates

Les tables (stats_daily, stats_draw, stats_event_log)

Le flux complet (command → event → projection → dashboard)

Le batch de recompute

La stratégie “real-time + batch truth correction”

Les limites et règles de cohérence

Le plan de résilience et de tests

Il est écrit pour être facilement lisible par n’importe quel nouveau développeur dans Tchalanet.

DOMAIN_STATS.md

# 📊 Domaine : Stats & Aggregates

**Tchalanet – Architecture Fonctionnelle & Technique**

## 1. Objectif du domaine

Le domaine **stats/aggregates** fournit une couche de **lecture rapide**, stable et cohérente pour les dashboards privés (Super Admin, Tenant Admin, Cashier, Operator).  
Il s'appuie sur deux mécanismes complémentaires :

1. **Calcul batch “vérité”** (rebuild) à partir des tables métiers (`ticket`, `session`, `draw`, `payout`, etc.).
2. **Mises à jour incrémentales** en temps réel via les _Domain Events_ du système.

Ce domaine sépare la charge analytique des tables transactionnelles, réduisant :

- les latences,
- les risques de lock/slow queries,
- les coûts des GROUP BY en conditions réelles.

---

## 2. Architecture générale

Flux complet :

[ CommandHandler (sales/draw/etc.) ]
│
▼
[ Domain Events ] (TicketPlacedEvent, SessionClosedEvent, DrawResultedEvent…)
│
▼
[ StatsAggregatesEventListener ]
│
▼
[ StatsDailyUpdater / StatsDrawUpdater ] → INSERT ... ON CONFLICT DO UPDATE
│
▼
stats_daily / stats_draw
│
▼
[ Dashboard Use Cases ] (TenantDashboardStatsUseCase, CashierDashboardStatsUseCase…)

En parallèle, un batch périodique :

RecomputeDailyStatsUseCase → rebuild complet via SELECT ... GROUP BY sur tables métier

Ce batch assure la **cohérence absolue** et peut corriger n’importe quelle dérive liée à un event manquant, un bug, ou un reset.

---

## 3. Tables utilisées

### 3.1. `stats_daily`

Agrégats journaliers pour chaque dimension :

- platform
- tenant
- outlet
- cashier

Colonnes principales :

dimension_type text ('platform' | 'tenant' | 'outlet' | 'cashier')
dimension_id uuid|null
ref_date date

tickets_count bigint
tickets_cancelled_count bigint
stake_sum_cents bigint
winnings_sum_cents bigint
net_revenue_cents bigint
payouts_count bigint
sessions_opened_count bigint
sessions_closed_count bigint

### 3.2. `stats_draw`

Statistiques par tirage individuel :

draw_id uuid (unique)
tenant_id uuid
game_code text
scheduled_at timestamptz

tickets_count bigint
stake_sum_cents bigint
winnings_sum_cents bigint
net_revenue_cents bigint

### 3.3. `stats_event_log` (idempotence)

Assure que chaque événement métier n’est traité qu’une seule fois.

---

## 4. Sources des données

### 4.1. Batch “vérité”

`RecomputeDailyStatsUseCase(from, to)` :

- supprime les lignes existantes dans la plage,
- reconstruit via des requêtes SQL optimisées sur les tables :
  - `ticket`,
  - `ticket_settlement`,
  - `payout`,
  - `session`,
  - éventuellement `draw_bet` selon ton modèle.

Le batch est idéalement programmé :

- chaque nuit,
- et peut être relancé manuellement.

### 4.2. Incrémental basé sur événements

Événements utilisés :

| Event                | Action                              | Mise à jour dans stats        |
| -------------------- | ----------------------------------- | ----------------------------- |
| TicketPlacedEvent    | Vente ou enregistrement d’un ticket | +1 ticket, +stake             |
| TicketCancelledEvent | Annulation                          | -1 ticket, +1 cancelled       |
| TicketSettledEvent   | Gain/perte déterminée               | +winnings, +payoutCount, +net |
| SessionOpenedEvent   | Début session caisse                | +sessionsOpened               |
| SessionClosedEvent   | Fin session caisse                  | +sessionsClosed               |
| DrawResultedEvent    | Résultat tirage                     | créé ligne dans `stats_draw`  |

Le listener applique des **UPSERT** via :

```sql
INSERT ... ON CONFLICT (dimension_type, dimension_id, ref_date)
DO UPDATE SET counters = counters + EXCLUDED.counters

5. Règles opérationnelles
5.1. Window temporelle des events

On ne met à jour que Aujourd’hui et J-1 via les events.
Pourquoi ?

éviter de modifier des dates anciennes qui seront reconstruites par le batch,

éviter les incohérences liées à des retards d’événements,

simplifier la logique.

5.2. Garantie de cohérence finale (“eventual consistency but repairable”)

Même si :

un event n’est pas reçu,

une projection échoue,

l'application redémarre,

un bug apparaît,

La cohérence finale est rétablie par :

RecomputeDailyStatsUseCase


→ c’est le plan de secours par conception.

6. Use cases qui consomment les stats
6.1. Tenant Dashboard

Lit uniquement :

stats_daily (dimension_type='tenant')

Optionnel : stats_draw pour breakdown par tirage

6.2. Cashier Dashboard

Lit :

stats_daily (dimension_type='cashier')

6.3. Outlet Dashboard (si ajouté plus tard)

Lit :

stats_daily (dimension_type='outlet')

6.4. Super Admin / Platform Dashboard

Lit :

platform-level (dimension_type='platform')

aggregation des tenants

éventuellement stats_draw

Tous ces dashboards NE DOIVENT PLUS faire de requêtes lourdes sur ticket.

7. Règles de maintenance
7.1. En cas de bug de stats incorrectes

Procédure :

Identifier la période fautive.

Exécuter :

RecomputeDailyStatsUseCase(from, to)


Vérifier les résultats.

Si besoin : corriger le listener ou upsert, redeployer.

7.2. En cas d’incohérence prolongée

Solution :

Réinitialisation complète :

TRUNCATE stats_daily, stats_draw, stats_event_log;
RecomputeDailyStatsUseCase(last 30 days)

7.3. En cas de backlog massif d’événements

→ la base “vérité” (batch) reprend le dessus.

8. Tests attendus
8.1. Tests unitaires

StatsDailyUpdater : deltas corrects

StatsAggregatesEventListener : idempotence, window, upserts

TenantDashboardFromAggregatesService : mapping correct

8.2. Tests intégration

Insertion tickets → events → projection

Comparaison avec batch → les deux doivent donner les mêmes chiffres

9. Limites connues

Les événements ne sont pas destinés à reconstituer toute l’histoire → le batch couvre tout.

Le domaine stats n'est pas transactionnel avec les tables métier → c’est volontaire.

Pour des analyses longues (annuelles) → prévoir un reporting externe (BigQuery, Redshift, etc.).

10. Conclusion

Le domaine stats/aggregates fournit :

une lecture ultra-rapide pour les dashboards,

une cohérence garantie par une stratégie mixte event-driven + batch,

une isolation claire entre transactionnel et analytique,

un plan de reprise simple et robuste.

C’est une architecture scalable, maintenable et résiliente alignée avec les bonnes pratiques hexagonales & analytique moderne.
```
