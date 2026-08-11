# Tasks

## 1 — Login and sale entry

- [ ] Add PIN visibility toggle.
- [ ] Map invalid credentials, blocked account, and network failures to distinct localized states.
- [ ] Add Material ripple/semantics/haptic feedback to game and bet chips.
- [ ] Disable `Verifye` for empty/invalid input.
- [ ] Replace fixed selection widths with responsive layout constraints.

## 2 — Dashboard and completion

- [ ] Isolate countdown rebuilds from dashboard cards.
- [ ] Add explicit latest-ticket empty/error states.
- [ ] Localize every completion action label, including copy code.
- [ ] Route `Nouvo tikè` directly to `/sell` with a valid draw context.

## 3 — History, verification, and details

- [ ] Add history pagination/load-more and date picker.
- [ ] Add debounced live ticket search.
- [ ] Remove or replace the QR placeholder until scanning is implemented.
- [ ] Disable empty-code verification.
- [ ] Localize ticket dates/times.
- [ ] Show cancellation only when allowed by ticket status and backend capability.

## 4 — Results and reports

- [ ] Compact result filters and show the active date range.
- [ ] Make report totals consistent with draw filters or label whole-day totals.
- [ ] Add report pagination beyond 50 tickets.
- [ ] Add explicit report share/export behavior with tests.

## 5 — Settings and cross-cutting UX

- [ ] Add saving/success/error feedback for settings updates.
- [ ] Connect profile and settings without a dead-end read-only screen.
- [ ] Add dark theme through existing tokens.
- [ ] Extract shared logout confirmation.
- [ ] Add blocked/forbidden reason and admin contact when supplied by backend.
- [ ] Add localization and accessibility coverage for all changed controls.

## 6 — Validation

- [ ] Add focused widget tests for loading, empty, error, disabled, saving, and success states.
- [ ] Add navigation tests for completion to `/sell` and terminal-context preservation.
- [ ] Add pagination/search/filter tests for history, results, and reports.
- [ ] Run `flutter analyze` and focused Flutter tests.
- [ ] Verify the print change still owns all PDF/POS dispatch behavior.
