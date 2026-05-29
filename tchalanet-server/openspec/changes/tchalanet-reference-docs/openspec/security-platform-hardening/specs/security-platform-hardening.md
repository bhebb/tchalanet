# OpenSpec — Security Platform Hardening

> Change: `security-platform-hardening`  
> Status: Proposed  
> Owner: Backend / Security / Architecture  
> Goal: implement the reference security posture for Web/Mobile/POS transaction safety.

## 1. Problem

Tchalanet’s business value depends on enabling tenants to sell through phone and POS. This increases risk if security is based only on a logged-in user.

Risks:

```text
- seller uses wrong terminal
- terminalId is spoofed in headers
- stolen phone/POS continues selling
- user sells from wrong outlet
- session mismatch
- tenant leak
- duplicate ticket from retry
- unauthorized phone sales
- forced ops without audit
```

## 2. Desired outcome

Every critical transaction is protected by:

```text
identity + tenant + permission + terminal + binding + outlet + session + trusted context + idempotency + audit + RLS
```

## 3. Scope

### In scope

```text
common.context updates
core.terminal secure terminal model
core.session validation integration
core.sales trusted context enforcement
platform.accesscontrol permission evaluation alignment
platform.communication OTP/email/SMS integration point
platform.audit sensitive action audit
platform.idempotence sell ticket hardening
batch/scheduler reference alignment
security test suite
```

### Out of scope

```text
full offline sales implementation
full rules engine
external SMS provider selection
outbox implementation
production fraud scoring
```

## 4. Requirements

### SEC-001 Context pipeline

The HTTP pipeline SHALL follow:

```text
BearerTokenAuthenticationFilter
-> UserBootstrapFilter
-> TchContextFilter
-> TchRequestContext bind
```

`UserBootstrapFilter` SHALL enrich actor only and SHALL NOT decide tenant.

### SEC-002 Operational context

Implement/align:

```java
OperationalRequestContext(
  TerminalId terminalId,
  OutletId outletId,
  SalesSessionId salesSessionId,
  OperationalContextSource source
)
```

Sources:

```text
SERVER_BOOTSTRAP
SIGNED_DEVICE_BINDING
ADMIN_SELECTION
CLIENT_CLAIM
NONE
```

Sensitive actions SHALL require trusted source.

### SEC-003 Terminal security model

Create or align `core.terminal`:

```text
Terminal
TerminalAssignment
TerminalDeviceBinding
TerminalActivationChallenge
TerminalPolicy
```

Terminal types:

```text
PHYSICAL_POS
VIRTUAL_PHONE
VIRTUAL_WEB
```

Statuses:

```text
PENDING_ACTIVATION
ACTIVE
LOCKED
REVOKED
EXPIRED
```

### SEC-004 Terminal activation

Support:

```text
physical POS pairing by code/QR
virtual phone activation by admin code/email OTP/SMS OTP optional
binding creation
binding revocation
```

### SEC-005 Permissions

Add permission keys:

```text
ticket.sell
ticket.sell.phone
terminal.create
terminal.assign
terminal.activate
terminal.revoke
terminal.lock
session.open
session.close
admin.pos_mode
```

Controllers SHALL declare requirements via method security.

### SEC-006 Sales enforcement

`SellTicketCommandHandler` or equivalent SHALL verify:

```text
trusted operational context
terminal valid for operation
outlet valid
session valid
permission valid
idempotency valid
```

### SEC-007 Idempotency

`POST /tenant/tickets` SHALL require `Idempotency-Key`.

Same key/hash => replay.  
Same key/different hash => 409.

### SEC-008 Audit

Audit required for:

```text
terminal create/assign/activate/revoke/lock
binding create/revoke
admin POS mode selection
sell ticket
void/cancel ticket
payout actions
offline grant/sync
forced ops
```

### SEC-009 RLS

All tenant-scoped tables added by this change SHALL enable RLS.

### SEC-010 Batch/scheduler

Schedulers SHALL be thin, context-bound, gate-aware, idempotent, and auditable when forced.

## 5. Data model suggestions

### terminal

```sql
create table terminal (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references tenant(id),
  outlet_id uuid null references outlet(id),
  code varchar(64) not null,
  label varchar(128) not null,
  type varchar(32) not null,
  status varchar(32) not null,
  capabilities jsonb not null default '[]'::jsonb,
  created_at timestamptz not null default now(),
  created_by uuid null,
  updated_at timestamptz not null default now(),
  updated_by uuid null,
  deleted_at timestamptz null,
  version bigint not null default 0,
  constraint uq_terminal_tenant_code unique (tenant_id, code)
);
```

