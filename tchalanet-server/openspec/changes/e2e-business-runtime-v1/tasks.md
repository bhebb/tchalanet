# Tasks — E2E Business Runtime V1

<!-- Synced to actual implementation 2026-05-31. -->

## 0. Harness

- [x] Create `testing/e2e/pyproject.toml`.
- [x] Create `pytest.ini` with markers.
- [x] Create `.env.example`.
- [x] Implement `config.py`.
- [x] Implement `client.py`.
- [x] Implement `auth.py`.
- [x] Implement `api_response.py`.
- [x] Implement `assertions.py`.
- [x] Implement `data_factory.py`.
- [x] Implement `scenario_world.py`.
- [x] Implement `ticket_matrix.py`.
- [x] Implement `concurrency.py`.

> Also implemented (beyond original spec): `flows/` (home, terminal, onboarding, profile, outlet, cashier, seller),
> `prereqs/` (draws, app_user, session), `fixtures/pos_context.py`.

## 1. Scenario world

- [x] Generate unique `run_id`.
- [x] Provision Tenant A (via conftest + `flows/onboarding.py`).
- [ ] Provision Tenant B with different config.
- [ ] Provision Tenant C minimal/incomplete.
- [x] Create tenant admins.
- [x] Create cashiers.
- [x] Create outlets.
- [x] Create terminals.
- [x] Bind terminals.
- [x] Open sessions.
- [ ] Configure A/B limits differently.
- [ ] Configure A/B promotions differently.
- [x] Store IDs/tokens in fixture world (`PosContext`).
- [ ] Add cleanup/deactivation policy.

> Multi-tenant B/C provisioning not yet in conftest — current suite runs single-tenant.
> Limits/promotions configuration for A vs B blocked on multi-tenant world.

## 2. Public runtime

> Implemented in `tests/public/test_public.py` (17 tests). Full anonymous surface swept:
> `/public/settings`, `/public/i18n`, `/public/news`, `/public/draw-results/{slots,history}`,
> `/public/page-models/{logicalId}`, `/public/tickets/{code}/verify`, `/public/tchala/*`,
> `/public/security/backend-signing-keys`.

- [x] Public home loads without auth (`test_public_home_page_model_loads`).
- [x] Public home uses `public_home` (asserts `meta.context == public_home`, `scope == public`).
- [x] Public home contains no private provider source (`test_public_home_has_no_private_provider_source`).
- [x] Public draw results loads without auth (`test_public_draw_result_slots` / `_history`).
- [x] Public draw results expose provider/slot metadata (slots carry `provider`/`slotKey`).
- [x] Public ticket check unknown code returns controlled 404 / no 500 (`test_public_ticket_check_unknown_is_controlled`).
- [ ] Public ticket check sold ticket returns expected public status. *(deferred — needs a real sold ticket publicCode + verificationCode)*
- [x] Public settings reachable + namespace filter (`test_public_settings_*`).
- [x] Public i18n bundle (locale + multi-surface; missing-locale → 400) (`test_public_i18n_*`).
- [x] Public endpoints survive parallel anonymous reads (L3 `test_public_endpoints_handle_parallel_reads`).

## 3. Auth/context

- [x] Login super admin (`test_super_admin_can_access_platform`).
- [ ] Login tenant admin A/B (implicit in setup, no explicit E2E assertion).
- [x] Login cashier A/B (`test_cashier_can_access_home`).
- [ ] Context/bootstrap returns expected role/tenant if endpoint exists.
- [x] Tenant-scoped endpoints do not trust client tenant id (`test_cashier_cannot_list_platform_tenants`).
- [x] Platform context is distinct (`test_super_admin_cannot_sell_tickets`, `test_super_admin_can_access_platform`).

> Also implemented: `test_cashier_cannot_access_platform_ops`, `test_cashier_cannot_generate_draws`.

## 4. Dashboards and overviews

> Scope décidé : smoke contract uniquement. Asserter endpoint joignable + shape ApiResponse + widgets attendus
> présents + rôle/surface correct + pas de leak cross-tenant. Jamais les valeurs KPI comme vérité business.
>
> Implemented in `tests/dashboard/test_dashboard_pagemodels.py`,
> `tests/overview/test_tenant_overview.py`, `tests/overview/test_platform_overview.py`.
> Post-login surfaces only (public pages already covered by `tests/public`). Cashier surface
> = the *web* dashboard (`private.dashboard.cashier.web`, seller on computer/tablet, page
> engine), distinct from the POS/Android app in `tests/cashier_pos`.

