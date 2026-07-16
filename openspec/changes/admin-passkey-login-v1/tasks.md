# Tasks: Admin passkey login V1

- [x] Define product and security behavior for passkey enrollment and login.
- [x] Defer implementation; keep V0 as username/email + password, password manager, reset password,
  and mobile PWA install guidance.
- [ ] Decide RP ID and allowed origins for production, staging, local development, PWA, and previews.
- [ ] Verify production Firebase passkey support before choosing the adapter.
- [ ] Choose native Firebase passkey adapter or Tchalanet WebAuthn + Firebase custom token adapter.
- [ ] Decide whether password recovery revokes existing passkeys.
- [ ] Define the recent-authentication window and accepted step-up methods.
- [ ] Define challenge storage, expiry, and atomic single-use consumption.
- [ ] Define future super-admin policy: optional, required for login, or required for sensitive actions.
- [ ] Backend: define WebAuthn registration options endpoint.
- [ ] Backend: define WebAuthn registration verification endpoint.
- [ ] Backend: define WebAuthn authentication options endpoint.
- [ ] Backend: define WebAuthn authentication verification endpoint.
- [ ] Backend: persist passkey credentials per `APP_USER` with tenant/user audit context.
- [ ] Backend: add support credential list/revoke endpoints without enrollment capability.
- [ ] Web: add "Continue with a passkey" action on admin/platform login.
- [ ] Web: add authenticated "Add this passkey" flow in profile/security.
- [ ] Web: keep username/email + password and reset password as fallback.
- [ ] Security: add rate limiting and audit logs for options, verification, and revocation attempts.
- [ ] Validation: unit tests for RP/origin, challenge lifecycle, recent-auth, user verification, revoked
  credentials, Firebase identity resolution, and uniform errors.
- [ ] Validation: integration tests for replay rejection, wrong RP/origin, blocked users, credential
  uniqueness, audit creation, and final Firebase/Tchalanet bootstrap pipeline.
- [ ] Validation: web tests for browser support display, enrollment, login cancellation, password fallback,
  and session restore not starting a WebAuthn ceremony.
- [ ] Validation: browser compatibility checks on iOS Safari, Android Chrome, and desktop Chrome.
