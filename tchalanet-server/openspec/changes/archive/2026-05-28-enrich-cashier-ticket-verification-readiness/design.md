# Design — Cashier verification and readiness V1

## Ownership

`features.cashier` is a BFF/orchestration layer. It may compose queries from Sales, Payout, Terminal, Outlet, Session, and platform notification/readiness services, but it must not own financial truth.

## Endpoint: verify scanned ticket

```http
POST /tenant/cashier/tickets/verify
```

Request:

```java
public record CashierVerifyTicketRequest(
    @NotBlank String scannedValue
) {}
```

Response:

```java
public record CashierTicketVerificationResponse(
    String status,
    String severity,
    String titleKey,
    String messageKey,
    Map<String, Object> params,
    List<CashierAction> availableActions
) {}
```

`scannedValue` may be:

```text
TCH-8F4K-29PL
https://tchalanet.com/v/TCH-8F4K-29PL
https://tchalanet.com/public/tickets/TCH-8F4K-29PL
```

The backend normalizes to `publicTicketCode`.

## Endpoint: readiness

```http
GET /tenant/cashier/readiness
```

Response shape:

```java
public record CashierReadinessResponse(
    boolean ready,
    CashierAttentionLevel attentionLevel,
    List<CashierBadge> badges,
    List<CashierNotification> notifications,
    List<CashierReadinessBlocker> blockers
) {}
```

V1 attention levels:

```java
public enum CashierAttentionLevel {
    NONE,
    BADGE,
    CARD,
    ACTION_REQUIRED,
    BLOCKED
}
```

For V1, old unpaid payout claims should normally be `BADGE` or `CARD`, not blocking.

## V1 notification rule

Show a notification only when:

```text
there are PayoutClaim OPEN or BLOCKED
AND their draw is older than the current/recently closed draw or business day
```

Message keys:

```text
pos.notification.previous_unpaid_payouts.title
pos.notification.previous_unpaid_payouts.message
pos.notification.previous_unpaid_payouts.action
```

## Available actions

```text
VIEW_TICKET
REPRINT_TICKET
RESEND_TICKET
EXECUTE_PAYOUT
VIEW_PAYOUTS_TO_PROCESS
CONTACT_ADMIN
NONE
```

Actions are suggestions. They do not bypass backend authorization or operational context validation.

## Public vs POS verification

The same public ticket code can be used in both places, but the responses differ:

- Public endpoint returns minimal public-safe status.
- Cashier endpoint requires auth/context and may return payout action metadata.
- Execute payout always requires authenticated POS/admin context.

## V1 simplification

No acknowledgement table, no "all paid" assertion, no heavy review workflow.
