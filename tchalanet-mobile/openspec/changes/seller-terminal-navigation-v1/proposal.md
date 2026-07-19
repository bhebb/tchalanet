# Change: Seller terminal navigation V1

The seller terminal currently exposes an inert header avatar, inconsistent navigation components,
and no direct path to seller account actions. This change establishes one POS navigation model.

The five primary destinations are:

1. Home (`/pos`) for open draws and starting a sale.
2. Tickets (`/pos/history`) for ticket search, detail, scan, and reprint.
3. Results (`/pos/results`) for searching published draw results.
4. Reports (`/pos/reports`) for seller sales, ticket, and commission reporting.
5. Profile (`/pos/profile`) for PIN, language, printer, support, version, and logout.

The notification bell remains a header action. The avatar opens Profile. The POS does not regain a
drawer or an unbounded menu.

## Scope

- One localized five-destination navigation bar.
- Avatar-to-profile navigation.
- Rename the seller statistics surface to Reports.
- Profile-owned secondary actions, including a confirmed logout.
- A real Results route only when its typed backend query contract is available; no placeholder screen.

## Out of scope

- Backend result-search BFF implementation.
- Printer configuration protocol and device driver work.
- Public password reset.
