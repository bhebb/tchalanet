# Spec 04 — Screens detailed flow (v0)

## Domain

Flutter POS app (`apps/tchalanet-mobile`) — UI/UX contract for each screen: purpose, mount calls, user interactions, mutations, error handling, navigation.

---

## ADDED Requirements

### Requirement: S1 — Login screen authenticates via Keycloak

The Login screen SHALL authenticate the user via Keycloak (Resource Owner Password Grant or PKCE per realm config).

**UI**:

- Logo "Tchalanet POS"
- Email input
- Password input (toggle show/hide)
- "Mot de passe oublié ?" link → opens browser to Keycloak forgot-password URL
- "Se connecter" button (primary, full-width, disabled until both fields filled)

**Flow**:

1. User submits → call Keycloak `/token` endpoint.
2. On success: store refresh token in secure storage, fetch access token.
3. Call `POST /admin/profile/bootstrap` (idempotent).
4. Call `GET /tenant/pos/bff/bootstrap` with `X-Tch-Platform`.
5. Navigate based on `currentSession`:
   - null → S2 "Aucune session active"
   - present → S3 Dashboard

#### Scenario: Successful login navigates to Dashboard when session is active

- **WHEN** login succeeds and `bootstrap.currentSession != null`
- **THEN** the app navigates to S3 Dashboard with the cached bootstrap payload

#### Scenario: Successful login navigates to S2 when no session exists

- **WHEN** login succeeds and `bootstrap.currentSession == null`
- **THEN** the app navigates to S2 "Aucune session active"

#### Scenario: Wrong credentials show inline error

- **WHEN** Keycloak returns an authentication error
- **THEN** the UI displays "Email ou mot de passe incorrect." inline (no navigation)

#### Scenario: Locked account shows admin contact message

- **WHEN** Keycloak returns a locked-account error
- **THEN** the UI displays "Compte verrouillé. Contactez l'administrateur."

---

### Requirement: S2 — Aucune session active screen lets the user open a session

**Mount data**: from bootstrap (no extra call needed).

**UI**:

- Big icon (lock with clock)
- Title "Aucune session active"
- Subtitle "Démarrez votre shift pour commencer à vendre"
- Read-only cards: outlet.displayName / terminal.code / user.displayName
- Input "Fond de caisse de départ (HTG)" with quick chips +500 / +1000 / +2000 / +5000
- Button "Démarrer la session" (primary; never disabled — 0 is allowed for ambulant agents)

**Action**: `POST /tenant/pos/bff/session/open` with `{ openingFloat }` and `Idempotency-Key`.

#### Scenario: Opening float of 0 is accepted

- **WHEN** the user taps "Démarrer la session" with the float field empty or 0
- **THEN** the request is sent with `openingFloat = 0` and no validation error is raised

#### Scenario: Successful open navigates to Dashboard

- **WHEN** `POST /tenant/pos/bff/session/open` returns 200
- **THEN** the app uses the session + dashboard payload from the response and navigates to S3

#### Scenario: SESSION_ALREADY_OPEN redirects to Dashboard

- **WHEN** `POST /tenant/pos/bff/session/open` returns 409 `SESSION_ALREADY_OPEN`
- **THEN** the app fetches the current session and redirects to S3

#### Scenario: NOT_ALLOWED_TO_SELL shows message with logout option

- **WHEN** `POST /tenant/pos/bff/session/open` returns `NOT_ALLOWED_TO_SELL`
- **THEN** the UI shows the mapped French message and a logout option

---

### Requirement: S3 — Dashboard is the command center during the shift

**Mount call**: `GET /tenant/pos/bff/dashboard`.
**Refresh**: on screen focus and every 30 s while visible.

**UI structure (top to bottom)**:

1. Header (Tchalanet POS + avatar + hamburger)
2. Session banner (compact, green): `● Session ouverte • {openedAt HH:MM} • {user.displayName} • {terminal.code}     Fermer →`
3. Section "Tirages en cours" — horizontal scrollable draw cards (lotteryLabel, slotLabel, countdown, daily sales); tap → S4 with that draw pre-selected
4. KPI grid 2×2: Ventes du jour (HTG + delta %), Balance en caisse (HTG), Tickets de la session (count), Ticket moyen (HTG)
5. Section "Résultats récents" + "Tout voir" link → S8; list of 3–4 last settled draws (winning numbers, winners count, payout)
6. Section "Dernier ticket" — single card (code, entries summary, total, status chip); tap → S6

