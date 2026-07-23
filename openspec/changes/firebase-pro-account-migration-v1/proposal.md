# Change: Firebase professional account migration (stg + prd)

## Status

Proposed

## Context

Tchalanet currently runs on a single Firebase project, `tchalanet-39115`,
owned under a personal Google/Firebase account. This one project is shared by:

- **Web**: admin-portal, platform-portal, public-portal (Firebase Auth client SDK).
- **API/server**: `RUNTIME_IDENTITY_PROVIDER=firebase` token verification,
  `FIREBASE_ADMIN_JSON_BASE64` service-account credential, user bootstrap.
- **Mobile**: `firebase_options.dart` / `google-services.json`
  (`projectId: tchalanet-39115`), App Distribution for staging APK builds.

There is no staging/production separation at the Firebase level today — one
project backs everything. The owner is moving Firebase to a professional
Google account and wants two projects going forward: one for staging, one for
production, so environments stop sharing Firebase Auth users, App Check state,
and App Distribution testers.

## Goal

- Create a **staging** Firebase project under the new professional account and
  cut every staging-facing surface (web, API, mobile) over to it.
- Prepare (but do not yet execute) the equivalent **production** cutover on a
  second Firebase project, once staging is validated.
- Decommission the old personal-account project (`tchalanet-39115`) only after
  both cutovers are confirmed and nothing still depends on it.

## Non-goals

- Do not build Flutter product flavors / multi-app-id Android variants unless
  the production phase actually requires running staging and prod builds
  side-by-side on the same device. Today only one build is installed at a
  time, so a single mobile Firebase config swapped at build time is enough.
- Do not touch business logic, authorization rules, or Tchalanet's own
  identity/role model — this is provider infrastructure only.
- Do not migrate Firebase Auth *user records* from the old project (staging
  users will be reprovisioned fresh in the new project; production migration
  strategy, if any, is decided in Phase 2).

## Impact

- **Infra**: Doppler `stg` (and later `prd`) config secrets
  (`FIREBASE_ADMIN_JSON_BASE64`, `FIREBASE_PROJECT_ID`), GitHub `staging`
  (later `production`) environment secrets, `envs/staging/api.env`.
- **Web**: `runtime.*.json` Firebase config per portal, or the CF Pages build
  step that generates it.
- **Mobile**: `flutterfire configure` output
  (`lib/firebase_options.dart`, `android/app/google-services.json`,
  `firebase.json`), `FIREBASE_ANDROID_APP_ID` repo variable, App Distribution
  tester groups.
- **Server**: none functionally — `TCH_IDENTITY_PROVIDER=firebase` already
  provider-neutral; only the credential/project ID values change.

## Plan

### Phase 0 — New Firebase project(s) (owner, one-time)

- [ ] Create the new professional Google Cloud / Firebase organization (or
      account) if not already done.
- [ ] Create Firebase project **tchalanet-stg** (or similar) under it.
- [ ] Enable Firebase Authentication (Email/Password + whatever providers the
      apps use today) on the new staging project.
- [ ] Register a new **Android app** in the staging project
      (package `com.tchalanet.mobile`) → download `google-services.json`.
- [ ] Create a new **service account** with the **Firebase Admin SDK** role
      (server token verification) and, separately, grant
      **Firebase App Distribution Admin** to whichever service account will
      run `mobile-distribute-staging.yml`.
- [ ] Re-create the **`staging`** tester group in
      Firebase Console → App Distribution → Testers & Groups.
- [ ] (Prod, later) Repeat all of the above for a **tchalanet-prd** project.

### Phase 1 — Staging cutover (do now)

- [ ] **Server**: replace the `FIREBASE_ADMIN_JSON_BASE64` GitHub Environment
      secret (`staging`) with the new staging service-account JSON
      (base64). Update `FIREBASE_PROJECT_ID` (or equivalent) in
      `tchalanet-infra/envs/staging/api.env` if the project ID is not read
      purely from the service-account JSON.
- [ ] **Server**: re-provision the admin/support users the staging backend
      expects (`ADMIN_PREPROVISIONED` bootstrap mode) as real users in the new
      staging Firebase project.
- [ ] **Mobile**: run `flutterfire configure` against the new staging Firebase
      project to regenerate `lib/firebase_options.dart`,
      `android/app/google-services.json`, and `firebase.json`.
- [ ] **Mobile**: update the `FIREBASE_ANDROID_APP_ID` repo variable (or leave
      unset if the workflow should read it from the regenerated
      `google-services.json` default — confirm current default in
      `mobile-distribute-staging.yml`).
- [ ] **Mobile**: re-provision staging **terminal** Firebase users
      (`terminal.stg.tchalanet.com` email domain) in the new project, matching
      what the backend seeds/expects.
- [ ] **Web**: update each portal's `runtime.*.json` (or the CF Pages build
      step generating it) with the new staging Firebase Auth config
      (`apiKey`, `authDomain`, `appId`, etc. for the new project's Web app).
- [ ] Run `deploy-infra-runtime.yml` (staging) to pick up the new server
      secret; run `mobile-distribute-staging.yml` to build/distribute a new
      staging APK against the new project; verify web login on
      `test.tchalanet.com`.
- [ ] **Delivery check**: login works end-to-end (web + mobile + API) against
      the new staging Firebase project; old `tchalanet-39115` project is no
      longer referenced by any staging config.

### Phase 2 — Production cutover (later, do not start until Phase 1 is verified)

- [ ] Decide the mobile distribution channel for production (Firebase App
      Distribution stays beta-only per `RB-03`; evaluate Managed Google Play /
      MDM before going further).
- [ ] **Server**: add `FIREBASE_ADMIN_JSON_BASE64` (and `PROD_SERVER_HOST`
      equivalents already exist) to the `production` GitHub Environment,
      sourced from the new **tchalanet-prd** service account.
- [ ] **Mobile**: decide whether prod needs a distinct Flutter build flavor
      (separate `google-services.json` per flavor) or a build-time
      `flutterfire configure` swap identical to staging's approach — revisit
      the non-goal above once this is closer.
- [ ] **Web**: prod portals' runtime Firebase config → new prod project.
- [ ] Re-provision production admin/terminal users in the new prod project
      (or plan a real user migration if production already has live users by
      then).
- [ ] Cut prod traffic over, verify, then decommission
      `tchalanet-39115` (personal account) entirely: revoke its service
      account keys, remove it from Firebase/GCP billing, confirm nothing in
      any environment still points at it.

## Open questions (owner to confirm before Phase 1 starts)

1. Which auth providers does the new staging project need enabled beyond
   Email/Password (phone, Google sign-in, etc.)?
2. Who holds the new professional Google account / who else needs
   Owner/Editor access to the new Firebase projects?
3. Should mobile staging and prod ever need to be installed side by side on
   the same test device? If yes, flavors must be introduced in Phase 1
   instead of deferred to Phase 2.
