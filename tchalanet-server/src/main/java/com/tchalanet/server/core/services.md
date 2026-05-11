# Tchalanet — MVP Core Architecture Notes

## Global Rules

```text
core = source de vérité métier
feature = orchestration UX/BFF
common = primitives techniques uniquement
```

```text
core domains ne dépendent jamais des features
features peuvent appeler core via CommandBus/QueryBus
```

---

# Core Domains

## core.outlet

### Responsibility

Operational point-of-sale context.

### Owns

- outlet configuration
- sales_blocked
- day_closed
- receipt config values
- outlet users
- outlet terminal associations

### Does NOT own

- selling logic
- receipt rendering
- payout logic

### Main models

```text
Outlet
OutletConfig
OutletSalesCapability
OutletOperationalContext
```

### Commands

```text
CreateOutletCommand
UpdateOutletConfigCommand
BlockOutletSalesCommand
UnblockOutletSalesCommand
AssignUserToOutletCommand
```

### Queries

```text
GetOutletByIdQuery
ListOutletsQuery
GetOutletOperationalContextQuery
GetOutletSalesCapabilityQuery
```

### Exposed Ports

```text
OutletOperationalContextPort
OutletSalesCapabilityPort
```

---

## core.terminal

### Responsibility

Physical/virtual runtime device management.

### Owns

- terminal registration
- active terminal
- terminal state
- heartbeat
- sync state
- outlet assignment
- user assignment

### Main models

```text
Terminal
TerminalState
TerminalSyncState
TerminalAssignment
```

### Commands

```text
RegisterTerminalCommand
AssignTerminalToOutletCommand
AssignTerminalToUserCommand
LockTerminalCommand
UnlockTerminalCommand
SendTerminalHeartbeatCommand
```

### Queries

```text
GetTerminalByIdQuery
ListTerminalsQuery
GetActiveTerminalForUserQuery
GetTerminalStatusQuery
```

### Exposed Ports

```text
TerminalStatusPort
ActiveTerminalPort
```

---

## core.session

### Responsibility

Seller-scoped sales sessions.

### Rules

```text
one seller = max one OPEN session
tickets remain attached after close
payout allowed after selling session closes
terminal is contextual, not owner
```

### Main models

```text
SalesSession
SalesSessionStatus
SalesSessionTotals
SalesSessionSummaryView
```

### Commands

```text
OpenSalesSessionCommand
CloseSalesSessionCommand
AutoOpenSalesSessionsCommand
AutoCloseSalesSessionsCommand
```

### Queries

```text
GetCurrentSalesSessionQuery
GetSalesSessionSummaryQuery
ListSalesSessionsQuery
```

### Exposed Ports

```text
SalesSessionLookupPort
CurrentSalesSessionPort
```

### Controllers

```text
SalesSessionTenantController
SalesSessionAdminController
```

### Important cleanup

Remove:

```text
SalesSessionTotalsController
```

Totals belong inside:

```text
SalesSessionSummaryView
```

---

## core.sales

### Responsibility

Ticket selling and lifecycle.

### Owns

- Ticket aggregate
- TicketLine
- sale/result/settlement lifecycle
- official ticket data

### Does NOT own

```text
PDF
ESC/POS
QR image rendering
layout/templates
email/sms delivery
```

### Main models

```text
Ticket
TicketLine
TicketReceiptDataView
TicketListItemView
TicketDetailView
PublicTicketVerificationRecord
TicketPayoutEligibilityRecord
```

### Commands

```text
SellTicketCommand
CancelTicketCommand
ApprovePendingSaleCommand
RejectPendingSaleCommand
RecordDrawTicketsResultCommand
SettleTicketsForDrawCommand
```

### Queries

```text
GetTicketByIdQuery
ListTicketsQuery
GetTicketReceiptDataQuery
GetPublicTicketVerificationRecordQuery
GetTicketPayoutEligibilityQuery
```

### Exposed Ports

