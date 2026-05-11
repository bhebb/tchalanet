# Convention — Offline Sync, Offline Claims & Offline Operations

## Principle

```text
OfflineSaleSubmission != Ticket
OfflineCodeReservation != Ticket
TicketPlacedEvent != OfflineBatchReceivedEvent
```

## Domain boundaries

```text
Offlinesync records technical claims.
Sales creates official tickets.
Session/Ledger/Payout react only to TicketPlacedEvent.
```

## Technical reject

Produced by Offlinesync:

```text
INVALID_SIGNATURE
PAYLOAD_HASH_MISMATCH
UNKNOWN_GRANT
GRANT_REVOKED
GRANT_EXPIRED
DUPLICATE_CODE
SEQUENCE_GAP
TERMINAL_MISMATCH
SELLER_MISMATCH
SESSION_MISMATCH
```

## Sales reject/review

Produced by Sales:

```text
DRAW_CUTOFF_PASSED
DRAW_ALREADY_RESULTED
SYNC_AFTER_RESULT_KNOWN
LIMIT_POLICY_BLOCKED
PRICING_MISMATCH
MONEY_BREAKDOWN_INVALID
SESSION_FINALIZED
DEVICE_TIME_UNTRUSTED
```

## FINALIZED session

If offline sync references a FINALIZED session:

```text
never auto-accept
never mutate session totals silently
mark SALES_REVIEW_REQUIRED or SALES_QUARANTINED
risk flag FINALIZED_SESSION
admin decision required
```

V1:

```text
status = SALES_REVIEW_REQUIRED
salesRejectReason = SESSION_FINALIZED
riskFlags += FINALIZED_SESSION
```

## Result known

If result is already known:

```text
never auto-accept
review or reject according to policy
```

## Batch receive

Receive for audit even if:

```text
outlet dayClosed
outlet salesBlocked
session CLOSED
```

But do not auto-accept unless Sales gates pass safely.