- [x] `GET /tenant/page-models` — TENANT_ADMIN → logicalId `private.dashboard.tenant_admin`, source `tenant_admin_dashboard`.
- [x] `GET /tenant/page-models` — response contains expected widget ids (header, kpis, readiness, alerts, operations).
- [x] `GET /tenant/page-models` — CASHIER cannot access (resolves a *different* page: `private.dashboard.cashier.web`, source `cashier_dashboard`).
- [x] `GET /tenant/page-models` — no cross-surface provider leak (tenant_admin model must not carry `platform_admin_dashboard`, and vice-versa).
- [x] `GET /platform/page-models` — SUPER_ADMIN → logicalId `private.dashboard.superadmin`.
- [x] `GET /platform/page-models` — TENANT_ADMIN cannot access (403); CASHIER cannot access (403).
- [x] `GET /admin/overview` — TENANT_ADMIN returns sections/status/missingCount; omits dashboard KPI fields.
- [x] `GET /admin/overview` — CASHIER cannot access (403).
- [x] `GET /admin/policies/overview` — TENANT_ADMIN returns tenantAssignmentsCount/autonomyConfigured.
- [x] `GET /platform/overview` — SUPER_ADMIN returns catalog/core/platform/sections (counts sane); TENANT_ADMIN 403.
- [x] Readiness consistency (via overview sections): valid statuses + `missingCount` == #(MISSING)+#(PARTIAL).
- [x] `?lang=fr|en|ht` echoes in `currentLang` (tenant admin dashboard).

> **Runtime bugs surfaced + fixed by this section (committed on the e2e branch):**
> 1. `GlobalErrorHandler` had no handler for Spring Security `AccessDeniedException`, so every
>    `@PreAuthorize` (method-security) denial fell through to the catch-all → **500 instead of 403**.
>    Added an `AccessDeniedException` handler → 403 (`code=access.denied`).
> 2. `GET /platform/overview` → **500**: `ThemePresetJpaEntity.config` was `@Lob String` on a PG
>    `text` column; on PostgreSQL `@Lob String` maps as a large-object OID (read via `getLong`) →
>    "Bad value for type long". Removed `@Lob`.
> 3. `GET /tenant/outlets/{id}/{operational-context,sales-capability}` guarded on a **non-existent**
>    authority `TENANT_USER` (real roles: CASHIER/OPERATOR/TENANT_ADMIN/SUPER_ADMIN) → denied everyone
>    (was masked as 500, now correctly fixed). Guard set to
>    `hasAnyAuthority('CASHIER','TENANT_ADMIN','SUPER_ADMIN','OPERATOR')`.

## 5. Onboarding

- [x] Tenant provisioning preview is read-only (`test_provisioning_preview_is_read_only`).
- [x] Tenant provisioning preview returns domains/readiness (`test_provisioning_preview_returns_domains_and_readiness`).
- [x] Tenant provisioning creates tenant if endpoint available (`test_provision_tenant_creates_tenant`).
- [x] User onboarding creates tenant admin (`test_user_onboarding_creates_tenant_admin`).
- [x] User onboarding creates cashier (`test_user_onboarding_creates_cashier`).
- [x] Outlet onboarding creates active outlet (`test_outlet_onboarding_creates_active_outlet`).
- [x] Terminal onboarding creates terminal (`test_terminal_onboarding_creates_terminal`).
- [x] Terminal binding prepares POS flow (`test_terminal_binding_prepares_pos_flow`).

> Also implemented: seller CRUD (`test_seller_onboarding.py` — create, list, get, assign, receipt config,
> sales capability, activation challenge, full bind flow, admin shortcut).

### 5b. Full tenant onboarding from scratch — NO seed (`test_tenant_no_seed_onboarding.py`)

> Drives the spec flow with no pre-existing seed: SUPER_ADMIN provisions a brand-new tenant
> + initial TENANT_ADMIN, then completes the config via REST acting as the new tenant
> (X-Tenant-Id override — the optional "super admin finishes the config" path). Asserts
> readiness goes MISSING → READY.

- [x] Provision a fresh tenant + initial admin; provision-result readiness shows identity READY, users/outlets/terminals MISSING, overall MISSING.
- [x] initialAdminEmail provided → initial TENANT_ADMIN created, `CREATE_INITIAL_ADMIN` absent from nextSteps.
- [x] Complete config without seed: apply plan → outlet → cashier user → seller (linked+assigned) → terminal; readiness rolls up to READY.
- [x] Document the catalog gap: DEFAULT_HAITI_LOTTERY provision reports games/draw_channels seeded in `domainStatuses` but nothing is created (games_pricing/draws stay UNKNOWN).

