# Tasks

## 1. Sell preconditions

- [ ] Session must be OPEN.
- [ ] Session belongs to current seller/user.
- [ ] Outlet allows sales.
- [ ] Terminal is valid/unlocked if terminal is used.
- [ ] Draw is OPEN and cutoff not passed.
- [ ] Tenant game is active.
- [ ] Pricing odds active.
- [ ] Limit/autonomy decision evaluated.

## 2. Commands

- [ ] `SellTicketCommand`
- [ ] `CancelTicketCommand`
- [ ] `ReprintTicketCommand` or query-based print endpoint
- [ ] `RecordDrawTicketsResultCommand`
- [ ] `SettleTicketsForDrawCommand` if not already draw-side

## 3. Queries / read endpoints

Do not rely only on cashier overview.

- [ ] `GetTicketByIdQuery`
- [ ] `GetTicketByCodeQuery`
- [ ] `GetTicketByPublicCodeQuery`
- [ ] `ListTicketsQuery`
- [ ] `ListRecentTicketsForSessionQuery`
- [ ] `ListRecentTicketsForCurrentUserQuery`
- [ ] `GetTicketPrintViewQuery`
- [ ] `GetSalesSummaryQuery`
- [ ] `GetSessionSalesSummaryQuery`
- [ ] `GetOutletSalesSummaryQuery`

## 4. Tenant HTTP endpoints

- [ ] `POST /tenant/tickets`
  - idempotency required
- [ ] `GET /tenant/tickets`
  - paginated
- [ ] `GET /tenant/tickets/{ticketId}`
- [ ] `GET /tenant/tickets/recent`
- [ ] `GET /tenant/tickets/{ticketId}/print/pdf`
- [ ] `GET /tenant/tickets/{ticketId}/print/escpos`
- [ ] `POST /tenant/tickets/{ticketId}/cancel` if allowed for seller/admin
- [ ] File endpoints may return Resource/bytes, not ApiResponse.

## 5. Admin/Ops endpoints

- [ ] `GET /admin/tickets`
- [ ] `GET /admin/tickets/{ticketId}`
- [ ] `POST /admin/tickets/{ticketId}/cancel`
- [ ] `GET /admin/sales/summary`
- [ ] `GET /admin/sales/by-outlet`
- [ ] `GET /admin/sales/by-session`
- [ ] `GET /admin/sales/by-user`

## 6. Ticket codes

- [ ] `ticket_code` internal unique per tenant.
- [ ] `public_code` NOT NULL and globally unique.
- [ ] Retry collision max 3.
- [ ] Use centralized code generators.
- [ ] Do not call `UUID.randomUUID()` directly outside generator.

## 7. Money

- [ ] Prefer cents fields in DB/domain.
- [ ] If API uses decimal, map at boundary.
- [ ] Avoid BigDecimal money drift inside aggregate where cents is easier.

## 8. Events

- [ ] Publish `TicketSoldEvent` after commit.
- [ ] Publish `TicketCancelledEvent` after commit.
- [ ] Publish result/settlement events after commit if needed.
- [ ] Consumers idempotent.

## 9. Tests

- [ ] Sell requires open seller session.
- [ ] Sell fails if outlet blocked.
- [ ] Sell fails if terminal locked.
- [ ] Sell creates ticket + lines with session.
- [ ] Public code is not null.
- [ ] Recent tickets query works after session close.