**Actions**: "Fermer" on banner → S9; Bottom nav: Sales / Reports / History / Settings.

#### Scenario: Tapping a draw card pre-selects it on S4

- **WHEN** the user taps a draw card on the Dashboard
- **THEN** S4 "Nouvelle vente" opens with that `drawId` pre-selected

#### Scenario: PARTIAL dashboard renders available sections with banner

- **WHEN** one internal query fails during `GET /tenant/pos/bff/dashboard`
- **THEN** the dashboard renders available sections and shows a banner "Certaines données sont temporairement indisponibles."

#### Scenario: Dashboard refreshes every 30 s while visible

- **WHEN** S3 is visible and 30 s have elapsed since last fetch
- **THEN** `GET /tenant/pos/bff/dashboard` is called automatically

---

### Requirement: S4 — Nouvelle vente builds a multi-entry ticket

**Mount call**: `GET /tenant/pos/bff/sale/context`.

**UI**:

1. Section "Sélectionner le tirage" — vertical cards; selected card has filled accent + checkmark; closed draws grayed out with countdown
2. Section "Type de pari" — horizontal chips from `betTypes` (BOLET_2D, BORLETTE, LOTTO_3D, …)
3. Section "Détails de l'entrée": Numéro input (placeholder respects min/max digits) + Montant (HTG) input
4. Section "Montants rapides" — chips from `saleSettings.quickAmounts` + "MAX"
5. Button "Ajouter au ticket" (primary)
6. Section "Entrées (N)" — list of added entries with delete (×) + "Vider" link
7. Footer (sticky): "Total ticket: X HTG" + Button "Valider"

**Local state**: `entries[]` is client-only until validation.

**Action — validate**: `POST /tenant/pos/bff/sale/place` with `{ sessionId, entries, smsOptin, customerPhone, idempotencyKey }`

#### Scenario: Closed draw cards are non-interactive

- **WHEN** a draw has `closesAt` in the past
- **THEN** its card is grayed out and cannot be selected

#### Scenario: Quick amount chip fills the amount input

- **WHEN** the user taps a quick amount chip
- **THEN** the Montant input is updated to that value

#### Scenario: PLACED navigates back to Dashboard

- **WHEN** `POST /tenant/pos/bff/sale/place` returns `state = PLACED`
- **THEN** entries are reset and the app returns to S3

#### Scenario: PENDING_APPROVAL opens modal S5

- **WHEN** `POST /tenant/pos/bff/sale/place` returns `state = PENDING_APPROVAL`
- **THEN** modal S5 "Approbation requise" is shown

#### Scenario: SALE_BLOCKED shows toast

- **WHEN** `POST /tenant/pos/bff/sale/place` returns `SALE_BLOCKED`
- **THEN** a toast "Vente bloquée par les limites." is shown and the form remains open

#### Scenario: IDEMPOTENCY_PAYLOAD_MISMATCH shows conflict message

- **WHEN** `POST /tenant/pos/bff/sale/place` returns `IDEMPOTENCY_PAYLOAD_MISMATCH`
- **THEN** the UI shows "Conflit. Réessayez avec un nouveau ticket."

---

### Requirement: S5 — Sale approval modal handles PENDING_APPROVAL tickets

**UI**:

- Title "Approbation requise"
- Summary: "Cette vente dépasse la limite ({limit} HTG)." + reason for block
- If `approval.callerCanSelfApprove == true`: Button "Approuver" (primary) + optional textarea "Raison"
- Else: message "Veuillez demander à un superviseur." + Button "OK"

**Action**: `POST /tenant/pos/bff/sale/{id}/approve` with optional reason.

#### Scenario: Self-approve navigates to ticket confirmation

- **WHEN** `approval.callerCanSelfApprove == true` and the user taps "Approuver"
- **THEN** `POST /tenant/pos/bff/sale/{id}/approve` is called and on success the app navigates to the ticket confirmation view

