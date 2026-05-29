# Design — Security Login, Terminal Binding, and Transaction Context

## Source Of Truth

The terminal domain model for this change is defined in:

```text
tchalanet-core/src/main/java/com/tchalanet/server/core/terminal/terminal_binding.md
```

This OpenSpec change constrains implementation and acceptance criteria. It must not drift from that near-code document. If a conflict appears, update the near-code document and this change together.

## Request Pipeline

The existing HTTP pipeline remains the single request-context pipeline:

```text
BearerTokenAuthenticationFilter
  -> UserBootstrapFilter
  -> TchContextFilter
      -> ApiScopeResolver
      -> TchRequestContextFactory
      -> TenantContextResolver
      -> ActorContextResolver
      -> OperationalContextResolver
      -> TchContextBinder.bind(finalCtx)
  -> Controller
  -> Method Security / TchPermissionEvaluator
  -> CommandBus / QueryBus
```

Do not add a separate operational-context filter. `TchContextFilter` extracts `Idempotency-Key` and operational headers, then delegates operational resolution.

## Operational Context

`OperationalContextResolver` classifies operational input early and validates trust only as far as the request pipeline can safely know.

Trusted sources:

- `SERVER_BOOTSTRAP`
- `SIGNED_DEVICE_BINDING`
- `ADMIN_SELECTION`

Untrusted sources:

- `CLIENT_CLAIM`
- `NONE`

Sensitive use cases must call `ctx.trustedOperationalContextRequired()` and then perform action-specific validation through core queries. Raw `X-Terminal-Id`, `X-Outlet-Id`, or `X-Sales-Session-Id` headers without a valid binding remain `CLIENT_CLAIM`.

## Terminal Domain Model

`core.terminal` owns terminal trust and validation. It uses separate aggregates because their lifecycles differ:

- `Terminal`: long-lived security surface, code, kind, surface, status, sync state, capabilities.
- `TerminalAssignment`: active/revoked user assignment lifecycle.
- `TerminalDeviceBinding`: active/revoked/expired device or virtual-phone binding.
- `TerminalActivationChallenge`: short-lived activation proof with attempt limits.

Aggregates reference `TerminalId` but do not cascade mutation across aggregate boundaries by default. Activation may coordinate challenge verification, binding creation, and terminal activation in one `@TchTx` transaction because it is a single domain operation, but it must use version/concurrency guards.

## Naming Decisions

Use the target domain names from `terminal_binding.md`:

- `TerminalKind`: `PHYSICAL`, `VIRTUAL`
- `TerminalSurface`: `POS`, `MOBILE`, `WEB`, `BACK_OFFICE`
- `TerminalStatus`: `REGISTERED`, `PENDING_ACTIVATION`, `ACTIVE`, `LOCKED`, `REVOKED`, `RETIRED`
- `TerminalSyncState`: `ONLINE`, `OFFLINE`, `SYNC_PENDING`, `SYNC_CONFLICT`
- `TerminalCapability`: `SELL_TICKET`, `SELL_PHONE`, `PAYOUT_CLAIM`, `PRINT_TICKET`, `REPRINT_TICKET`, `OFFLINE_SELL`, `OFFLINE_SYNC`, `SCAN_TICKET`
- `TerminalOperation`: `SELL_TICKET`, `SELL_PHONE`, `PAYOUT_CLAIM`, `PRINT_TICKET`, `REPRINT_TICKET`, `OFFLINE_GRANT`, `OFFLINE_SYNC`, `SCAN_TICKET`
- `TerminalBindingType`: `POS_DEVICE`, `MOBILE_APP`, `ADMIN_SELECTION`
- `TerminalBindingStatus`: `ACTIVE`, `REVOKED`, `EXPIRED`
- `TerminalChallengeType`: `POS_PAIRING`, `MOBILE_OTP`, `ADMIN_PAIRING_CODE`
- `TerminalChallengeChannel`: `QR`, `SMS`, `EMAIL`, `SLACK`, `TEST_CAPTURE`, `ADMIN_MANUAL`
- `TerminalChallengeStatus`: `PENDING`, `CONSUMED`, `EXPIRED`, `CANCELLED`