> **Runtime bugs surfaced + fixed by the no-seed flow (committed on the e2e branch):**
> 4. `POST /admin/terminals` (and every `@RequiredFeature` endpoint: promotions, phone-sell,
>    terminal-licensing) → **500 NPE**. `RequiredFeatureAspect` used a
>    `@annotation(x) || @within(x)` pointcut; Spring AOP can't bind `x` from an OR of
>    method- and class-level annotations, so it passed `null`. Now resolves the annotation
>    from the join point (method → declaring class).
> 5. `POST /admin/outlets` with no `slug` → **500** (DataIntegrityViolation: slug NOT NULL).
>    Added `@NotBlank` on `CreateOutletRequest.name/slug` → clean 400.
>
> **Provisioning-completeness gaps (NOT yet fixed — need a product decision):**
> - Provisioning does **not** attach a subscription/plan, so a fresh tenant can't create
>   outlets until `POST /platform/subscriptions/{id}/apply` is called (the e2e does this as
>   an explicit step). `EntitlementService.requireLimitAtMost` throws `ProblemRest.internal`
>   (500) when no plan — arguably should be a clearer 4xx, and/or provisioning should apply a
>   default plan.
> - Provisioning does **not** seed games/pricing/draw_channels for DEFAULT_HAITI_LOTTERY
>   despite advertising them in `domainStatuses` ("channels pas copiés"). A provisioned tenant
>   is readiness-READY but cannot actually sell (no draws/games). No tenant-admin REST path to
>   create them yet (catalog-level).
> - `GET /platform/plans` → 500 (separate, not on the onboarding critical path).

## 6. Cashier POS

- [x] POS home works with active context.
- [x] Missing context returns required step.
- [x] Closed session returns required step.
- [x] Open session.
- [x] List available draws.
- [x] Sell short ticket.
- [x] Print ticket (fixed payload: `printOptionsRequest.outputFormat`, not top-level `format`).
- [x] Send ticket notification if configured (`§9 step 8`, env-guarded via `TCH_TEST_SLACK_CHANNEL_KEY`).
- [x] List recent tickets.
- [x] Close session (`§9 step 10`, asserts session no longer OPEN).
- [ ] Terminal locked blocks sell. *(skipped — needs admin lock endpoint)*
- [ ] Outlet blocked/sales disabled blocks sell. *(skipped — needs admin outlet-block endpoint)*
- [x] Session mismatch blocks sell.
- [x] Cross-tenant terminal/outlet/session usage is blocked.

> Also implemented in `test_pos_sell_context_errors.py`: missing session header, bogus terminal, unknown game
> code, bogus draw id. Skipped (needs fixture prerequisites): outlet-terminal mismatch, other-seller session,
> closed draw, missing pricing odds, stake limit exceeded.

> Happy path `test_cashier_morning_happy_path` covers the full §9 main path
> (draws → preview → sell → print → send → list → close). Aligned with the live
> cashier API: `/tenant/cashier/tickets/{preview,sell,{id}/print,{id}/send}`,
> `SaleDecision.ACCEPTABLE`, `SellTicketOutcome.ACCEPTED`,
> `PrintTicketRequest.printOptionsRequest.outputFormat`.

## 7. Ticket matrix and payout

- [x] Implement SHORT_SINGLE_GAME_LOW_STAKE (`test_sell_short_single_game`).
- [ ] Implement SHORT_SINGLE_GAME_ALLOWED_STAKE (100 HTG, Tenant B, must be accepted — prerequisite for §8).
- [ ] Implement SHORT_SINGLE_GAME_HIGH_STAKE (1000 HTG, Tenant B, must be blocked — requires Tenant B limit > 500 HTG configured).
- [x] Implement MEDIUM_MULTI_LINE_MIXED_STAKE (`test_sell_medium_multi_line_mixed_stake`).
- [x] Implement MEDIUM_MULTI_GAME (`test_sell_medium_multi_game`).
- [x] Implement LONG_ALL_GAMES (`test_sell_long_all_games` + `test_sell_long_and_list_tickets`).
- [x] Ensure selections differ (defined in `ticket_matrix.py`).
- [x] Ensure stakes differ (defined in `ticket_matrix.py`).
- [x] Load enabled games/pricing where possible (`_draw_supporting` skips if game unavailable).
- [ ] Assert line snapshots.
- [ ] Assert stake total.
- [ ] Assert money breakdown.
- [ ] Assert potential payout from snapshots.
- [x] Assert print payload for long ticket (PDF binary check via `flow.print_pdf`).

