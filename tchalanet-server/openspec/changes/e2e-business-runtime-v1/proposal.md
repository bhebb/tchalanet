# Change: e2e-business-runtime-v1

## Status

Draft V1.

## Why

Après le cadrage public/dashboard/overview, il faut une suite E2E qui prouve que la plateforme fonctionne comme un tout :

- plusieurs tenants ;
- plusieurs rôles ;
- configs différentes ;
- POS cashier ;
- ventes ;
- tickets court/moyen/long ;
- tous les jeux V1 activés ;
- stakes différents ;
- potential payout ;
- limites ;
- promotions ;
- idempotency ;
- isolation tenant ;
- petites courses concurrentes.

## Goals

- Créer un monde de test reproductible.
- Tester public runtime sans auth.
- Tester login/context par rôle.
- Tester onboarding tenant/user/outlet/terminal.
- Tester dashboards PageModel et overviews.
- Tester POS/cashier home, bind/session/sell/print/send.
- Tester tickets avec sélections différentes, tailles différentes, jeux différents, stakes différents.
- Vérifier potential payout à partir des snapshots retournés.
- Tester limites et promotions V1.
- Tester isolation multi-tenant.
- Tester petites concurrences de correction.

## Non-goals

- Tests de performance.
- Tests de charge.
- Latence p95/p99.
- Benchmark DB.
- Toutes validations CRUD.
- Tous les filtres rapports.
- Tous les détails UI.

## Key decisions

1. Les tests E2E sont scénario-first, pas endpoint-first.
2. La concurrence E2E est une concurrence de correction, pas de performance.
3. Les tests de performance feront partie d’un autre change.
4. Les tickets de vente doivent varier : court, moyen, long, tous les jeux V1, stakes différents, sélections différentes.
5. Chaque vente réussie doit vérifier line snapshots, stake total, money breakdown et potential payout.
6. Les promotions doivent être vérifiées via snapshots sales.
7. Les limites doivent inclure des cas normaux, bloqués et race condition contrôlée.