```text
TicketReceiptDataPort
PublicTicketVerificationPort
TicketPayoutEligibilityPort
```

---

## core.payout

### Responsibility

Winner payment lifecycle.

### Rules

```text
selling_session_id remains attached
paying session may differ
double payout forbidden
```

### Main models

```text
Payout
PayoutStatus
PayoutEligibilityView
```

### Commands

```text
RegisterPayoutCommand
ApprovePayoutCommand
RejectPayoutCommand
ExecutePayoutCommand
```

### Queries

```text
GetPayoutByIdQuery
ListPayoutsQuery
GetPayoutEligibilityQuery
```

---

# Policies Architecture

## core.limitpolicy

### Responsibility

Evaluate operational limits.

### Returns

```text
ALLOW
WARN
REQUIRE_APPROVAL
BLOCK
```

### Owns

```text
LimitDefinition
LimitAssignment
LimitDecision
LimitBreach
```

### Queries

```text
EvaluateSaleLimitsQuery
EvaluatePayoutLimitsQuery
GetEffectiveLimitPolicyQuery
```

---

## core.autonomy

### Responsibility

Determine approval autonomy.

### Owns

```text
AutonomyPolicyRule
AutonomyLevel
ApprovalRole
ApprovalRequirement
```

### Queries

```text
ResolveApprovalRequirementQuery
GetEffectiveAutonomyPolicyQuery
```

---

## Policy Evaluation

Use one orchestration service:

```text
PolicyEvaluationService
```

Combines:

```text
limitpolicy + autonomy
```

Returns:

```text
ALLOW
WARN
REQUIRE_APPROVAL
BLOCK
```

sales/payout consume the result only.

---

# Features

## features.cashier

### Responsibility

Seller runtime UX.

### Owns

```text
sell dashboard
recent tickets
quick sell flow
session actions
```

### Calls

```text
core.sales
core.session
core.terminal
```

---

## features.receipt

### Responsibility

Document exposure.

### Owns

```text
receipt endpoints
pdf/html/escpos/image rendering
layout/templates
```

### Endpoints

```text
GET /tenant/receipts/tickets/{ticketId}?format=PDF
GET /tenant/receipts/tickets/{ticketId}?format=ESCPOS
```

### Flow

```text
ReceiptController
 -> QueryBus(GetTicketReceiptDataQuery)
 -> ReceiptGenerationService
 -> common.print render
```

---

## features.delivery

### Responsibility

Send/distribute content.

### Owns

```text
email
sms
whatsapp
attachments
delivery orchestration
```

### Endpoint

```text
POST /tenant/delivery
```

### Flow

```text
DeliveryController
 -> optional receipt generation
 -> core.notification
 -> edge-service
```

---

## features.ticketverify

### Responsibility

Public ticket verification.

### Uses

```text
GetPublicTicketVerificationRecordQuery
```

### Does

```text
masking
visibility rules
public statuses
```

---

# Notification

## core.notification

### Responsibility

Notification workflow tracking.

### Owns

```text
delivery status
retry state
audit
notification history
```

### Does NOT own

```text
provider transport
```

---

# edge-service

### Responsibility

Real external transport.

### Owns

```text
Twilio
Brevo
WhatsApp provider
Slack
Email transport
```

---

# Common

## common.print

### Responsibility

Low-level rendering primitives only.

### Owns

```text
PdfEngine
QrRenderer
EscposRenderer
HtmlTemplateEngine
```

### Must NOT know

```text
Ticket
Payout
Draw
Sales
```

---

# Read Model Rule

```text
writes = aggregates/JPA/domain
reads = JDBC/projection/view models
```

Use projections for:

```text
ticket lists
receipt data
dashboard/search
summary screens
```

---

# Controller Rule

Each core domain uses:

```text
AdminController
TenantController
```

Example:

```text
OutletAdminController
TerminalTenantController
SalesSessionAdminController
TicketTenantController
PayoutAdminController
```

Rule:

```text
admin = management/supervision
tenant = operational runtime
```
