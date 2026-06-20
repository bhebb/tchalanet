# Firebase Auth Session Persistence

## Why

The web app must use Firebase as the identity provider while keeping the Tchalanet session derived from the private runtime bootstrap. Users who close the browser and return with a persisted Firebase session must remain authenticated instead of being redirected to login during Firebase startup.

## What

- Keep Firebase SDK access inside the Firebase auth adapter.
- Make the provider-neutral session service depend on `AUTH_CLIENT`.
- Wait for Firebase auth state restoration before reading the current provider user or token.
- Attach bearer tokens to all non-public Tchalanet API calls through the shared application API matcher.
- Cover login, logout, restored-session, and bearer retry behavior with focused tests.

## Impact

- Web auth/session only.
- No backend endpoint changes.
- Existing login UI remains the entry point.