#### Scenario: Supervisor-only modal shows message without approve button

- **WHEN** `approval.callerCanSelfApprove == false`
- **THEN** only the "Veuillez demander à un superviseur." message and "OK" button are shown

#### Scenario: Race condition on approval returns error

- **WHEN** `POST /tenant/pos/bff/sale/{id}/approve` returns `APPROVAL_INSUFFICIENT_ROLE`
- **THEN** the UI shows the mapped French error message

---

### Requirement: S6 — Ticket detail provides full read and actions

**Mount call**: `GET /tenant/pos/bff/sale/{id}`.

**UI**:

1. Header "Ticket #{code}"
2. Status chip (PLACED / PENDING_APPROVAL / PAID / CANCELLED)
3. Metadata: createdAt, agent.displayName, outlet.displayName, terminal.code
4. Entries list: lottery + slotLabel + betType + selection + amount per entry
5. Total + fees
6. Customer phone + SMS sent flag (if applicable)
7. Timeline: placed / approved / cancelled events with actor and timestamp
8. Action buttons (rendered only if `actions.canX = true`):
   - "Annuler" → confirm modal → `POST /sale/{id}/cancel`
   - "Réimprimer" (PHYSICAL only) → `POST /sale/{id}/reprint` → send to printer driver
   - "Approuver" (if PENDING_APPROVAL and caller can) → opens S5

#### Scenario: Cancel action only shown when canCancel is true

- **WHEN** `actions.canCancel = false`
- **THEN** the "Annuler" button is not rendered

#### Scenario: Reprint button only shown when canReprint is true

- **WHEN** `actions.canReprint = false`
- **THEN** the "Réimprimer" button is not rendered

#### Scenario: CANCEL_WINDOW_EXPIRED shown as error

- **WHEN** `POST /sale/{id}/cancel` returns `CANCEL_WINDOW_EXPIRED`
- **THEN** the UI displays "Délai d'annulation dépassé."

#### Scenario: PRINTER_NOT_AVAILABLE shown as error

- **WHEN** `POST /sale/{id}/reprint` returns `PRINTER_NOT_AVAILABLE`
- **THEN** the UI displays "Imprimante non disponible."

---

### Requirement: S7 — Historique provides a paged, filterable ticket list

**Mount call**: `GET /tenant/pos/bff/history?page=0&size=20`.

**UI**:

1. Search input "Rechercher ticket #…"
2. Date picker (single day or range)
3. Filter chips: "Cette session" / "Aujourd'hui" / "7 jours" + per-lottery (Tous / NY / FL / GA)
4. Status filter chips: Tous / Payés / En attente / Annulés
5. List of ticket cards: "TK-8942 • Today 14:30 • NY • 500 HTG • Payé" — tap → S6
6. Pagination: load more on scroll

#### Scenario: Filter change triggers a new fetch

- **WHEN** the user changes any filter chip or date picker
- **THEN** `GET /tenant/pos/bff/history` is called with the updated query params

#### Scenario: Empty state shown when no tickets match

- **WHEN** the history response returns an empty list
- **THEN** the UI shows "Aucun ticket pour ces filtres."

---

### Requirement: S8 — Résultats shows settled draws with sales/payout/margin

**Mount call**: `GET /tenant/pos/bff/results?date=today&lotteryCode=ALL`.

**UI**:

1. Tabs "Aujourd'hui" / "Hier" / Custom (date picker)
2. Filter chips by lottery: TOUS / NY / FL / GA
3. Settled draw cards: lotteryCode + slotLabel + drawTime + winning numbers in big circles (3 numbers) + stats row (Ventes / Payé / Marge)
4. Marge omitted if caller lacks `sales.results.margin.read`

#### Scenario: Margin hidden without permission

- **WHEN** the caller lacks `sales.results.margin.read`
- **THEN** the Marge column is not rendered in the stats row

#### Scenario: Tab change refetches results

- **WHEN** the user taps "Hier" or selects a custom date
- **THEN** `GET /tenant/pos/bff/results` is called with the updated `date` param

#### Scenario: Empty state shown when no results for period

