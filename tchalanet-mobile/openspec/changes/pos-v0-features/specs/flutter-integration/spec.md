# Spec 03 — Flutter integration contract

## Domain

Flutter POS app (`apps/tchalanet-mobile`) — integration contract only; no server-side changes.

## Principles

1. **One screen, one BFF call** at mount. Mutations call only their own endpoint.
2. **No client-side authorization logic.** Server returns `editable`, `actions.canCancel`, `actions.canReprint`, `actions.canApprove` — Flutter consumes them as-is.
3. **Idempotency keys on every POST that creates state.** UUIDs generated client-side; reused on retry.
4. **`X-Tch-Platform: MOBILE | TERMINAL`** header on every request.
5. **Local persistence**: Keycloak refresh token (secure storage) + draft ticket entries (transient). Nothing else cached locally.
6. **All amounts as `Decimal`** (`package:decimal`). Never `double`.

---

## ADDED Requirements

### Requirement: App boot flow

The app SHALL follow the boot sequence below on every cold start:

```
1. Read Keycloak refresh token from secure storage.
   - absent / invalid → Login screen (S1)
2. User submits credentials → Keycloak token endpoint → access + refresh tokens stored.
3. POST /admin/profile/bootstrap  (idempotent — creates profile if absent).
4. GET  /tenant/pos/bff/bootstrap  with X-Tch-Platform header.
   - currentSession != null → Dashboard (S3)
   - currentSession == null → "Aucune session active" (S2)
```

#### Scenario: Valid stored token skips login

- **WHEN** a valid Keycloak refresh token exists in secure storage
- **THEN** the app exchanges it for an access token and proceeds directly to step 3 without showing the Login screen

#### Scenario: Absent or invalid token shows Login

- **WHEN** no refresh token or an expired/revoked refresh token is found
- **THEN** the Login screen (S1) is displayed

#### Scenario: Bootstrap routes to Dashboard when session active

- **WHEN** `GET /tenant/pos/bff/bootstrap` returns `currentSession != null`
- **THEN** the app navigates to S3 Dashboard

#### Scenario: Bootstrap routes to S2 when no session

- **WHEN** `GET /tenant/pos/bff/bootstrap` returns `currentSession == null`
- **THEN** the app navigates to S2 "Aucune session active"

---

### Requirement: Screen → endpoint mapping

Each screen SHALL mount from at most one BFF GET call.
All mutation endpoints per screen are listed below:

| Screen                   | Mount (GET)                                            | Mutations (POST / PUT / DELETE)                                                                                        |
| ------------------------ | ------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------- |
| S1 Login                 | —                                                      | Keycloak token + `POST /admin/profile/bootstrap` + `GET /bff/bootstrap`                                                |
| S2 Aucune session active | (bootstrap already loaded)                             | `POST /bff/session/open`                                                                                               |
| S3 Dashboard             | `GET /bff/dashboard` (+ refresh on focus / every 30 s) | tap draw card → S4 (preselected draw); "Fermer" → S9                                                                   |
| S4 Nouvelle vente        | `GET /bff/sale/context`                                | `POST /bff/sale/place`                                                                                                 |
| S5 Approbation (modal)   | —                                                      | `POST /bff/sale/{id}/approve`                                                                                          |
| S6 Ticket detail         | `GET /bff/sale/{id}`                                   | `POST /bff/sale/{id}/cancel` ; `POST /bff/sale/{id}/reprint`                                                           |
| S7 Historique            | `GET /bff/history?page=0&size=20&...`                  | tap row → S6                                                                                                           |
| S8 Résultats             | `GET /bff/results?date=today&lotteryCode=ALL`          | —                                                                                                                      |
| S9 Fermer la session     | `GET /bff/session/close/preview`                       | `POST /bff/session/close`                                                                                              |
| S10 Paramètres           | `GET /bff/settings`                                    | `PUT /bff/settings/{ns}/{key}` ; `DELETE /bff/settings/{ns}/{key}` ; `POST /bff/sync/trigger` ; `POST /bff/print/test` |

