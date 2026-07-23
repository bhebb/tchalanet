# Tasks: Firebase professional account migration (stg + prd)

## Phase 0 — New Firebase project(s) (owner)

- [ ] Create/confirm the professional Google account or org.
- [ ] Create Firebase project `tchalanet-stg`.
- [ ] Enable required Auth providers on `tchalanet-stg`.
- [ ] Register Android app `com.tchalanet.mobile` in `tchalanet-stg`.
- [ ] Create service account with Firebase Admin SDK role for the server.
- [ ] Grant Firebase App Distribution Admin to the distribution service account.
- [ ] Re-create the `staging` tester group in App Distribution.

## Phase 1 — Staging cutover

- [ ] Replace `FIREBASE_ADMIN_JSON_BASE64` (GitHub Environment `staging`) with
      the new staging service-account JSON (base64).
- [ ] Update `FIREBASE_PROJECT_ID`/equivalent in
      `tchalanet-infra/envs/staging/api.env` if needed.
- [ ] Re-provision staging admin/support users
      (`TCH_SECURITY_USER_BOOTSTRAP_MODE=ADMIN_PREPROVISIONED`) in the new
      project.
- [ ] `flutterfire configure` against `tchalanet-stg` →
      `lib/firebase_options.dart`, `android/app/google-services.json`,
      `firebase.json`.
- [ ] Update `FIREBASE_ANDROID_APP_ID` repo variable if the new app ID differs.
- [ ] Re-provision staging terminal users
      (`terminal.stg.tchalanet.com` domain) in the new project.
- [ ] Update each web portal's `runtime.*.json` (or CF Pages build step) with
      the new Firebase Web app config.
- [ ] Re-run `deploy-infra-runtime.yml` (staging) to pick up the new server
      secret.
- [ ] Re-run `mobile-distribute-staging.yml`; confirm distribution succeeds
      against the new project (App Distribution Admin role must be correct
      this time).
- [ ] Verify end-to-end login: web (`test.tchalanet.com`), mobile APK, API.

### Delivery check — Phase 1

- [ ] No staging config anywhere still references `tchalanet-39115`.
- [ ] Web, mobile, and API all authenticate successfully against
      `tchalanet-stg`.

## Phase 2 — Production cutover (later — do not start until Phase 1 is done)

- [ ] Decide production mobile distribution channel (App Distribution vs.
      Managed Google Play / MDM).
- [ ] Create Firebase project `tchalanet-prd`, repeat Phase 0 steps for it.
- [ ] Add `FIREBASE_ADMIN_JSON_BASE64` to the `production` GitHub Environment.
- [ ] Decide mobile flavor strategy (single swapped config vs. real Flutter
      product flavors) based on the open question in `proposal.md`.
- [ ] Update prod web portals' runtime Firebase config.
- [ ] Re-provision/migrate production admin and terminal users.
- [ ] Cut prod traffic over; verify end-to-end.
- [ ] Decommission `tchalanet-39115`: revoke its service account keys, remove
      billing, confirm no environment references it anymore.