- **WHEN** the results response returns an empty list
- **THEN** the UI shows "Pas de résultats pour cette période."

---

### Requirement: S9 — Fermer la session produces z-report and closes the shift

**Mount call**: `GET /tenant/pos/bff/session/close/preview`.

**UI**:

1. Title "Fermer la session" + subtitle "Vérifiez les données avant de finaliser"
2. Read-only recap: Heure d'ouverture, Durée, Tickets vendus, Total des ventes, Fond de départ, **Attendu en caisse** (highlighted)
3. Input HTG "Montant final en caisse" (numeric keyboard)
4. If `closingAmount != expected`: orange alert "Écart: −X HTG (manque)" + optional textarea "Note / explication"
5. Buttons: "Confirmer la fermeture" (primary, with confirm modal) + "Annuler" (link)

**Action**:

- Confirm modal: "Fermer la session ? Cette action est irréversible."
- `POST /tenant/pos/bff/session/close` with `{ sessionId, closingAmount, varianceNote }`
- On success: navigate to S2 "Aucune session active"

#### Scenario: Variance alert shown when closing amount differs from expected

- **WHEN** the user enters a closing amount different from `expectedCash`
- **THEN** an orange alert shows the variance amount inline

#### Scenario: Successful close navigates to S2

- **WHEN** `POST /tenant/pos/bff/session/close` returns 200
- **THEN** the app navigates to S2 "Aucune session active"

#### Scenario: Confirm modal required before close

- **WHEN** the user taps "Confirmer la fermeture"
- **THEN** a modal "Fermer la session ? Cette action est irréversible." is shown before the POST is sent

---

### Requirement: S10 — Paramètres reads and (when permitted) edits operational settings

**Mount call**: `GET /tenant/pos/bff/settings`.

**UI sections** (each a card):

| Section                  | Key fields                                                                                                      |
| ------------------------ | --------------------------------------------------------------------------------------------------------------- |
| A. Notifications & Envoi | Toggle SMS, Toggle Email, frais SMS/Email (editable per `editable` flag)                                        |
| B. Point de vente        | Adresse outlet (editable by ownership), Devise principale (read-only)                                           |
| C. Synchronisation       | Toggle Mode hors-ligne, Last sync timestamp, Button "Synchroniser maintenant"                                   |
| D. Imprimante thermique  | Largeur papier 58/80, Contraste slider, Button "Imprimer une page de test" — only if `terminal.kind = PHYSICAL` |

**Actions**:

- Each editable field: `PUT /tenant/pos/bff/settings/{ns}/{key}` → response is refreshed settings payload → update UI in place
- Locked fields: show 🔒 icon + tooltip "Géré par l'administrateur"
- Each editable field has a "Réinitialiser" link → `DELETE /tenant/pos/bff/settings/{ns}/{key}?level=AGENT|TERMINAL`

#### Scenario: Locked field shows lock icon

- **WHEN** a settings field has `editable = false`
- **THEN** a 🔒 icon is shown and the field is read-only; `PUT` is not called

#### Scenario: Thermal printer section hidden on VIRTUAL terminal

- **WHEN** `terminal.kind != PHYSICAL`
- **THEN** section D "Imprimante thermique" is not rendered

#### Scenario: Sync trigger calls POST endpoint

- **WHEN** the user taps "Synchroniser maintenant"
- **THEN** `POST /tenant/pos/bff/sync/trigger` is called

#### Scenario: Test print calls POST endpoint

- **WHEN** the user taps "Imprimer une page de test"
- **THEN** `POST /tenant/pos/bff/print/test` is called

---

### Requirement: Bottom navigation disabled tabs when no session is active

The bottom navigation (Sales / Reports / History / Settings) SHALL be shown on all post-login screens.
When no session is active (S1 / S2), all tabs except **Settings** SHALL be grayed out and non-interactive.

#### Scenario: History tab disabled on S2

- **WHEN** the user is on S2 "Aucune session active"
- **THEN** the History tab is grayed out and tapping it has no effect

#### Scenario: Settings tab always reachable

- **WHEN** the user is on any screen (S1 or S2 without session)
- **THEN** the Settings tab (→ S10) remains enabled