> Current sell tests assert `ticketId` presence only. Deep snapshot assertions (lines, stakes, payout,
> money breakdown) are the remaining gap in this section.
> HIGH_STAKE scenario requires Tenant B with explicit limit rule (threshold > 500 HTG) — assert rule active before scenario.

## 8. Limits

> Setup Tenant B : utiliser admin API si stable, sinon seeds E2E (idempotents, isolés profil test, documentés).
> Ne pas mélanger API setup et SQL patches non documentés.
> Prérequis : Tenant B configuré avec threshold stake > 500 HTG pour SHORT_SINGLE_GAME.

- [ ] Configure Tenant B limit rule (stake > 500 HTG per line, SHORT_SINGLE_GAME) via admin API or seed.
- [ ] Assert limit rule active before running blocked scenarios.
- [ ] Below limit succeeds (100 HTG — `SHORT_SINGLE_GAME_ALLOWED_STAKE`).
- [ ] Exactly at limit follows documented rule.
- [ ] Above line limit blocks (1000 HTG — `SHORT_SINGLE_GAME_HIGH_STAKE`).
- [ ] Above ticket total limit blocks.
- [ ] Per-selection/exposure limit blocks if implemented.
- [ ] Tenant A/B limit configs independent.
- [ ] Controlled concurrent race does not allow overexposure.

## 9. Promotions

- [ ] BOOST_ODDS eligible sale uses odds override snapshot.
- [ ] BOOST_ODDS potential payout uses boosted odds.
- [ ] BOOST_ODDS does not modify base pricing odds.
- [ ] FREE_GAME_LINE materializes free/promotional line or snapshot.
- [ ] FREE_GAME_LINE paid total remains correct.
- [ ] WAIVE_CHARGE waives charge in money breakdown.
- [ ] WAIVE_CHARGE does not modify line pricing/payout.
- [ ] Non-eligible cases do not apply promotion.
- [ ] Tenant A promotion does not affect B.

## 10. Idempotency and concurrency

- [ ] Missing Idempotency-Key on required sell endpoint returns expected error.
- [x] Same key + same payload returns same ticket/result (`test_sell_same_key_same_payload_is_idempotent`).
- [x] Same key + different payload returns payload mismatch (`test_sell_same_key_different_payload_returns_conflict`).
- [x] Concurrent same key + same payload creates one ticket only (`test_concurrent_same_key_same_payload_creates_one_ticket`).
- [x] Concurrent same key + different payload is rejected cleanly (`test_concurrent_same_key_different_payload_rejected_cleanly`).
- [x] Concurrent distinct keys → N distinct tickets, no interference (`test_concurrent_distinct_keys_all_create_tickets`).
- [ ] Limit race: not all overexposing sells succeed. *(deferred — needs Tenant B + limit config)*
- [ ] Session close vs sell race remains consistent. *(deferred)*
- [ ] Tenant A/B parallel sales do not leak data/config. *(deferred — needs Tenant B)*

> Concurrency suite (`tests/concurrency/test_pos_concurrency.py`, L3) surfaced and fixed two
> runtime bugs: (1) Hikari pool starvation — an audited sell holds 2 connections (own tx +
> audit REQUIRES_NEW), so a pool of 10 deadlocked at ~5 parallel sells; pool sized to 30.
> (2) `JpaIdempotencyStore.begin` made non-transactional so a concurrent duplicate-key insert
> rolls back in isolation instead of marking the tx rollback-only (UnexpectedRollbackException
> → 500); the race loser now re-reads and replays the winner.

## 11. Documentation

- [x] Document smoke/critical/concurrency/full commands (`testing/e2e/README.md` §4).
- [x] Document env vars (`testing/e2e/README.md` §3).
- [x] Document seed assumptions (`testing/e2e/README.md` §3).
- [ ] Document cleanup policy.
- [x] Document E2E is not performance/load testing (`testing/e2e/README.md` §9, design §2).

> `testing/e2e/README.md` (entry points, URLs, env, Keycloak + keycloak-init cache gotcha,
> edge-service verification, reliable API rebuild, Makefile, troubleshooting) + `AGENTS.md`
> router added for agents.