### terminal_assignment

```sql
create table terminal_assignment (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references tenant(id),
  terminal_id uuid not null references terminal(id),
  user_id uuid not null,
  status varchar(32) not null,
  assigned_at timestamptz not null,
  revoked_at timestamptz null,
  created_at timestamptz not null default now(),
  created_by uuid null,
  version bigint not null default 0
);

create unique index uq_terminal_assignment_active
on terminal_assignment(tenant_id, terminal_id)
where status = 'ACTIVE';
```

### terminal_device_binding

```sql
create table terminal_device_binding (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references tenant(id),
  terminal_id uuid not null references terminal(id),
  binding_type varchar(32) not null,
  binding_key_hash varchar(256) not null,
  device_fingerprint_hash varchar(256) null,
  status varchar(32) not null,
  bound_at timestamptz not null,
  last_seen_at timestamptz null,
  revoked_at timestamptz null,
  created_at timestamptz not null default now(),
  created_by uuid null,
  version bigint not null default 0
);

create unique index uq_terminal_binding_active
on terminal_device_binding(tenant_id, terminal_id)
where status = 'ACTIVE';
```

### terminal_activation_challenge

```sql
create table terminal_activation_challenge (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references tenant(id),
  terminal_id uuid not null references terminal(id),
  user_id uuid null,
  challenge_type varchar(32) not null,
  channel varchar(32) not null,
  code_hash varchar(256) not null,
  expires_at timestamptz not null,
  attempt_count int not null default 0,
  status varchar(32) not null,
  created_at timestamptz not null default now(),
  created_by uuid null
);
```

All tables SHALL use RLS.

## 6. API surface

### Admin terminal management

```http
POST   /admin/terminals
GET    /admin/terminals
GET    /admin/terminals/{terminalId}
POST   /admin/terminals/{terminalId}/assign
POST   /admin/terminals/{terminalId}/lock
POST   /admin/terminals/{terminalId}/revoke
```

### POS pairing

```http
POST /tenant/terminals/{terminalId}/pairing-challenges
POST /tenant/terminals/{terminalId}/pair
GET  /tenant/me/operational-context
```

### Virtual phone terminal

```http
POST /tenant/virtual-terminals/phone/activation-challenges
POST /tenant/virtual-terminals/phone/activate
```

### Admin POS mode

```http
POST   /tenant/me/operational-context/select
GET    /tenant/me/operational-context
DELETE /tenant/me/operational-context
```

## 7. Commands / Queries

Commands:

```text
CreateTerminalCommand
AssignTerminalToUserCommand
LockTerminalCommand
RevokeTerminalCommand
CreateTerminalActivationChallengeCommand
VerifyTerminalActivationChallengeCommand
BindPhysicalTerminalDeviceCommand
ActivateVirtualPhoneTerminalCommand
SelectOperationalContextCommand
ClearOperationalContextCommand
```

Queries:

```text
ResolveOperationalContextQuery
ValidateTerminalForOperationQuery
GetCurrentOperationalContextQuery
ListTerminalsQuery
```

## 8. Test requirements

Must implement tests from:

```text
docs/reference/TRANSACTION_SECURITY_TEST_PLAN.md
```

Minimum P0 tests:

```text
SELL-001 happy path POS
SELL-002 missing idempotency
SELL-003 replay same key
SELL-004 payload mismatch
SELL-005 client claim not trusted
POS-002 wrong user tries terminal
RLS-001 cross tenant read blocked
RLS-002 cross tenant write blocked
CONC-001 same idempotency key parallel
```

## 9. Rollout phases

### Phase 1 — Docs and architecture tests

```text
Add reference docs.
Add ArchUnit checks for protected controllers if missing.
Document permission keys.
```

### Phase 2 — Terminal persistence and API

```text
Add tables, entities, adapters, commands/queries.
Add admin terminal management.
```

### Phase 3 — Operational context binding

```text
Add binding validation.
Resolve source SIGNED_DEVICE_BINDING.
Enforce trusted context in sales.
```

### Phase 4 — POS pairing

```text
Challenge + pair flow.
Device binding lifecycle.
Flutter integration.
```

### Phase 5 — Phone sales

```text
VIRTUAL_PHONE terminal.
Entitlement check PHONE_SALES_ENABLED.
OTP/code activation.
Permission ticket.sell.phone.
```

### Phase 6 — Hardening

```text
Audit dashboards.
Risk events.
Revocation tests.
Batch/scheduler alignment.
```
