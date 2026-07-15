# Change: Username login V0

## Why

Tenant admins and platform admins mostly use phones. Requiring the full Firebase
email every time is too heavy for daily operations, especially from a saved PWA
shortcut.

Firebase remains the active identity provider for V0, but the app should let a
human enter a short, globally unique Tchalanet username when signing in.

## What

- Add a public, rate-limited backend resolver that resolves a normalized username
  to a provider sign-in identifier.
- Update web login so the field accepts either an email or a username.
- If the identifier contains `@`, the web client calls Firebase directly.
- If it does not contain `@`, the web client calls the lookup once on submit,
  then calls Firebase with the returned resolved identifier and the user-entered password.
- Keep `/login` restore behavior unchanged: no username lookup on page load.

## Impact

- Backend: new public identity lookup endpoint in the identity/platform boundary.
- Web: login copy, validation and `AuthSessionService.login()` flow.
- Docs: auth flow, support mode, and admin POS ticket-selling docs updated before implementation.

## Non-goals

- Passkeys / WebAuthn / Face ID custom-token flow.
- Replacing Firebase Auth.
- Logging in with phone number.
- Exposing whether a username exists through distinct error messages.
- SellerTerminal authentication changes.