_All paths prefixed with `/api/v1/tenant/pos`._

#### Scenario: Dashboard screen makes one call

- **WHEN** the Dashboard screen mounts
- **THEN** exactly one call to `GET /tenant/pos/bff/dashboard` is made

---

### Requirement: One screen, one BFF mount call

No client-side fan-out. Each screen assembles its view from a single BFF response.

---

### Requirement: No client-side authorization logic

The Flutter app SHALL NOT evaluate permissions, roles, or ownership. It SHALL consume `editable`, `actions.canCancel`, `actions.canReprint`, `actions.canApprove` as returned by the server.

#### Scenario: Lock icon shown for non-editable setting

- **WHEN** a setting field has `editable = false`
- **THEN** the Flutter widget renders a 🔒 icon and the field is read-only; no permission check is performed client-side

---

### Requirement: Idempotency-Key on all state-creating POSTs

Every POST that creates state SHALL include an `Idempotency-Key: <uuid>` header.
The UUID is generated client-side before the call.
On network retry, the same UUID is reused.

| Endpoint                      | Idempotency-Key required  |
| ----------------------------- | ------------------------- |
| B2 `POST /session/open`       | Yes                       |
| B4 `POST /session/close`      | Yes                       |
| B7 `POST /sale/place`         | Yes                       |
| B8 `POST /sale/{id}/approve`  | Yes                       |
| B9 `POST /sale/{id}/cancel`   | No (idempotent by design) |
| B10 `POST /sale/{id}/reprint` | No (read-only)            |
| B16 `POST /sync/trigger`      | No                        |
| B17 `POST /print/test`        | No                        |

#### Scenario: Retry with same Idempotency-Key returns same ticket

- **WHEN** `POST /tenant/pos/bff/sale/place` is called twice with the same `Idempotency-Key`
- **THEN** the second call returns the same ticket without creating a duplicate

---

### Requirement: `X-Tch-Platform` header sent on every request

The Flutter app SHALL set `X-Tch-Platform: MOBILE` on every request (TERMINAL if deployed on a physical terminal device).
The BFF uses this header to decide whether to provision a VIRTUAL terminal (MOBILE) or require an existing PHYSICAL terminal (TERMINAL).

#### Scenario: MOBILE platform triggers virtual terminal provisioning

- **WHEN** `GET /tenant/pos/bff/bootstrap` is received with `X-Tch-Platform: MOBILE` and no terminal exists
- **THEN** a VIRTUAL terminal is provisioned and returned

---

### Requirement: All amounts rendered via `package:decimal`

The Flutter app SHALL use `package:decimal` for all monetary amounts.
Amounts from the API are decimal strings (`"14250.00"`); they SHALL be parsed to `Decimal`, never to `double`.
Display currency is always `HTG` in v0 (`uiHints.currencyDisplay`). The `$` symbol SHALL never appear in v0.

#### Scenario: Amount displayed without float rounding error

- **WHEN** the amount `"14250.00"` is received
- **THEN** the rendered text is `"14 250,00 HTG"` (locale-formatted) with no rounding deviation

---

### Requirement: Countdown timers color-coded by remaining time

Active draw cards SHALL display a countdown to `closesAt` (ISO-8601 UTC) using device clock.
Color rules:

- `> 30 min` → neutral (gray)
- `5–30 min` → warning (amber)
- `< 5 min` → danger (red)
- `<= 0` → "Fermé", card disabled

#### Scenario: Card disabled when draw closed

- **WHEN** `closesAt` is in the past
- **THEN** the draw card is grayed out and the "Ajouter au ticket" action is unavailable

---

### Requirement: Session banner shown on all screens when session is active

When `currentSession != null`, all screens except Login (S1) and "Aucune session active" (S2) SHALL display:

