# Tasks — E2E Business Runtime V1

## 0. Harness

- [ ] Create `testing/e2e/pyproject.toml`.
- [ ] Create `pytest.ini` with markers.
- [ ] Create `.env.example`.
- [ ] Implement `config.py`.
- [ ] Implement `client.py`.
- [ ] Implement `auth.py`.
- [ ] Implement `api_response.py`.
- [ ] Implement `assertions.py`.
- [ ] Implement `data_factory.py`.
- [ ] Implement `scenario_world.py`.
- [ ] Implement `ticket_matrix.py`.
- [ ] Implement `concurrency.py`.

## 1. Scenario world

- [ ] Generate unique `run_id`.
- [ ] Provision Tenant A.
- [ ] Provision Tenant B with different config.
- [ ] Provision Tenant C minimal/incomplete.
- [ ] Create tenant admins.
- [ ] Create cashiers.
- [ ] Create outlets.
- [ ] Create terminals.
- [ ] Bind terminals.
- [ ] Open sessions.
- [ ] Configure A/B limits differently.
- [ ] Configure A/B promotions differently.
- [ ] Store IDs/tokens in fixture world.
- [ ] Add cleanup/deactivation policy.

## 2. Public runtime

- [ ] Public home loads without auth.
- [ ] Public home uses `public_home`.
- [ ] Public home contains no private provider source.
- [ ] Public draw results loads without auth.
- [ ] Public draw results uses `public_draw_results`.
- [ ] Public ticket check unknown code returns controlled response/no 500.
- [ ] Public ticket check sold ticket returns expected public status.

## 3. Auth/context

- [ ] Login super admin.
- [ ] Login tenant admin A/B.
- [ ] Login cashier A/B.
- [ ] Context/bootstrap returns expected role/tenant if endpoint exists.
- [ ] Tenant-scoped endpoints do not trust client tenant id.
- [ ] Platform context is distinct.

## 4. Dashboards and overviews

- [ ] Tenant admin dashboard uses `tenant_admin_dashboard`.
- [ ] Tenant admin dashboard includes KPI/readiness summary/summaries.
- [ ] Tenant admin dashboard excludes overview sections.
- [ ] Tenant overview returns sections/status/issues/routes.
- [ ] Tenant overview excludes KPI fields.
- [ ] Cashier web dashboard uses `cashier_dashboard`.
- [ ] Cashier web dashboard contains session/action/draws/recent tickets.
- [ ] Platform dashboard uses `platform_admin_dashboard`.
- [ ] Platform overview returns platform/config/ops/report sections.

## 5. Onboarding

- [ ] Tenant provisioning preview is read-only.
- [ ] Tenant provisioning preview returns domains/readiness.
- [ ] Tenant provisioning creates tenant if endpoint available.
- [ ] User onboarding creates tenant admin.
- [ ] User onboarding creates cashier.
- [ ] Outlet onboarding creates active outlet.
- [ ] Terminal onboarding creates terminal.
- [ ] Terminal binding prepares POS flow.

## 6. Cashier POS

- [ ] POS home works with active context.
- [ ] Missing context returns required step.
- [ ] Closed session returns required step.
- [ ] Open session.
- [ ] List available draws.
- [ ] Sell short ticket.
- [ ] Print ticket.
- [ ] Send ticket notification if configured.
- [ ] List recent tickets.
- [ ] Close session.
- [ ] Terminal locked blocks sell.
- [ ] Outlet blocked/sales disabled blocks sell.
- [ ] Session mismatch blocks sell.
- [ ] Cross-tenant terminal/outlet/session usage is blocked.

## 7. Ticket matrix and payout

- [ ] Implement SHORT_SINGLE_GAME_LOW_STAKE.
- [ ] Implement SHORT_SINGLE_GAME_HIGH_STAKE.
- [ ] Implement MEDIUM_MULTI_LINE_MIXED_STAKE.
- [ ] Implement MEDIUM_MULTI_GAME.
- [ ] Implement LONG_ALL_GAMES.
- [ ] Ensure selections differ.
- [ ] Ensure stakes differ.
- [ ] Load enabled games/pricing where possible.
- [ ] Assert line snapshots.
- [ ] Assert stake total.
- [ ] Assert money breakdown.
- [ ] Assert potential payout from snapshots.
- [ ] Assert print payload for long ticket.

## 8. Limits

- [ ] Below limit succeeds.
- [ ] Exactly at limit follows documented rule.
- [ ] Above line limit blocks.
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
- [ ] Same key + same payload returns same ticket/result.
- [ ] Same key + different payload returns payload mismatch.
- [ ] Concurrent same key + same payload creates one ticket only.
- [ ] Concurrent same key + different payload is rejected cleanly.
- [ ] Limit race: not all overexposing sells succeed.
- [ ] Session close vs sell race remains consistent.
- [ ] Tenant A/B parallel sales do not leak data/config.

## 11. Documentation

- [ ] Document smoke/critical/concurrency/full commands.
- [ ] Document env vars.
- [ ] Document seed assumptions.
- [ ] Document cleanup policy.
- [ ] Document E2E is not performance/load testing.
