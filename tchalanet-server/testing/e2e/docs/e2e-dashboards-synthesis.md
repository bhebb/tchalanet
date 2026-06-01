# E2E Synthesis — Post-Login Dashboards, Overview & Readiness

**Status:** planning / scoping
**Author:** generated for the `feat/e2e-business-runtime-fixes-and-docs` branch
**Scope:** the pages a user lands on **after authentication**, per role.
**Out of scope:** public pages (`/public/...`) — already covered by `tests/public/`.

---

## 1. Goal

Cover, with real REST e2e tests (no seeds beyond what migrations provide, no
direct DB access), the **first authenticated surface** each role sees plus the
two structural-diagnosis surfaces that hang off it:

- the **dashboard PageModel** resolved server-side from the caller's role,
- the **overview** (structural sections + navigation),
- the **readiness** diagnosis (what is missing/partial before the tenant can operate).

These are read surfaces: success means *correct shape, correct role-gating, and
internally-consistent status* — not business mutations.

---

## 2. Roles in scope & their landing surface

| Role | Login → landing | PageModel endpoint | Overview | Readiness |
|------|-----------------|--------------------|----------|-----------|
| **CASHIER** (cashier web) | Cashier dashboard | `GET /tenant/page-models` | — | — |
| **TENANT_ADMIN** | Tenant admin dashboard | `GET /tenant/page-models` | `GET /admin/overview` | sections inside `/admin/overview` |
| **SUPER_ADMIN** | Platform dashboard | `GET /platform/page-models` | `GET /platform/overview` | per-tenant readiness via provisioning result / `/admin/overview` w/ override |

**Key invariant (security):** the PageModel `logicalId` is **resolved server-side
from the role in `TchRequestContext`** — the client never passes an arbitrary
`logicalId`. Tests must confirm each role gets *its own* model and cannot reach
another role's surface.

---

## 3. Endpoint inventory

| # | Method & path | Auth | Returns | Source |
|---|---------------|------|---------|--------|
| E1 | `GET /tenant/page-models?lang=` | authenticated | `DashboardPageModelResponse` (role-resolved) | `TenantPageModelController` |
| E2 | `GET /platform/page-models?lang=` | `SUPER_ADMIN` | `DashboardPageModelResponse` (super-admin) | `PlatformPageModelController` |
| E3 | `GET /admin/overview` | `TENANT_ADMIN` / `SUPER_ADMIN` | `TenantAdminOverviewView` (header, status, missingCount, sections[]) | `TenantAdminOverviewController` |
| E4 | `GET /platform/overview` | `SUPER_ADMIN` | `PlatformAdminOverviewView` (generatedAt, catalog, core, platform, sections[]) | `PlatformAdminOverviewController` |

**Readiness has no standalone endpoint.** It is a *projection embedded in*:
- `TenantAdminOverviewView.sections[]` → `TenantReadinessSection { id, labelKey, status, route, issues[] }`,
- the tenant-provisioning result (`TenantProvisioningResultView.readiness` → `TenantReadinessView`),
- (and, per the model docs, a `TenantReadinessSummary` consumed by the tenant-admin dashboard PageModel).

So readiness is tested **through** E3 and through the provisioning result, not as
its own call.

---

## 4. Response shapes (assertion contracts)

**`DashboardPageModelResponse`** (E1/E2) — shape to be pinned from
`pagemodel/.../DashboardPageModelResponse.java` (TODO: confirm fields:
`logicalId`, `version`, `sections`/`widgets`, resolved `lang`). Assertions:
non-empty body, a stable `logicalId`, `lang` echoes the requested locale.

**`TenantAdminOverviewView`** (E3):
```
header { tenantId, tenantCode, tenantName, timezone, currency, tenantType, tenantStatus }
status        : READY | PARTIAL | MISSING | UNKNOWN
missingCount  : int
sections[]    : TenantReadinessSection { id, labelKey, status, route, issues[] }
```
**MUST NOT** contain dashboard KPIs (`salesToday`, `ticketCountToday`,
`activeSessions`, `openDraws`) — assert their absence (overview is structural,
not KPI).

**`PlatformAdminOverviewView`** (E4):
```
generatedAt
catalog  { games, resultSlots, i18nGlobalKeys, pageModelTemplates, themePresets }  (CountItem = {total, active})
core     { tenants {total, active, suspended}, subscriptions {total, active, pastDue, canceled, byPlan[]} }
platform { plans, pricingRules, globalSettings }
sections[] { key, enabled, href }
```

`TenantReadinessSection.status` ∈ `{READY, PARTIAL, MISSING, UNKNOWN}`;
`missingCount` must equal the count of non-`READY` sections (consistency check).

---

## 5. Test scenarios

### 5.1 Dashboard PageModel — `tests/dashboard/` (`@dashboard`)

