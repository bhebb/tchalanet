# Sell Flow Mobile — Architecture & Implementation Guide

> Complete lottery ticket sell flow for POS terminals, from catalog to success confirmation.

## Overview

The sell flow is a stateful, server-driven user journey spanning:
1. **Catalog** (draws + games available)
2. **Selection** (draw → game → numbers → stake)
3. **Preview** (server validation: is this combo acceptable?)
4. **Confirmation** (final sell with idempotency)
5. **Success** (share/print ticket code)

```
CashierSellPage
├─ SellController (Riverpod state machine)
├─ Form data (draws, games, selections, stake)
├─ Preview result (acceptance + issues)
└─ CTA: APERÇU → CONFIRMER
    ↓ (success)
    CashierSellSuccessPage
    ├─ Ticket code display
    ├─ SendReceiptSheet (SMS/WhatsApp/email)
    └─ NOUVEAU TICKET button
```

## State Machine (`SellController`)

Sealed class hierarchy with exhaustive pattern matching:

```dart
sealed class SellState
├─ SellLoadingCatalog          // Initial load
├─ SellReady                    // Ready to edit (may have preview + error)
├─ SellPreviewing              // POST /preview in flight
├─ SellConfirming              // POST /sell in flight
├─ SellSuccess                 // Ticket created, show code
└─ SellCatalogError            // Load failed
```

**Key behaviors:**
- **Idempotency**: One UUID v4 key per SellController instance; `reset()` generates new key
- **No null states**: State always represents a valid transition
- **Form preservation**: Errors don't clear selections; user can fix and retry
- **Preselection**: If home has `primaryDraw`, auto-select it on load

## Data Layer

### Services
- `CashierSellCatalogService`
  - `fetchAvailableDraws(lookaheadHours=48, limit=20)` → `List<CashierAvailableDrawView>`
  - `fetchAvailableGames()` → `List<CashierGameOptionResponse>`

- `CashierTicketService`
  - `preview(CashierTicketPreviewRequest)` → `CashierTicketPreviewResponse`
  - `sell(CashierSellTicketRequest, idempotencyKey)` → `CashierSellTicketResponse`

### Models

**Form data** (`SellFormData`):
```dart
draws: List<CashierAvailableDrawView>
games: List<CashierGameOptionResponse>
selectedDrawId: String?
selectedGameCode: String?
selectedBetType: String?           // e.g. "Unit", "Double", "Triple"
selectedBetOption: int?            // Combo index if multi-option game
selection: String                  // Numbers entered: "45" or "12-45-89"
stake: double                       // HTG amount

canPreview: bool  // All fields valid
```

**Preview response**:
```dart
isAccepted: bool
totalAmount: double
formattedAmount: String
issues: List<CashierSaleIssue>    // Why rejected if !accepted
```

## Presentation Layer

### CashierSellPage

**Architecture:**
- Consumes `SellController` (readonly)
- Builds layout dynamically per state (SellReady, SellPreviewing, SellConfirming)
- One `_SellBody` widget layout, no state variants in UI (prevents bloat)

**Components:**
- **Draw chips** (horizontal scroll, shows cutoff countdown)
  - Computed `formattedCutoff` from `cutoffAt` timestamp
  - Click → `selectDraw()`
- **Game chips** (auto-hide if no draw selected)
- **Bet option chips** (conditional, if game has multiple bet types)
- **Number input** (large HTG display for stakes, uppercase formatter)
- **Preview card** (green if ACCEPTED, red with issues if REJECTED)
- **CTAs**: APERÇU (always) → CONFIRMER (only if accepted)

**Touch targets** (POS-optimized):
- Button height: 56dp base, ×1.15 scale on `SurfaceContext.posTerminal`
- All interactive targets ≥56dp (operator fingers, quick taps)

### CashierSellSuccessPage

**Shows after `POST /sell` succeeds:**
- Large ticket code display (selectable, auto-copy on tap)
- 2×2 action grid: Copy | Message | Print | WhatsApp
- Sticky bottom: NOUVEAU TICKET button

**Message action:**
- Launches `SendReceiptSheet` (SMS/WhatsApp/email/Slack dev)
- Backend: `POST /tickets/{id}/send` with `deliveryMode` + `buyerPhoneNumber` / `buyerEmail`

## Request/Response Contracts

### POST /tenant/cashier/tickets/preview

