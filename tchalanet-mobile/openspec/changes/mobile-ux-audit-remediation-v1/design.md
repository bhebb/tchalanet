# Design — Mobile UX Audit Remediation

## Interaction and feedback

- Use Material interaction feedback for game and bet chips, including ripple,
  pressed state, semantics, and haptic feedback where supported.
- Disable `Verifye` when the sale input is empty or invalid.
- Replace fixed-width selection fields with responsive constraints so long
  selections and accessibility text do not clip on narrow Sunmi screens.
- Add PIN visibility toggles and distinguish invalid credentials, blocked
  account, and network failure.

## Sale completion

- Keep the ticket code copy action fully localized.
- `Nouvo tikè` returns directly to `/sell` with the last valid draw selected
  when possible.
- Print behavior remains owned by `pos-fast-sale-direct-print-v1`; automatic
  POS printing must use `PrinterService` and must never invoke
  `Printing.layoutPdf`.

## Data states and performance

- Isolate dashboard countdown rebuilds from the complete dashboard tree.
- Show explicit empty/error content for latest ticket, history, reports, and
  result lists.
- Add pagination or load-more behavior for history and report tickets instead
  of silently truncating at 50.
- Add date selection for older ticket history and debounce live ticket search.
- Result filters should use a compact bottom sheet or responsive filter layout;
  the active date range must be visible.
- Report totals must either follow the active draw filter or be labeled as
  whole-day totals.

## Ticket verification and details

- Do not show a QR scanner placeholder as if scanning were supported.
- Disable verification with an empty code.
- Localize ticket dates and times.
- Expose cancellation only when the backend and ticket status permit it.

## Settings and account surfaces

- Combine or clearly connect read-only profile information with settings.
- Show saving, success, and error feedback for every settings update.
- Keep printer settings and diagnostics in the print change, while this change
  standardizes feedback and localization around that screen.
- Add dark theme support through existing design tokens.
- Extract the duplicated logout confirmation into a shared action.
- Forbidden/blocked screens show a reason when provided and a configured admin
  contact action when available.

## Reporting and sharing

- Preserve server-side pagination and filters in the client model.
- Add a report share/export action using a deliberate text/share or PDF
  contract; it must not silently open the system PDF UI during a POS sale.
