# 04 Mobile Screens V1

> Status: normative  
> Scope: first POS screens and required states

## Login

Main task: authenticate.

Must show:

- Username/password or configured auth action.
- Clear login action.
- Loading state.
- Invalid credentials state.
- Network unavailable state.

Must not show:

- POS dashboard.
- Marketing/branding block larger than the form.

## Accueil Vendeur

Main task: start selling or fix the operational context.

Must show:

- Seller.
- Outlet.
- Terminal.
- Session.
- Primary button: `Vendre`.
- Last transaction.
- Session total.
- Access to tickets, close session, profile.

Must not show:

- Heavy branding block.
- Long lists.
- Dense dashboard.

Required states:

- Loading profile/session.
- Session closed with open-session action.
- Terminal invalid with blocking message.
- Outlet blocked with blocking message.
- Offline mode.

## Vente Ticket

Main task: build a ticket and verify it.

Must show:

- Current draw.
- Game choice.
- Number / option / stake input.
- Add button.
- Current cart.
- Total.
- Sticky verify action.

Must not hide:

- Total.
- Final action.
- Important cart lines.

Required states:

- Loading draws.
- No open draw.
- Empty cart.
- Invalid selection.
- Limit warning.
- Validation rejected.
- Network error with retry/offline fallback where allowed.

## Preview / Verification

Main task: confirm the ticket before final sale.

Must show:

- Ticket lines.
- Stake and total.
- Warnings or rejection reason.
- Primary confirmation action when allowed.
- Back/edit action.

Rules:

- Warnings must not look like success.
- Rejected tickets must not expose a primary sell action.
- The seller must understand what to change.

## Succes Vente

Main task: give the client code and start the next useful action.

Must show:

- Sale accepted.
- Huge client code.
- Simple instruction.
- Primary action: new ticket.
- Secondary actions: copy, print, send.

Rules:

- `ClientCodeDisplay` uses `heroCode`.
- The code must be readable on small screens.
- Do not bury the new-ticket action below secondary actions.

## Tickets Recents

Main task: find a recent ticket.

Must show:

- Recent tickets.
- Search/filter only if it does not slow the first view.
- Ticket status.
- Retry/error state.

Rules:

- Keep rows compact.
- Avoid full back-office history density in POS V1.

## Session

Main task: open or close the current seller session.

Must show:

- Current session state.
- Opening action if closed.
- Closing action if open.
- Session totals needed for the action.

Rules:

- Closing session is a high-friction action, not a casual red link.
- Dangerous actions use `DangerActionButton`.

## Profile

Main task: identify user and access safe preferences.

Must show:

- Seller identity.
- Tenant/outlet/terminal context.
- Logout.

Must not show:

- Admin settings.
- Dense permissions matrix.