**Request:**
```json
{
  "terminalId": "string",
  "drawId": "string",
  "drawChannelId": "string (optional)",
  "currency": "string (HTG)",
  "lines": [
    {
      "gameCode": "string",
      "betType": "string",
      "selection": "string",
      "stake": number,
      "betOption": int (optional)
    }
  ]
}
```

**Response:**
```json
{
  "isAccepted": boolean,
  "totalAmount": number,
  "formattedAmount": "string (HTG)",
  "issues": [
    {
      "code": "string",
      "message": "string (Haitian French)"
    }
  ]
}
```

### POST /tenant/cashier/tickets/sell

**Request:** (same as preview + idempotency header)

**Headers:**
```
X-Idempotency-Key: <UUID v4>
X-Tch-Terminal-Id: <from OpContextInterceptor>
X-Tch-Outlet-Id: <from OpContextInterceptor>
X-Tch-Sales-Session-Id: <from OpContextInterceptor>
```

**Response:**
```json
{
  "ticketCode": "string (e.g. 40CP-JBMR)",
  "publicCode": "string (shareable variant, optional)",
  "totalAmount": number,
  "formattedAmount": "string",
  "backup": {
    "shareableText": "string (emoji-safe for SMS)"
  }
}
```

## UX Flows

### Happy Path
1. **Home → /sell** (preseleced draw if available)
2. **Select draw** (if needed) → games load
3. **Select game** → bet options appear (if applicable)
4. **Enter numbers + stake**
5. **APERÇU** → server preview
   - ✅ Accepted → button changes to CONFIRMER
   - ❌ Rejected → red card with issues
6. **CONFIRMER** → POST /sell
   - Loading spinner
   - Success → navigate to /pos/sell/success
7. **Show code** → copy / share / print
8. **NOUVEAU TICKET** → reload catalog, reset form

### Error Recovery
- **Network error during preview**: card shows error, user can retry
- **Network error during sell**: user sees error, form preserved, can retry
- **Validation error from server**: red card explains the issue (Haitian French)
- **Offline**: opaque errors from Dio, user must retry when online

### Edge Cases
- **Draw closes mid-selection**: preview rejects with "draw closed"
- **Game disabled mid-selection**: games list refreshes on next load
- **Stake out of range**: preview rejects with amount constraints
- **Cutoff expired**: draw chips show "Closed" label, selection disallowed

## Testing Strategy

**Unit tests** (`sell_controller_test.dart`):
- State transitions (ready → previewing → success)
- Form data mutations (selectDraw, updateStake, etc.)
- Idempotency key generation (UUID v4 format)

**Widget tests** (`cashier_sell_page_test.dart`):
- Render each state variant (ready, previewing, confirming)
- Input formatters (uppercase game codes, numeric stakes)
- Chip selections trigger controller actions
- Preview card colors (green vs red)

**E2E tests** (via Dart test + mock server):
- Full happy path: load → select → preview (accepted) → sell → success
- Rejection flow: preview with issues, user sees red card
- Idempotency: retry sell with same key, get same response

## Design System Compliance

**Colors** (TchColors):
- `success` + `successContainer` for accepted preview
- `warning` + `warningContainer` for cutoff warnings
- `error` + `errorContainer` for rejected preview
- No hardcoded hex values

**Spacing** (TchSpacing):
- s8 between chips (compact)
- s16 around cards (breathing room)
- s24 around page sections

**Touch targets**:
- POS: ≥56dp height (context.minTouchTarget)
- Mobile: ≥48dp height (Material 3 default)

**Typography**:
- displaySmall for large HTG amount
- titleSmall for game names + draw labels
- bodySmall for helper text
- labelSmall for cutoff warnings

## Accessibility

- **Semantic labels**: Game code inputs auto-uppercase (no manual CapsLock needed)
- **Focus management**: Number input auto-selects on initial focus
- **Error messaging**: Plain Haitian French, no jargon
- **Contrast**: All text passes WCAG AA (colorScheme tokens ensure this)

## Offline Behavior

**No explicit offline detection.** When network unavailable:
- `Dio` throws `DioException`
- Controller catches → shows error state
- User must retry when online
- Idempotency key preserved across retries (safe)

**Submitted tickets (optimistic):** Not stored locally; rely on server idempotency key for safety.

## Future Enhancements

- **Barcode scanner**: Replace number input with mobile camera QR scan
- **Suggested numbers**: ML-based quick picks based on recent draws
- **Multi-line tickets**: Support selling multiple games in one transaction
- **Offline drafts**: Save unsigned tickets locally, sync on reconnect
- **Print integration**: Native print preview via `printing` package