```
● Session ouverte • {openedAt HH:MM} • {user.displayName} • {terminal.code}     Fermer →
```

Tapping "Fermer" navigates to the Close session screen (S9).

#### Scenario: Banner absent on Login screen

- **WHEN** the Login screen (S1) is displayed with an active session
- **THEN** the session banner is not shown

---

### Requirement: Place ticket flow uses local DraftEntry state

The sale flow SHALL manage a `List<DraftEntry>` in local state (Riverpod provider).
The list is purely client-side until "Valider" is tapped.

```
State: List<DraftEntry> entries = []

User picks draw       → entry.drawId
User picks bet type   → entry.betTypeCode
User enters bolet     → entry.selection
User enters amount    → entry.amountHtg
Tap "Ajouter"         → entries.add(entry); reset form fields

Tap "Valider":
  POST /tenant/pos/bff/sale/place
  Header: Idempotency-Key: <uuid>            ← generated once, reused on retry
  Body:   { sessionId, entries, smsOptin, customerPhone }

Response state:
  PLACED              → success screen; entries.clear(); navigate to S3
  PENDING_APPROVAL    → modal S5 "Approbation requise"
    approval.callerCanSelfApprove == true
      → show "Approuver" → POST /sale/{id}/approve
    approval.callerCanSelfApprove == false
      → show "Demander à un superviseur"
      → ticket stays PENDING; supervisor approves on their device
```

#### Scenario: Entries list cleared after PLACED

- **WHEN** `POST /bff/sale/place` returns `state = PLACED`
- **THEN** `entries` is cleared and the app navigates to S3

#### Scenario: PENDING_APPROVAL opens modal S5

- **WHEN** `POST /bff/sale/place` returns `state = PENDING_APPROVAL`
- **THEN** modal S5 is shown; entries are NOT cleared (used for display summary)

#### Scenario: Idempotency-Key reused on retry

- **WHEN** a network error occurs during `POST /bff/sale/place` and the user retries
- **THEN** the same UUID is sent; the server returns the existing ticket if already persisted

---

### Requirement: Error codes mapped to French UI messages

Every `application/problem+json` response with a `code` field SHALL be mapped to a user-facing French message.
Unknown codes fall back to: `"Une erreur inattendue s'est produite."`

| Code                           | Message UI                                                               |
| ------------------------------ | ------------------------------------------------------------------------ |
| `NOT_ALLOWED_TO_SELL`          | "Vous n'avez pas le rôle d'agent pour vendre."                           |
| `OUTLET_NOT_ASSIGNED`          | "Vous n'êtes pas affecté à ce point de vente."                           |
| `TERMINAL_NOT_AVAILABLE`       | "Ce terminal n'est pas disponible."                                      |
| `SESSION_ALREADY_OPEN`         | "Une session est déjà ouverte sur ce terminal."                          |
| `SESSION_NOT_OPEN`             | "La session n'est plus ouverte."                                         |
| `SESSION_EXPIRED`              | "Votre session a expiré. Veuillez vous reconnecter."                     |
| `SETTING_OVERRIDE_DISABLED`    | "Ce paramètre est verrouillé par l'administrateur."                      |
| `SETTING_LEVEL_NOT_ALLOWED`    | "Vous ne pouvez pas modifier ce paramètre à ce niveau."                  |
| `SETTING_INVALID_VALUE`        | "Valeur invalide pour ce paramètre."                                     |
| `SALE_BLOCKED`                 | "Cette vente dépasse les limites autorisées."                            |
| `SALE_PENDING_APPROVAL`        | "Cette vente nécessite l'approbation d'un superviseur."                  |
| `APPROVAL_INSUFFICIENT_ROLE`   | "Vous n'avez pas le rôle requis pour approuver cette vente."             |
| `CANCEL_WINDOW_EXPIRED`        | "La fenêtre d'annulation est dépassée."                                  |
| `IDEMPOTENCY_PAYLOAD_MISMATCH` | "Une vente avec la même clé existe déjà mais avec un contenu différent." |
| `PRINTER_NOT_AVAILABLE`        | "Imprimante non disponible."                                             |

