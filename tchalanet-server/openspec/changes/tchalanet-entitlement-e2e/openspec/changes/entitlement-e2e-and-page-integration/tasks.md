# Tasks — Entitlement E2E & Integration

## P0 — Stabilize entitlement tests

- [ ] Verify `catalog.plan` seeds: STARTER, STANDARD, PRO, DEMO.
- [ ] Verify `features_json` and `limits_json` parse into objects, not text nodes.
- [ ] Verify `GET /tenant/me/capabilities` returns correct plan and capabilities.
- [ ] Verify inactive/suspended/canceled subscription returns inactive capabilities.
- [ ] Verify unknown/missing plan fails predictably.
- [ ] Verify cache uses `tenantId.value()` for both cache put and eviction.
- [ ] Verify subscription apply/change/suspend/resume/cancel evicts tenant snapshot.
- [ ] Verify multitenant snapshots do not leak between tenants.

## P1 — Onboarding integration

- [ ] Onboard tenant with default STARTER subscription.
- [ ] Onboard demo tenant with DEMO plan and sandbox/demo metadata.
- [ ] Verify onboarding E2E can create tenant, admin user, outlet, terminal, subscription.
- [ ] Verify newly onboarded tenant can load capabilities immediately.
- [ ] Verify tenant without subscription is blocked or receives safe empty capabilities.

## P1 — Quota E2E

- [ ] Terminal quota: max reached blocks create terminal.
- [ ] Outlet quota: max reached blocks create outlet.
- [ ] User quota: max reached blocks create user.
- [ ] Mobile device quota: max reached blocks device binding.
- [ ] Quota messages include `entitlement.limit_exceeded` or agreed error code.

## P1 — Feature E2E

- [ ] STANDARD cannot create basic promotion if feature absent.
- [ ] PRO can create basic promotion.
- [ ] STARTER/STANDARD cannot access offline sales/grants.
- [ ] PRO can access offline grant setup.
- [ ] Payout approval workflow requires `payout.approval.workflow`.

## P2 — Page generation after tests pass

- [ ] Public home/pricing page lists active plans.
- [ ] Public pricing shows curated feature list, not raw full matrix.
- [ ] Tenant/admin page model includes capability actions.
- [ ] POS/mobile page model hides unavailable seller actions.
- [ ] Page generation uses `EntitlementApi.getSnapshot` or dedicated BFF aggregator.

## P2 — Dashboard counts

- [ ] Tenant admin dashboard returns users active/max.
- [ ] Tenant admin dashboard returns outlets active/max.
- [ ] Tenant admin dashboard returns terminals active/max.
- [ ] Tenant admin dashboard returns mobile devices active/max.
- [ ] Dashboard counts are informational and do not replace enforcement.

## P2 — Service mapping

- [ ] Map terminal create to `terminal.licensing` + `limits.terminals.max`.
- [ ] Map outlet create to `outlet.management.multi` + `limits.outlets.max`.
- [ ] Map user create to `user.management.basic|standard` + `limits.users.max`.
- [ ] Map mobile device binding to `mobile.device.management` + `limits.mobile_devices.max`.
- [ ] Map promotion create to `promotion.rules.basic` + `limits.promotion_rules.max`.
- [ ] Map offline grant/sync/sell to `offline.sales.basic` / `offline.grant.basic`.
- [ ] Map payout approval endpoints to `payout.approval.workflow`.

## Review rule

- [ ] Add checklist entry to playbook / docs:
      “Before creating a new API or handler, decide whether entitlement applies.”
