# Design — commercial-network-v1

## Boundary model

```text
User -> authentication
Seller -> machann / seller business profile
Cashier -> UI/BFF flow
Outlet -> sales channel/location/institution/partner
Sales -> ticket creation and snapshots
LimitPolicy -> sale limits
Promotion -> commercial effects configuration
```

## Sell flow

```text
POS/Cashier request
  -> context resolves user
  -> operational context validates terminal/outlet/session
  -> sales resolves seller
  -> sales revalidates seller/assignment transactionally
  -> limitpolicy validates SELLER scope
  -> promotion evaluated
  -> charges calculated/waived
  -> ticket + lines + snapshots persisted
  -> events after commit
```

## Snapshot policy

Sales snapshots: seller_id, seller_assignment_id, sold_by_user_id, seller_commission_snapshot, promotion_decision_id, ticket_line pricing/promotion data, charge waiver data.

## Stale read protection

`ResolveSellerForOperationQuery` is not enough for critical write. Sell handler must re-check seller and assignment inside the transaction.

## Promotion policy

Promotion config can change after ticket sale. Settlement and payout must use sales snapshots only.

## Limit policy

Seller limit is not in `core.seller`. Use `LimitScopeType.SELLER`, `scopeRef = sellerId`.