#### Scenario: SESSION_ALREADY_OPEN maps to French message

- **WHEN** B2 session/open returns 409 `SESSION_ALREADY_OPEN`
- **THEN** the Flutter UI displays "Une session est déjà ouverte sur ce terminal." and redirects to Dashboard

---

### Requirement: Network resilience (v0)

There is no offline support for sales in v0. The app SHALL degrade gracefully on connectivity loss.

- On network loss: persistent banner "Connexion perdue, les ventes sont temporairement indisponibles."
- Draft closing amount is preserved locally if the user is mid-close when connectivity drops.
- On reconnect: `GET /tenant/pos/bff/dashboard` is called to refresh state.

#### Scenario: Offline banner shown on network loss

- **WHEN** a network request fails with a connectivity error (no HTTP response)
- **THEN** the banner "Connexion perdue, les ventes sont temporairement indisponibles." is displayed

#### Scenario: Dashboard refreshed on reconnect

- **WHEN** network connectivity is restored
- **THEN** `GET /tenant/pos/bff/dashboard` is called automatically

#### Scenario: Closing amount preserved during mid-close network loss

- **WHEN** the user has entered a closing amount on S9 and loses connectivity before submitting
- **THEN** the entered amount is not lost; the user can retry `POST /bff/session/close` when reconnected

---

### Requirement: Receipt printing is client-side only

The BFF returns a `receipt.format = "ESC_POS_TEXT"` payload with `lines[]`.
The Flutter app is responsible for sending this to the connected thermal printer driver.
On VIRTUAL terminals, `POST /sale/{id}/reprint` returns 422 — Flutter SHALL hide the reprint button when `actions.canReprint = false`.

#### Scenario: Reprint button hidden on VIRTUAL terminal

- **WHEN** ticket detail response has `actions.canReprint = false`
- **THEN** the "Réimprimer" button is not rendered

---

### Requirement: Build flavors configured

Three build flavors SHALL be defined:

| Flavor    | API base URL                        |
| --------- | ----------------------------------- |
| `dev`     | `https://dev-api.tchalanet.com`     |
| `staging` | `https://staging-api.tchalanet.com` |
| `prod`    | `https://api.tchalanet.com`         |

Each flavor uses its own Keycloak realm configuration.

#### Scenario: Correct base URL used in dev build

- **WHEN** the app is built with `--flavor dev`
- **THEN** all API calls target `https://dev-api.tchalanet.com`

---

### Requirement: Local state shape minimal and documented

The Flutter app SHALL persist only: Keycloak refresh token (secure storage) and draft ticket entries (transient — loss on crash is acceptable).
The `AppSession` local store SHALL contain:

```dart
class AppSession {
  final UserDto user;
  final OutletDto? outlet;
  final TerminalDto? terminal;
  final SalesSessionDto? currentSession;
  final UiHintsDto uiHints;
}
```

Updated on: bootstrap (full overwrite), session open (`currentSession` populated), session close (`currentSession` reset to null), dashboard refresh (counters updated).

#### Scenario: AppSession reset on session close

- **WHEN** B4 session/close succeeds
- **THEN** `AppSession.currentSession` is set to null and the app navigates to S2

---

### Requirement: v0 target devices

Primary target: **iPhone 12+ portrait** and **Android 10+ portrait**.
Secondary (v0 must support, no landscape optimisation required): tablet PHYSICAL terminal — same UI, portrait; landscape optimisation deferred to v1.

#### Scenario: App renders correctly on Android 10 phone portrait

- **WHEN** the app runs on an Android 10 device in portrait mode
- **THEN** all screens render without overflow or layout errors