| ID | Marker | Scenario | Assert |
|----|--------|----------|--------|
| D1 | L1 | CASHIER calls `GET /tenant/page-models` | 200; body present; `logicalId` = cashier dashboard; not the admin model |
| D2 | L1 | TENANT_ADMIN calls `GET /tenant/page-models` | 200; `logicalId` = tenant-admin dashboard (≠ D1) |
| D3 | L1 | SUPER_ADMIN calls `GET /platform/page-models` | 200; `logicalId` = platform dashboard |
| D4 | L2 | `?lang=ht` then `?lang=fr` | 200; resolved `lang` echoes request; labels differ or key present |
| D5 | L1 | CASHIER (or unauth) calls `GET /platform/page-models` | 403 (super-admin only) |
| D6 | L2 | no token → `GET /tenant/page-models` | 401 |

### 5.2 Tenant Overview — `tests/overview/` (`@overview`, `@readiness`)

| ID | Marker | Scenario | Assert |
|----|--------|----------|--------|
| O1 | L1 | TENANT_ADMIN `GET /admin/overview` | 200; header.tenantCode = `tchalanet`; `sections[]` non-empty |
| O2 | L1 | overview omits KPI fields | no `salesToday`/`ticketCountToday`/`activeSessions`/`openDraws` keys |
| O3 | L2 | readiness consistency | every `section.status` ∈ enum; `missingCount` == #(sections not READY) |
| O4 | L2 | section catalog | section ids cover the spec set (users, outlets, terminals, draws, games_pricing, …) |
| O5 | L1 | CASHIER `GET /admin/overview` | 403 (admin/super only) |
| O6 | L2 | seeded `tchalanet` tenant is operational | top-level `status` = READY (or documents the missing sections if not) |

### 5.3 Platform Overview — `tests/overview/` (`@platformadmin`)

| ID | Marker | Scenario | Assert |
|----|--------|----------|--------|
| P1 | L1 | SUPER_ADMIN `GET /platform/overview` | 200; `generatedAt` present; `catalog/core/platform` blocks present |
| P2 | L2 | counts sane | all `CountItem.active ≤ total`; `tenants.total ≥ 1` (seeded tenant) |
| P3 | L2 | sections | `sections[]` items have `key` + boolean `enabled` + `href` |
| P4 | L1 | TENANT_ADMIN `GET /platform/overview` | 403 |
| P5 | L2 | no token | 401 |

### 5.4 Readiness (through overview) — folded into O3/O4/O6

Readiness is asserted via the embedded `sections[]`. A dedicated test verifies
the **summary↔detail consistency**: rolled-up `status`/`missingCount` agree with
the per-section statuses.

---

## 6. Fixtures & auth (already available in `conftest.py`)

| Fixture | Role | Notes |
|---------|------|-------|
| `super_admin_client` | SUPER_ADMIN | `super_admin` / env |
| `tenant_admin_client` | TENANT_ADMIN | seeded `admin` (tenant_code=tchalanet) |
| `cashier_client_a` / `cashier_client` | CASHIER | seeded `cashier`, carries `X-Device-Binding` |
| `base_url` | — | `https://api.localtest.me/api/v1` |

Negative-auth cases need a **token-less `ApiClient`** (construct inline) and a
**wrong-role** client (reuse `cashier_client` against admin/platform paths).

---

## 7. File layout & markers

```
tests/
  dashboard/   test_dashboard_pagemodels.py   @dashboard
  overview/    test_tenant_overview.py        @overview @readiness
               test_platform_overview.py      @platformadmin
```

New markers registered in `pytest.ini`: `dashboard`, `overview`,
`platformadmin`, `readiness`.

Run: `pytest -m "dashboard or overview or platformadmin"  -m "not L3"`.

---

## 8. Open questions / assumptions

1. **`DashboardPageModelResponse` fields** — need to pin exact JSON keys
   (`logicalId`, `version`, `sections`?) before writing D1–D4 assertions.
2. **Cashier "web admin" vs POS** — confirm whether "cashier web admin" is a
   distinct role/logicalId or just the cashier dashboard PageModel. Resolver is
   `PageModelTypeResolver.forDashboard(role)`.
3. **`lang` values** — confirm supported locales (`ht`, `fr`, `en`?) for D4.
4. **Per-tenant readiness for SUPER_ADMIN** — does `/admin/overview` accept an
   `X-Tenant-Id` override for super-admin to inspect another tenant's readiness?
5. **Readiness summary in the dashboard PageModel** — is `TenantReadinessSummary`
   actually embedded in the tenant-admin dashboard payload (would let D2 assert it)?

---

## 9. Not in this slice (tracked separately)

- **Tenant provisioning completeness** (`/platform/tenant-onboarding/*`): a
  freshly-provisioned `DEFAULT_HAITI_LOTTERY` tenant reports
  `draw_channels=DEFAULT_HAITI` / `games=DEFAULT_LOTTERY` in `domainStatuses`,
  but the orchestrator only creates the tenant + initial admin — it does **not**
  actually seed games/pricing/draw-channels. Readiness for a new tenant will
  therefore show those sections MISSING. Whether to (a) assert current behavior
  or (b) implement real seeding is a **scope decision** to confirm before coding.
```
