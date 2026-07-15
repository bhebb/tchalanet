# Proposal: Admin passkey login V1

## Why

Admin and super admin users will mostly use phones. Username login and PWA install reduce friction,
but repeated password entry on mobile is still too heavy. Passkeys let a known device use Face ID,
Touch ID, or the device unlock code without teaching the web app the phone PIN.

## What Changes

- Add a future passkey enrollment flow for authenticated `APP_USER` accounts.
- Add a future passkey login option on admin/platform login pages.
- Keep email/username + password as the fallback and recovery path.
- Keep SellerTerminal POS authentication unchanged.

## Non-Goals

- No implementation in this PR.
- No passkey support for SellerTerminal POS.
- No replacement of Firebase email/password in V1; passkeys are an additional login method.

## Decisions

- Passkeys are scoped to `APP_USER` accounts only.
- Enrollment happens only after a successful authenticated session.
- Login with passkey still ends by establishing the normal application session and calling
  `/runtime/private`.
- A user can have multiple passkeys, typically one per device.
- Admin/superadmin password reset remains available as the recovery path.

