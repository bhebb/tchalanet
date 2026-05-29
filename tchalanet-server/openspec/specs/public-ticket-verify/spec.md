# public-ticket-verify Specification

## Purpose
TBD - created by archiving change harden-critical-ticket-flows-v1. Update Purpose after archive.
## Requirements
### Requirement: Public verification must be a read-only BFF

`features.ticketverify` SHALL be a public read-only BFF.

It SHALL own:

- HTTP request mapping;
- public code normalization;
- rate limiting;
- no-store/noindex headers;
- response DTO mapping.

It SHALL NOT own:

- ticket status resolution;
- visibility policy;
- promotion truth;
- winning amount visibility;
- line truth.

#### Scenario: Verify public ticket

Given a valid public code and verification code
When the customer verifies the ticket
Then `features.ticketverify` asks `core.sales` through `VerifyTicketByPublicCodeQuery`
And returns a safe public response.

### Requirement: Verification must require public code and verification code

Public verification SHALL require both public code and verification code.

Wrong verification code and unknown ticket SHALL return the same not-found error.

#### Scenario: Wrong verification code

Given an existing ticket public code
When the customer submits the wrong verification code
Then the response is 404 `ticket.not_found`
And the response does not reveal whether the public code exists.

### Requirement: Public verification response must expose customer-safe status only

Public verification SHALL expose a customer-safe status derived by `core.sales`.

It SHALL NOT expose internal sale/result/settlement statuses directly.

#### Scenario: Pending result ticket

Given a valid ticket with no result yet
When customer verifies it
Then the response status is a customer-safe pending/result status
And internal status fields are not present.

### Requirement: Public verification must display canonical public proof

`TicketVerificationView` SHALL include enough canonical sales facts for public proof:

- public code;
- display code;
- customer status;
- total amount;
- winning amount only when visible and applicable;
- placed timestamp;
- draw info including draw channel label;
- outlet info if visible;
- line info including promotion display.

Line info SHALL include:

- line number;
- game label;
- bet type label;
- option label;
- display selection;
- stake;
- potential payout;
- promotional flag;
- promotion label.

#### Scenario: Verify ticket with Maryaj gratuit

Given a valid ticket with Maryaj gratuit
When customer verifies it
Then the public response displays the promotional line
And includes `Maryaj gratuit` or configured promotion label.

#### Scenario: Verify ticket with draw channel label

Given a sold ticket with draw channel label
When customer verifies it
Then the public response includes the same draw label used by receipt/print.

### Requirement: Verification display labels must come from sale snapshots

Verification projection SHALL prefer display labels snapshotted by sales.

Catalog lookup MAY be used only as fallback.

#### Scenario: Catalog label changes after sale

Given a ticket was sold with a game label snapshot
And the catalog game label changes later
When customer verifies the old ticket
Then the response displays the sale snapshot label, not the new catalog label.

### Requirement: Public verification must be no-store and noindex

The public verification endpoint SHALL return headers:

- `X-Robots-Tag: noindex, nofollow`;
- `Cache-Control: no-store`;
- `Pragma: no-cache`;
- `Expires: 0`.

#### Scenario: Successful verification response

Given a valid verification request
When the response is returned
Then the response contains no-store/noindex headers.

### Requirement: Public verification must be rate-limited

The public verification endpoint SHALL be rate-limited by client IP or trusted proxy-resolved IP.

For single-node V1, an in-memory limiter is acceptable.
For multi-instance production, rate limiting SHALL be migrated to Redis-backed or gateway-level distributed enforcement.

`X-Forwarded-For` SHALL be trusted only behind configured reverse proxy.

#### Scenario: Too many requests

Given rate limit is enabled
When a client exceeds the configured rate
Then the endpoint returns 429
And no ticket data is returned
And `Retry-After` is present if supported.

### Requirement: Public verification must apply visibility policy

`core.sales` SHALL apply `TicketVisibilityPolicy` before returning public verification details.

#### Scenario: Ticket is not publicly visible

Given a ticket is not publicly visible according to policy
When customer verifies it
Then the response is 404 `ticket.not_found`.