`TerminalType` is removed in favor of `TerminalKind + TerminalSurface`. `TerminalState` is legacy persistence/API vocabulary and must be migrated progressively to `TerminalStatus + TerminalSyncState`.

Core rule:

```text
User permission != Terminal capability != Outlet flag != Session validity
```

The four gates are independent. None replaces another.

## Capability And Entitlement

`platform.entitlement.api` is the effective capability source for plan features and limits. This change must not introduce `core.entitlement` and must not parse plan JSON inside `core.terminal` or `core.sales`.

Core terminal checks:

- phone sales feature: `PHONE_SALES_ENABLED` or the finalized entitlement key mapped to phone sales;
- physical terminal per-user limit;
- virtual phone terminal per-user limit;
- offline sales feature/limits where the operation is offline-sensitive.

Missing entitlement limits must stay distinguishable from zero. Do not use sentinels such as `-1`.

## Activation Flows

Physical POS activation:

1. Admin creates `PHYSICAL + POS` terminal.
2. Admin assigns terminal to seller/user and outlet.
3. Server creates one short-lived pending pairing challenge for `(terminal, user)`.
4. POS/client submits code and device proof.
5. Server verifies challenge, creates active binding, revokes previous active binding, and activates terminal.
6. Audit and domain/integration events are emitted after commit.

Virtual phone activation:

1. Tenant entitlement allows phone sales.
2. Admin creates or enables `VIRTUAL + MOBILE` terminal for seller/user.
3. Server creates activation challenge via approved channel (`SMS`, `EMAIL`, or admin manual code).
4. Server verifies challenge and creates signed virtual-phone binding.
5. Phone sales require terminal capability `SELL_PHONE`, user permission `ticket.sell.phone`, entitlement, and a trusted binding.

OTP is for activation, reset, change-device, or high-risk actions. It is not a login substitute and is not required for every login.

Challenge delivery is environment-policy driven:

- dev may use `SLACK` or `EMAIL` for `MOBILE_OTP` to avoid SMS cost;
- automated e2e uses `TEST_CAPTURE` so tests can retrieve the clear code from a test-only surface;
- live uses `SMS` for `MOBILE_OTP`, but only for activation, change-device, reset binding, suspected fraud, or explicit risk step-up.

A revoked refresh token forces re-authentication. It does not automatically send SMS unless no active compatible mobile binding can be validated or risk policy requires step-up.

## Validation Flow

Sensitive operations must validate in this order:

1. valid authentication and `TchRequestContext`;
2. Spring permission / `TchPermissionEvaluator`;
3. idempotency key when required;
4. trusted operational context;
5. terminal exists and belongs to tenant;
6. terminal is `ACTIVE`;
7. active terminal assignment exists for actor user;
8. active compatible binding exists and is not expired;
9. required terminal capability is present;
10. outlet/session compatibility is valid for the action;
11. seller is resolved from actor user, outlet, and session through `core.seller`;
12. seller and seller-outlet assignment are active and eligible for sale;
13. entitlement is valid for phone/offline or plan-limited operations;
14. business-domain rules such as draw cutoff, pricing, payout, or limit policy;
15. audit and events after commit.

Seller is never accepted from client headers or sale payload. The authenticated actor user plus trusted operational outlet/session determine the business seller identity. Ticket persistence snapshots `sellerId` and `sellerAssignmentId`.

## Persistence

Pre-go-live migration rules apply. Do not add a new Flyway migration without explicit confirmation; fold schema changes into the current canonical migration set when implementation begins.

Tenant-scoped tables must include `tenant_id`, tenant-aware indexes, RLS, and audit metadata where required. Capability storage should prefer a relational table over JSON for closed enum capabilities.

## Security Notes

- Store only hashes for activation codes and binding secrets.
- Never log activation code, binding secret, signed binding token, OTP, or raw device fingerprint.
- Binding tokens must bind tenant, terminal, binding id, user where applicable, expiry, and key material/fingerprint.
- Admin selections can be trusted only when created server-side and permission-checked.
- Client claims are useful hints, never transaction authority.
