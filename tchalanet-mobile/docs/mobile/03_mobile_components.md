# 03 Mobile Components

> Status: normative  
> Scope: required shared UI components before POS V1 screen work

Do not start feature screens by inventing local buttons, cards, banners, and action bars.
Create shared components first and compose screens from them.

## Global Components

The shared foundation is available from
`lib/design_system/components/components.dart`.

| Component | Purpose |
| --- | --- |
| `PrimaryActionButton` | Main action; filled primary. |
| `SecondaryActionButton` | Secondary action; outline or surface. |
| `TonalActionButton` | Lower-emphasis semantic action. |
| `DangerActionButton` | Destructive/blocking action; red only. |
| `SemanticIconAction` | Icon action with a required localized tooltip. |
| `PosActionButton` | Large POS action using a semantic tone. |
| `BottomActionBar` | Sticky bottom action zone. |
| `FeedbackState` | Loading, empty, error, offline, blocked, or success feedback. |
| `AppNotificationBanner` | Temporary semantic notification rendered by the root notification host. |
| `StatusBadge` | Semantic status indicator. |
| `OnlineBadge` | Online/offline indicator with caller-provided labels. |
| `SectionHeader` | Section title and optional trailing action. |
| `SurfaceCard` | Material 3 surface-container card. |
| `FieldError` | Accessible field validation message. |
| `AdaptiveNavigationShell` | Bottom navigation on compact screens and navigation rail on wider screens. |

Feature-specific shared components such as `CashierHeader`, `MetricCard`, and
`IssueBanner` are introduced only when a migrated screen proves their reusable API.

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
- Components must not define user-visible text. The caller resolves every label,
  message, and tooltip through i18n.
- Components accept semantic variants, not arbitrary feature colors.

Use `FieldError` for one field, `FeedbackState` for durable screen state, and the root
notification host for temporary cross-screen information, success, warning, or error.

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
