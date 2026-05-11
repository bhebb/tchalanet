# Tasks

## 1. Package structure

- [ ] `features.cashier.dashboard`
- [ ] `features.cashier.sell`
- [ ] `features.cashier.session`
- [ ] `features.cashier.receipt`
- [ ] Keep packages only where rule-of-3 justifies it.

## 2. Dashboard / context endpoints

- [ ] `GET /tenant/cashier/dashboard`
- [ ] `GET /tenant/cashier/sell-context`
- [ ] Aggregates:
  - current seller
  - current sales session
  - active terminal
  - outlet operational context
  - open draws
  - enabled games
  - recent tickets
  - policy warnings if any
  - sync/offline status

## 3. Session UX endpoints

- [ ] `GET /tenant/cashier/session/current`
- [ ] `POST /tenant/cashier/session/open`
  - delegates to core.session
- [ ] `POST /tenant/cashier/session/{sessionId}/close`
  - delegates to core.session
- [ ] May simply proxy core views with UI-friendly response.

## 4. Sell UX endpoint

- [ ] `POST /tenant/cashier/sell`
  - delegates to `core.sales SellTicketCommand`
  - passes Idempotency-Key requirement through
  - returns receipt/sell response view
- [ ] No limit/payout/cutoff calculation in feature.

## 5. Ticket/receipt endpoints

- [ ] `GET /tenant/cashier/tickets/recent`
- [ ] `GET /tenant/cashier/tickets/{ticketId}`
- [ ] `POST /tenant/cashier/tickets/{ticketId}/reprint`
  - delegates to core sales print/reprint query/command
- [ ] File rendering remains in core.sales if already there.

## 6. Offline/sync UX

- [ ] Include terminal sync status.
- [ ] Include pending sync count if available.
- [ ] Do not implement conflict resolution here yet.

## 7. Tests

- [ ] Dashboard aggregates from fake queries.
- [ ] Sell delegates once to command bus.
- [ ] Feature does not contain business validation.
