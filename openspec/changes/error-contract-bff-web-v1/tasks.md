# Tasks — error-contract-bff-web-v1

## 0. Reconcile the current state

- [x] Inspect the existing backend error/advice chain and the web/mobile client paths.
- [x] Identify the false completion state: the documented web feedback interceptor does not exist,
      the envelope can be unwrapped before notices are consumed, and exact-code keys are absent from
      shipped web locale bundles.
- [ ] Reconcile the related backend `complete-apiresponse-notices` OpenSpec with production code;
      keep it open until its tests and tasks match reality, then archive it through OpenSpec.
- [ ] Produce a BFF inventory across public, admin, and platform surfaces: required/optional/
      background slices, owner surface, and current response-envelope consumption.

## 1. Backend contract and catalogs

- [ ] Preserve `2xx = ApiResponse<T>` and `4xx/5xx = ProblemDetail`; publish a JSON contract fixture
      for blocking, partial-BFF, field-validation, and warning-success outcomes.
- [ ] Define the owner-based backend code catalog and forbid ad hoc externally visible code strings.
- [ ] Verify every `ProblemDetail` carries a stable code and safe support correlation through
      `GlobalErrorHandler`.
- [ ] Define a typed BFF feedback descriptor for presentation-owned slice degradation; keep domain
      business notices UI-agnostic and feature-owned.
- [ ] Require a BFF to declare every dependency as required, optional, or background before adding a
      notice/service status.
- [ ] Validate the existing `ApiResponseNotices` helper attaches only safe, reserved diagnostics and
      cannot create duplicate notices for one slice failure.

## 2. Client adapters and feedback ownership

- [ ] Make `TchBackendClient` retain `ApiResponse` whenever the caller owns notices; prohibit silent
      loss through data-only helpers for BFF screens.
- [ ] Implement one explicit notice-routing policy in each web portal (public, admin, platform):
      page/section/form/feature owner first, shell only for unowned cross-cutting feedback.
- [ ] Do not implement a blanket global notice toast: sales and other domain notices remain in their
      feature flow unless a response explicitly describes BFF presentation ownership.
- [ ] Correct Flutter `ApiException` and `ApiNotice` mapping: preserve separate request/trace/error
      references, translate by code, and never make server `title`, `detail`, or `message` the
      default customer copy.
- [ ] Define the same routing policy for Flutter POS, admin, public, and platform surfaces.

## 3. Recovery, forms, and accessibility

- [ ] Replace the minimal generic page error with one mobile-first recovery surface: localized copy,
      owner-declared retry, back action, safe copyable support reference, and admin support action.
- [ ] Standardize mutation outcomes: `editing -> submitting -> confirmed | field-errors |
      block-error`; success/warning confirmation remains visible in its owning block.
- [ ] Focus the confirmation/block error after a mutation; focus the first invalid field for a
      validation error; do not auto-retry non-idempotent mutations.
- [ ] Apply the same semantics to web and Flutter without duplicating visual components across apps.

## 4. i18n and contract tests

- [ ] Add a canonical code manifest derived from backend owner catalogs, with product copy owned by
      each client rather than Java exception messages.
- [ ] Add CI parity tests proving every supported locale has title/message for every product-visible
      code in web (`ht`, `fr`, `en`) and mobile (`ht`, `fr`, `en`).
- [ ] Test web public/admin/platform with shared response fixtures for page failure, BFF partial,
      form-field failure, and successful warning.
- [ ] Test Flutter with the same fixtures, including a no-raw-server-copy assertion.
- [ ] Add backend integration tests for correlation fields, BFF idempotent notice emission, and the
      distinction between optional degradation and blocking failure.

## 5. Documentation and rollout

- [ ] Update `error-management.md`, API-response conventions, mobile API contract, and the BFF
      playbook with the exact owner decision tree.
- [ ] Migrate the highest-risk dashboard BFFs and sensitive configuration forms first; track every
      remaining legacy response consumer in the inventory.
- [ ] Gate new BFF endpoints and new form mutations on the fixture and i18n-parity tests.
