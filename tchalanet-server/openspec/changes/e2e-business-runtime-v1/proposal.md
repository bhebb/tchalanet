# Change: e2e-business-runtime-v1

## Status

In progress — harness + POS/onboarding/auth done; public, dashboards, limits, promotions, concurrency remaining.

## Deepened — 2026-05-31

Audited the actual `testing/e2e/` tree against tasks.md. Significant work has landed since the initial spec:

**Done (checked off in tasks.md):**
- Full harness (§0): all 12 modules + supplementary `flows/`, `prereqs/`, `fixtures/` layers.
- Scenario world (§1): single-tenant A provisioning via conftest/fixtures; Tenant B/C and A/B config comparison not yet wired.
- Auth/context (§3): cashier role isolation (6 tests), super admin isolation (2 tests); tenant admin login not yet explicitly tested E2E.
- Onboarding (§5): all 8 spec items + 8 additional seller-management tests.
- Cashier POS (§6): full happy path + 4 extra context-error tests; 5 skipped tests await admin endpoints (lock, outlet-block, closed draw, etc.).
- Ticket matrix sells (§7): 4 of 5 scenarios implemented; SHORT_SINGLE_GAME_HIGH_STAKE missing. Deep payout assertions (line snapshots, stake total, money breakdown, potential payout) not yet added — current tests assert `ticketId` only.
- Idempotency (§10): 2 sequential tests done; concurrent and race variants not yet implemented.

**Remaining (open [ ] in tasks.md):**
- §1: Tenant B/C provisioning, A/B limit/promo config, cleanup policy.
- §2: Public runtime — zero tests.
- §3: Tenant admin login assertion, context/bootstrap endpoint.
- §4: Dashboards and overviews — zero tests.
- §7: SHORT_SINGLE_GAME_HIGH_STAKE; all deep payout assertions.
- §8: Limits — zero tests (depends on §1 multi-tenant).
- §9: Promotions — zero tests.
- §10: Missing key error, concurrent idempotency, all race scenarios.
- §11: Documentation.

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
8. **§4 Dashboards** — Les tests dashboard assertent uniquement : endpoint joignable, shape `ApiResponse`, widgets attendus présents, rôle/surface correct, pas de leak cross-tenant. Les KPIs dashboard ne sont pas source de vérité business. Les assertions business passent par les endpoints owning-domain.
9. **§7 HIGH_STAKE** — Tenant B configuré avec threshold explicite stake > 500 HTG. `SHORT_SINGLE_GAME_ALLOWED_STAKE` = 100 HTG (accepté), `SHORT_SINGLE_GAME_HIGH_STAKE` = 1000 HTG (bloqué). Asserter la règle limite active avant le scénario.
10. **§8/§9 Setup Tenant B** — Utiliser les admin APIs quand stables. Si incomplètes, seeds E2E acceptés sous conditions : idempotents, isolés au profil test, documentés dans la fixture, jamais mélangés à des SQL patches non documentés.

## Endpoints PageModel (découverts 2026-05-31)

- `GET /public/page-models/{logicalId}` — anonyme, résout par logicalId explicite (`PublicPageModelController`)
- `GET /tenant/page-models` — isAuthenticated, logicalId résolu par rôle depuis `TchRequestContext` (`TenantPageModelController`)
- `GET /platform/page-models` — SUPER_ADMIN seulement (`PlatformPageModelController`)
- `GET /admin/overview` — TENANT_ADMIN | SUPER_ADMIN (`TenantAdminOverviewController`)
- `GET /admin/policies/overview` — TENANT_ADMIN (`TenantAdminPoliciesController`)

Résolution de rôle → logicalId (via `PageModelTypeResolver`) :
- SUPER_ADMIN → `private.dashboard.superadmin`
- TENANT_ADMIN → `private.dashboard.tenant_admin`
- CASHIER → `private.dashboard.cashier.web`

Provider source keys (pour assertions dans les widgets) :
- `tenant_admin_dashboard` (TenantAdminDashboardProvider — widgets : header, kpis, readiness, alerts, operations, commercial, public_content, quick_actions)
- `public_home` (PublicHomeProvider — widgets : hero, news, plans, tchala, testimonials, features)
- `json_file` (JsonFileProvider — fragments shell/sidebar/header depuis classpath)
