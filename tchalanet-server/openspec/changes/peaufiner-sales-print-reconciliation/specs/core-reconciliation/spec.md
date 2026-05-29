# Spec — core.reconciliation

## Requirement: Own reconciliation runs and anomalies

`core.reconciliation` MUST own reconciliation run and anomaly persistence.

### Tables

```text
reconciliation_run
reconciliation_anomaly
```

`core.reconciliation` MUST NOT update sales, payout, drawresult, or ledger state during normal reconciliation.

## Requirement: Daily reconciliation is batch-driven

Daily reconciliation MUST run as a batch job.

### Scenario: scheduled daily run

Given a tenant is active
And tenant-local time reaches configured midnight window
When the scheduler ticks
Then it launches daily reconciliation for the previous tenant-local business date
And the scheduler remains thin
And business verification runs inside the batch/job services.

### Scenario: forced ops run

Given a SUPER_ADMIN provides tenant id, business date, and reason
When `POST /platform/ops/reconciliation/daily-runs` is called
Then a forced daily reconciliation run is launched
And the reason is required
And the action is audited.

## Requirement: Reconciliation compares domain read models

Reconciliation MUST read data using QueryBus from domain public query APIs.

V1 sources:

- `core.drawresult`
- `core.sales`
- `core.payout`

Out of V1:

- `core.ledger`
- `core.settlement` because it does not exist yet

### Scenario: draw result verification

Given a resulted draw exists for business date
When reconciliation processes it
Then it loads official draw result metadata from `core.drawresult`
And expected/actual sales outcomes from `core.sales`
And payout claims/payments from `core.payout`.

## Requirement: Summary checks

Reconciliation MUST perform summary checks per draw.

### Anomalies

```text
DRAW_WINNER_COUNT_MISMATCH
DRAW_PAYOUT_TOTAL_MISMATCH
DRAW_PAID_TOTAL_EXCEEDS_EXPECTED
```

### Scenario: winner count mismatch

Given expected winner count is 12
And actual sales winner count is 11
When reconciliation compares summaries
Then it creates `DRAW_WINNER_COUNT_MISMATCH`.

## Requirement: Ticket-level checks

Reconciliation MUST perform detail checks to identify exact tickets even when summary counts match.

### Anomalies

```text
TICKET_RESULT_STATUS_MISSING_AFTER_DRAW_RESULT
EXPECTED_WINNER_NOT_RESULTED
FALSE_WINNER_RESULTED
SALES_OUTCOME_AMOUNT_MISMATCH
```

### Scenario: ticket missing result status

Given a draw result has been applied
And a ticket is accepted and not cancelled/voided/rejected
And ticket result status is PENDING or missing
When reconciliation runs
Then it creates `TICKET_RESULT_STATUS_MISSING_AFTER_DRAW_RESULT`.

### Scenario: expected winner is not marked winner

Given expected outcome says a ticket should win
And actual sales result status is not WON
When reconciliation compares outcomes
Then it creates `EXPECTED_WINNER_NOT_RESULTED`.

### Scenario: false winner

Given expected outcome says a ticket should not win
And actual sales result status is WON
When reconciliation compares outcomes
Then it creates `FALSE_WINNER_RESULTED` with CRITICAL severity.

## Requirement: Payout claim checks

Reconciliation MUST compare expected sales outcomes with payout claims.

### Anomalies

```text
WINNER_WITHOUT_PAYOUT_CLAIM
CLAIM_FOR_NON_WINNING_TICKET
PAYOUT_CLAIM_AMOUNT_MISMATCH
```

### Scenario: winner without claim

Given expected outcome says a ticket should win
And expected payout amount is greater than zero
And no payout claim exists for the ticket
When reconciliation compares payouts
Then it creates `WINNER_WITHOUT_PAYOUT_CLAIM`.

### Scenario: claim for non-winning ticket

Given a payout claim exists for a ticket
And expected outcome says the ticket should not win
When reconciliation compares payouts
Then it creates `CLAIM_FOR_NON_WINNING_TICKET` with CRITICAL severity.

## Requirement: Payout payment checks

Reconciliation MUST compare payout claims and payments.

### Anomalies

```text
PAYMENT_EXCEEDS_CLAIM_AMOUNT
PAYMENT_CLAIM_STATUS_MISMATCH
PAID_NON_WINNING_TICKET
```

### Scenario: payment exceeds claim

Given posted payments total exceeds claim amount
When reconciliation compares claim and payments
Then it creates `PAYMENT_EXCEEDS_CLAIM_AMOUNT`.

### Scenario: payment for non-winning ticket

Given a posted payment exists
And expected sales outcome says the ticket should not win
When reconciliation runs
Then it creates `PAID_NON_WINNING_TICKET` with CRITICAL severity.

## Requirement: Anomalies are idempotent

Anomalies MUST be deduplicated by a stable fingerprint.

### Scenario: anomaly rediscovered

Given an OPEN anomaly already exists with the same fingerprint
When a later run detects it again
Then no duplicate anomaly is created
And `lastSeenAt` and run association are updated.

## Requirement: CSV report is generated from persisted anomalies

The notification CSV MUST be generated from `reconciliation_anomaly` rows, not from recalculating domain facts.

### CSV columns V1

```csv
run_id,tenant_id,tenant_code,business_date,severity,anomaly_type,anomaly_status,draw_id,draw_channel_id,draw_result_id,ticket_id,ticket_code,public_code,display_code,payout_claim_id,payout_payment_id,expected_status,actual_status,expected_amount,actual_amount,currency,message,fingerprint,created_at
```

## Requirement: No silent correction

Reconciliation MUST NOT mutate sales/payout/drawresult/ledger states during normal run.

### Scenario: winner missing status

Given reconciliation detects a ticket that should win but is not marked winner
When the anomaly is persisted
Then reconciliation does not update the ticket
And the anomaly remains OPEN until reviewed or repaired through explicit audited action.
