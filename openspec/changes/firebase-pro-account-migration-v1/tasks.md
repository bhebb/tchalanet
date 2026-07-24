# Tasks: Firebase professional account migration (stg + prd)

## Phase 0 — New Firebase project(s) (owner)

- [x] Create/confirm the professional Google account or org.
- [x] Create Firebase project `tchalanet` (single project so far; a
      dedicated `tchalanet-stg` was not split out - see note below).
- [x] Enable required Auth providers (Email/Password) on `tchalanet`.
- [x] Register Android app `com.tchalanet.mobile` in `tchalanet`.
- [x] Create service account (`tch-dist@tchalanet.iam.gserviceaccount.com`)
      with Firebase Admin SDK role (Firebase Authentication Admin) for the
      server.
- [x] Grant Firebase App Distribution Admin to the distribution service
      account (same `tch-dist` SA).
- [x] Re-create the `staging` tester group in App Distribution.

## Phase 1 — Staging cutover

- [x] Replace `FIREBASE_ADMIN_JSON_BASE64` (GitHub Environment `staging`)
      with the new staging service-account JSON (base64).
- [x] Set `FIREBASE_PROJECT_ID=tchalanet` in
      `tchalanet-infra/envs/staging/api.env` (docker-compose default still
      points at the old project - required, not optional as originally
      assumed).
- [x] Staging admin/support users re-provisioned automatically by the
      existing `ADMIN_PREPROVISIONED` bootstrap on API startup (no manual
      step needed).
- [x] `flutterfire configure` against `tchalanet` →
      `lib/firebase_options.dart`, `android/app/google-services.json`,
      `firebase.json`.
- [x] Updated `FIREBASE_ANDROID_APP_ID` repo variable to the new app ID.
- [x] Rotated the Android release keystore (`TCH_ANDROID_KEYSTORE_*`
      secrets) - the previous one could not be located/backed up in time.
- [~] Staging terminal users: **not re-provisioned to a new domain** - the
      existing `POS-001` terminal's Firebase user
      (`pos-001@terminal.tchalanet.local`) was already correct against the
      server's default `tch.identity.firebase.terminal-email-domain`
      (`terminal.tchalanet.local`, not `terminal.stg.tchalanet.com` as
      originally assumed in this plan). Fixed by building mobile with
      `terminal_email_domain=terminal.tchalanet.local` instead of changing
      the server. Revisit if a real `terminal.stg.tchalanet.com` domain is
      ever wired up server-side.
- [x] Updated each web portal's `environment(.prod).ts` `fallbackConfig.firebase`
      with the new project's Web app config (pulled via
      `firebase apps:sdkconfig web ... --project tchalanet`).
- [x] Re-ran `deploy-infra-runtime.yml` (staging) - API healthy against the
      new project.
- [x] Re-ran `mobile-distribute-staging.yml` - distribution succeeded
      against the new project, tester group `staging`.
- [x] Verified end-to-end login: web (`test.tchalanet.com`), mobile APK
      (`POS-001` terminal login), API.

### Delivery check — Phase 1

- [x] No active staging config references `tchalanet-39115` (only a
      stale, gitignored, unused local scratch file -
      `tchalanet-infra/envs/staging/deploy-secrets.env.local` - still
      mentions it; harmless).
- [x] Web, mobile, and API all authenticate successfully against
      `tchalanet`.

**Phase 1 complete as of 2026-07-24.**

## Phase 2 — Production cutover (later — do not start until Phase 1 is done)

- [ ] Decide production mobile distribution channel (App Distribution vs.
      Managed Google Play / MDM).
- [ ] Create Firebase project `tchalanet-prd` (or decide whether prod stays
      on the same `tchalanet` project with separate Auth users - revisit
      given Phase 1 ended up as a single project rather than the originally
      planned stg/prd split).
- [ ] Add `FIREBASE_ADMIN_JSON_BASE64` to the `production` GitHub Environment.
- [ ] Decide mobile flavor strategy (single swapped config vs. real Flutter
      product flavors) based on the open question in `proposal.md`.
- [ ] Update prod web portals' runtime Firebase config.
- [ ] Re-provision/migrate production admin and terminal users.
- [ ] Cut prod traffic over; verify end-to-end.
- [ ] Decommission `tchalanet-39115`: revoke its service account keys, remove
      billing, confirm no environment references it anymore.
