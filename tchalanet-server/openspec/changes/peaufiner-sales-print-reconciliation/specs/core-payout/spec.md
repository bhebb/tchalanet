# Spec — core.payout read support for reconciliation

## Requirement: Expose payout claims by draw

`core.payout` MUST expose claims for a draw.

### Query: ListPayoutClaimsForDrawQuery

Input:

```java
public record ListPayoutClaimsForDrawQuery(
    DrawId drawId
) implements Query<List<PayoutClaimForDrawRow>> {}
```

Output:

```java
public record PayoutClaimForDrawRow(
    PayoutClaimId claimId,
    TicketId ticketId,
    String ticketCode,
    String publicCode,
    String displayCode,
    BigDecimal claimAmount,
    CurrencyCode currency,
    PayoutClaimStatus status
) {}
```

## Requirement: Expose payout payments by draw

### Query: ListPayoutPaymentsForDrawQuery

Input:

```java
public record ListPayoutPaymentsForDrawQuery(
    DrawId drawId
) implements Query<List<PayoutPaymentForDrawRow>> {}
```

Output:

```java
public record PayoutPaymentForDrawRow(
    PayoutPaymentId paymentId,
    PayoutClaimId claimId,
    TicketId ticketId,
    String ticketCode,
    String publicCode,
    String displayCode,
    BigDecimal paidAmount,
    CurrencyCode currency,
    PayoutPaymentStatus status,
    Instant paidAt
) {}
```

## Requirement: Expose payout summary by draw

### Query: GetPayoutSummaryForDrawQuery

Input:

```java
public record GetPayoutSummaryForDrawQuery(
    DrawId drawId
) implements Query<PayoutSummaryForDrawRow> {}
```

Output:

```java
public record PayoutSummaryForDrawRow(
    DrawId drawId,
    long claimCount,
    BigDecimal claimTotal,
    BigDecimal paidTotal,
    long postedPaymentCount
) {}
```
