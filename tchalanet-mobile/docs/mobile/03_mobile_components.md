# 03 Mobile Components

> Status: normative  
> Scope: required shared UI components before POS V1 screen work

Do not start feature screens by inventing local buttons, cards, banners, and action bars.
Create shared components first and compose screens from them.

## Global Components

| Component | Purpose |
| --- | --- |
| `CashierHeader` | Compact seller/outlet/session header. |
| `SessionStatusBadge` | Open/closed/offline/blocked status indicator. |
| `PrimaryActionButton` | Main action; filled primary. |
| `SecondaryActionButton` | Secondary action; outline or surface. |
| `DangerActionButton` | Destructive/blocking action; red only. |
| `MetricCard` | One metric with label and numeric value. |
| `ShortcutButton` | Large shortcut for POS actions. |
| `BottomActionBar` | Sticky bottom action zone. |
| `EmptyState` | No data/cart/tickets state with next action. |
| `IssueBanner` | Warning/error/offline/blocking message. |
| `LoadingState` | Loading placeholder for profile/session/draws. |
| `ErrorState` | Error message plus retry or fallback action. |

## Sell Ticket Components

| Component | Purpose |
| --- | --- |
| `DrawCurrentCard` | Current draw summary and cutoff/status. |
| `GameTabBar` | Game selection with compact tabs. |
| `TicketEntryForm` | Number/option/stake input. |
| `StakeQuickButtons` | Fast stake choices. |
| `TicketCartList` | Current cart lines. |
| `TicketCartLine` | One cart row; stable height. |
| `TicketTotalBar` | Total and important validation state. |
| `StickyTicketActionBar` | Verify/sell action always visible. |

## Success Components

| Component | Purpose |
| --- | --- |
| `SuccessHero` | Sale accepted state. |
| `ClientCodeDisplay` | Very large client/public code. |
| `TicketActionGrid` | Copy, print, send secondary actions. |
| `NewTicketButton` | Main next action after success. |

## Component Rules

- Components must use `ThemeData` tokens.
- Components must not call repositories or Dio.
- Components receive display data and callbacks.
- Components must define disabled/loading states when interactive.
- Components must be usable on small Android screens.
- Text must not overflow its touch target.
- Button labels should be short and action-oriented.

## Button Rules

Primary:

- One per screen or sticky action area.
- Filled primary.
- Used for sell, verify, confirm, new ticket.

Secondary:

- Multiple allowed.
- Outline or surface.
- Used for print, send, copy, view details.

Danger:

- Only for destructive or blocking actions.
- Red.
- Requires clear wording.

## Feedback Rules

Loading:

- Show what is loading: session, draws, ticket, profile.
- Avoid spinner-only full screens if stale content can remain visible.

Error:

- Explain what happened in user language.
- Give a next action: retry, go offline, open session, contact admin.

Offline:

- Show whether selling is allowed.
- Never imply offline tickets are confirmed by the backend.
