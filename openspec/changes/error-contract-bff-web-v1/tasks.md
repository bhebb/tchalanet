# Tasks — error-contract-bff-web-v1

**Status:** IN PROGRESS

**Scope:** backend BFF aggregation, shared HTTP error contract, public/admin/platform Angular
portals, Flutter mobile, client-originated failures, i18n, accessibility, recovery, and migration.

## Phase 0 — Contract decisions, security baseline, and inventory

- [x] Inspect the current server error/advice chain and web/mobile client paths.
- [x] Establish the initial gaps: no shipped exact-code web catalog, envelope-loss risk, raw mobile
      server-copy fallback, and incomplete generic recovery.
- [x] Capture initial server-producer and high-risk dashboard evidence in `inventory.md`; the full
      BFF and producer inventories remain open until every feature endpoint is classified.
- [~] Establish redaction regression tests before broad migration. The baseline now covers malformed request bodies,
      `decorate(..., verbose=true)`, legacy exception handlers, provider failures, security and RLS
      denials through `GlobalErrorHandler`; assert that response bodies never expose exception/parser/provider prose,
      Java class names, PINs, passwords, tokens, or rejected sensitive values. Extend this to concrete
      provider adapters, telemetry, and a running SecurityFilterChain test once the pre-existing app
      `testCompile` failures are repaired before closing the task.
- [ ] Classify every legacy `ProblemRest`/direct-`ProblemDetail` producer as `STABLE_CODE`,
      `BUSINESS_PROSE`, `HYBRID_OR_DYNAMIC`, or `FRAMEWORK_OR_TECHNICAL`. Record the owner,
      HTTP reachability, conversion outcome, and any temporary bridge in the migration ledger.
- [ ] Decide and document the owner of the existing `BffSlices` helper. Remove the ambiguity that
      places feature-composition policy in `common.web.advice`, and prove a required technical slice
      failure cannot reach the legacy `IllegalStateException -> 422` mapper.
- [ ] Name and resolve current response-semantic contradictions before new BFF migration:
      `ApiResponse.notFound()` must not emit `SUCCESS` for unusable primary data, and
      `APPROVAL_REQUIRED` must not remain a transport-layer `PENDING` sentinel.
- [ ] Inventory every blocking backend producer: `GlobalErrorHandler`, direct `ProblemDetail`,
      `ProblemRest`/`ProblemRestException` where present, validation, security, idempotency, and
      feature/BFF catches.
- [ ] Inventory every 2xx feedback producer: `ApiResponseContext`, `ApiNotice`, `ServiceStatus`,
      explicit partial responses, and implicit `ApiResponseBodyAdvice` wrapping.
- [ ] Inventory every BFF under `features/**`; record primary/secondary slices, retry scope, and
      classify each dependency as `BLOCKING`, `NON_BLOCKING`, or `BACKGROUND`.
- [ ] Confirm canonical response semantics: `SUCCESS` complete; `SUCCESS_WITH_WARNINGS` complete
      with relevant warning; `PARTIAL` usable primary data with expected optional data unavailable;
      `ProblemDetail` primary data unusable.
- [ ] Confirm only the BFF orchestration layer decides whether a downstream failure blocks the full
      response; downstream HTTP status alone never decides it.
- [ ] Decide whether `CREATED`, `ACCEPTED`, and `PENDING` add genuine envelope meaning beyond HTTP
      semantics; eliminate aliases that do not.
- [ ] Decide whether notices need semantic kind `BUSINESS`, `DEGRADATION`, `INFORMATION`; reserve
      `ServiceStatus` for actual service/capability health, not UI sections.
- [ ] Establish the shared owner model: `shell`, `page`, `section`, `form`, `field`, `feature`.
      Each feedback item has exactly one renderer; normalization never implies rendering.
- [ ] Document trusted visible inputs (stable code, approved parameters, explicit category fallback)
      and diagnostic-only inputs (exception/detail/provider/HTTP prose, stack trace, arbitrary meta).
- [ ] Scope the `ProblemDetail` guarantee to application JSON endpoints and document explicit
      exceptions/adapters for proxy/CORS failures, binary downloads, SSE/WebSocket, and empty/HEAD
      responses. Do not promise a JSON body where the transport cannot carry one.

## Phase 1 — Executable server contract and migration guard

- [x] Establish Wave 0 for authenticated request context: `tenant`, user, seller-terminal, and
      operational-context failures now use owned descriptors rather than hybrid prose in `detail`.

- [x] Establish an additive code-first descriptor baseline in `common.web.error` and a tested
      `ProblemRest` factory, without changing legacy message-first call sites.
- [x] Migrate the first critical POS producer: receipt print-profile validation now emits
      `pos.receipt.print_options_invalid` without exposing the underlying exception prose.
- [x] Normalize framework validation failures to stable field violations without serializing Bean
      Validation or request-deserialization prose; retain only code, field, and target.
- [ ] Reconcile the related `complete-apiresponse-notices` OpenSpec with production code and archive
      it only after its tasks/tests match reality.
- [x] Define common technical contract types as needed: `ErrorCode`, `ErrorCategory`,
      `ErrorDescriptor`, `ErrorSeverity`, `ErrorRetryPolicy`, and typed `ErrorParamSpec`; keep concrete business codes in
      their owning package (`core`, `platform`, or `features`).
- [ ] Define lowercase dotted code syntax, stable owner/category/retry policy, uniqueness and expected
      HTTP status where applicable. Reject vague codes, HTTP prose, Java class names, and inline
      string invention.
- [x] Define public parameter specs per descriptor: approved name, primitive/display-safe value type,
      optional formatting semantics, and audience. Reject or drop-and-log every unknown, unsafe,
      object, internal-ID, credential, token, PIN, provider, or SQL parameter before serialization.
- [x] Keep `retryable` as a derived presentation property but make `ErrorRetryPolicy` canonical:
      distinguish no retry, user correction, reauthentication, delayed retry, and retry of the same
      idempotent intent. Clients must never automatically retry a non-idempotent sale mutation.
- [ ] Replace the message-first `ProblemRest`/`ProblemRestException` contract through a compatible
      code-first factory path. Convert each ledger class deliberately: register existing stable codes,
      mint owner codes for business prose, split hybrid/dynamic strings into code plus approved safe
      parameters or server-only diagnostics, and map framework/technical producers explicitly.
- [ ] Add an owner-distributed descriptor registry/collector validated at startup or test time and an
      ArchUnit/static guard against raw externally visible codes in controllers/services. Block new
      message-first producers with a baseline allowlist that only shrinks.
- [ ] Ensure every `ProblemDetail` has code, status, title, detail, instance, request/trace/span IDs
      when available, and generated error ID when applicable. These prose fields are diagnostic-only.
- [~] Prevent serialization of stacks, nested exception/provider prose, credentials, or enumeration
      signals from security/identity/tenant failures. The global legacy-problem firewall and handler
      regression tests are in place; owner migrations and authentication enumeration tests remain.
- [ ] Add locale fixtures and CI parity for every first migrated product-visible code in `ht`, `fr`,
      and `en`. A new registered code cannot rely only on a category fallback.
- [ ] Extend the stable validation shape with approved safe parameters where a specific form needs
      them; Bean-validation text is never the client translation contract.
- [ ] Provide approved generic and domain field codes; remove detail-text inference once producers
      have migrated.

## Phase 2 — ApiResponse notices and service metadata

- [ ] Review every `ApiNotice`/`ServiceStatus` field and mark user-visible versus diagnostic-only.
      `ApiNotice.message` and `ServiceStatus.message` must not be client display contracts.
- [ ] Define safe notice fields: stable code, severity, domain/source, optional stable functional
      target, safe parameters, retryability, and trace metadata. Do not use framework component IDs
      or backend UI placement (`PAGE`, `FORM`, etc.).
- [ ] Separate safe parameters from arbitrary metadata and reject/redact PINs, passwords, tokens,
      customer payloads, SQL/provider payloads, and sensitive internal identifiers.
- [ ] Replace free-form notice metadata with typed `kind`, functional `target`, public params,
      retry policy, and structured correlation. A target is a stable feature/slice key, never an
      internal service, class, component, or visual placement. Remove `message` and `meta` only
      after the migration ledger reaches zero.
- [ ] Add approved helpers such as business/degradation/information notices and service
      down/degraded states; attach correlation metadata and deduplicate deterministically.
- [ ] Implement deterministic status precedence: explicit `PENDING`, then `PARTIAL`, then
      `SUCCESS_WITH_WARNINGS`, then `SUCCESS`; test notices and services together.

## Phase 3 — BFF orchestration and compatibility

- [ ] Implement the Phase 0 ownership decision for BFF aggregation. Keep transport normalization in
      `common`; keep feature slice classification and required/optional policy in a feature-owned
      composition support module. Do not introduce a fake async bus abstraction.
- [ ] Ensure required slices stop composition and preserve/map to an approved stable error code.
- [ ] Ensure optional failures retain primary data, produce one degradation notice per functional
      slice, return `PARTIAL`, and represent independently recoverable sections deterministically
      (`AVAILABLE`, `EMPTY`, `UNAVAILABLE`) rather than relying on nullable business values.
- [ ] Resolve response status from meaning, not merely any warning: `PARTIAL` only when expected
      response data is unavailable; capability degradation without missing data is
      `SUCCESS_WITH_WARNINGS`. `PENDING` is explicit handler intent and its HTTP status is decided
      per operation, never inferred by advice.
- [ ] Keep empty business data distinct from a failed optional section; avoid forced full-page reload
      when an individual section can be retried.
- [ ] Preserve root-cause classification through redacted structured observability only (stable code,
      correlation IDs, and approved diagnostic type); prevent duplicate notices/service floods from
      a single outage; set timeouts only for external calls.
- [ ] Add BFF contract tests for all-success, required failure, single/multiple optional failure,
      empty optional result, warning-without-failure, duplicate failure, and unknown technical error.
- [ ] Review `ApiResponseBodyAdvice` exclusions, ThreadLocal cleanup on success/failure/async, 201/
      202/204 behavior, and legacy auto-wrap compatibility. Prefer explicit envelopes for new BFFs.

## Phase 4 — Shared fixtures, observability, and privacy hardening

- [~] Publish the first versioned blocking-error fixtures. Broaden fixture consumption in the
      server, web, and mobile transport tests to validation, warning, partial, malformed envelope,
      and void cases before treating fixture coverage as complete.

- [ ] Publish versioned JSON fixtures for blocking errors, validation failures, successful warnings,
      partial BFF results, malformed envelopes, and void responses; share fixtures, not runtime code,
      between Java, TypeScript, and Dart.
- [ ] Standardize requestId/traceId/spanId/errorId names and body/header precedence. Remove ambiguous
      aliases and the mobile bug that assigns request ID to trace ID.
- [ ] Define support-reference copy format and structured diagnostics: stable code, route/screen,
      owner, trace IDs, retry count, and permitted tenant/slice context only.
- [ ] Define client diagnostics as redacted structured context only. Raw server `title`, `detail`,
      notice message, exception text, and response snippets are not retained in production client
      state, support references, or telemetry.
- [ ] Extend the Phase 0 redaction suite to telemetry, screenshots, support references, auth/tenant
      enumeration, and RLS/access-denied behavior. The baseline must remain green as producers move.

## Phase 5 — Angular envelope retention and normalized errors

- [~] Review every `TchBackendClient` data-only method and `unwrapApiResponse()` use. The retained
      `*ApiResponse` family now covers CRUD, paged data, multipart requests, and GET resources
      without breaking legacy return types; consumer migration remains.
- [ ] Mark data-only methods unsuitable for BFFs, dashboards, mutations, and business-notice
      endpoints; add a lint/code-review gate for new callsites.
- [ ] Migrate first POS, login/session, tenant/platform dashboards, Ops, and sensitive forms; retain
      status/notices/services/trace through normal and paged resources.
- [ ] Treat `PARTIAL` as successful degraded state; superseded resource cancellation as silent;
      malformed 2xx envelope as stable client invalid-response failure.
- [~] Evolve `WebAppError` to retain code/category/origin/status/retryability/owner/dedupe key and
      structured public params, not final translated prose. Initial factories no longer retain raw
      ProblemDetail, notice, violation, or service text in client state; the complete key-based
      model and removal of fallback copy remain.
- [ ] Add factories for backend `ProblemDetail`, violations, notice/service states, and client-origin
      network/timeout/cancellation/invalid response/auth/storage/print/frontend failures.
- [ ] Align the closed category vocabulary across server and clients, including a client network
      category, and use payload descriptor metadata rather than substring/status inference for known
      codes.
- [ ] Remove hardcoded French and all raw `ProblemDetail.title/detail` or `ApiNotice.message`
      fallbacks. `WebAppError` must not render a raw code as its title either; descriptor lookup must
      replace substring categorization and the temporary `legacyDetailCode` bridge must only shrink.

## Phase 6 — Angular transport boundary and ownership routing

- [ ] Keep `problemDetailInterceptor` transport-only; handle valid/invalid ProblemDetail, text/HTML,
      empty errors, network, timeout, cancellation, and correlation headers without displaying copy.
- [~] Replace boolean-only `suppressShellFeedback` with explicit feedback context/owner/mode while
      preserving a migration bridge. `TCH_FEEDBACK_CONTEXT` now carries `owner`, `mode`, and
      `target`; legacy calls map to local ownership and PageModel is migrated. A shell interceptor is
      now registered in all three portals and routes only explicit `owner: 'shell'` failures;
      local/silent/inherited-without-owner requests remain non-rendering. Inventory confirms the
      current boolean was not read before this migration, so feature owner adoption and dedupe remain.
      API clients normalize but never choose toast/banner/panel. Interceptor composition now keeps
      the auth retry closer to the transport, so it handles raw 401 responses before the final
      ProblemDetail normalization or shell feedback presentation.
- [ ] Add envelope consumption helper and development diagnostics for unconsumed feedback or duplicate
      ownership; dedupe by code/source/target/owner/correlation, never translated text.
- [~] Implement lifecycle rules: clear on successful reload; retain during retry; clear server field
      error on field edit; clear form summary at submit; retain support reference to recovery/navigation.
      `clearServerFieldErrorsOnEdit` now clears only the edited reactive control and is active for
      business-profile plus seller-terminal create/block; Signal Form controls and remaining forms
      still need adoption.
- [ ] Ensure shell owns only session/global outage/maintenance/global restriction; page owns blocking
      route data; section optional BFF degradation; form and field validation; feature domain notices.

## Phase 7 — Angular forms, recovery, and uncaught errors

- [~] Upgrade field mapping for nested groups/arrays and multiple errors; preserve unknown targets in
      form summary and log them in development without discarding them. The shared router now keeps
      all mapped server errors on a control and returns unknown targets to its caller. The shared
      mobile-first `tch-form-error-summary` now renders safe unconsumed violations and moves focus
      to itself; tenant business-profile is the first consumer. Nested/array paths, development
      diagnostics, and field-change lifecycle remain.
- [ ] Merge local/server validation by stable code without overwriting unrelated validators; provide
      accessible summaries, `aria-describedby`, links, and deterministic focus.
- [~] Deliver mobile-first reusable page/section/form/field/shell feedback surfaces with localized
      copy, owner-declared retry, back, copyable support reference, correct aria-live behavior, and
      no duplicate announcement. The shared `tch-error-panel` and `tch-page-error` now support safe
      support references, owner-declared retry state, and narrow-screen actions. `tch-field-error`
      now translates local validator keys and `tch-form-error-summary` focuses unconsumed violations;
      section/form/shell wiring and duplicate-announcement tests remain.
- [ ] Apply focus policy: page failure heading; restored retry target; mutation confirmation; form
      summary or first invalid field; preserve focus for section degradation.
- [ ] Review Angular global ErrorHandler/route fallback: map unexpected frontend crashes to a stable
      client code, log redacted diagnostics, and do not render raw exception text.

## Phase 8 — Web catalogs and tests

- [x] Restore the i18n coverage gate: it validates referenced keys, locale parity, and forbidden
      placement; the POS confirmation/delivery dialog and common error bundle are now included.
- [ ] Clean the existing duplicate declarations and legacy bundle roots reported by the inventory;
      keep them visible as diagnostics until the ownership migration is complete.
- [ ] Define the exact-code/category/generic namespace and strict lookup order. Add all registered
      backend and client codes to shipped `ht`, `fr`, and `en` bundles with safe interpolation rules.
- [ ] Add CI catalog parity for duplicate/orphan/missing codes and invalid interpolation, scoped by
      descriptor audience/client surface. A known production code missing an applicable `ht`, `fr`,
      or `en` translation must fail CI; unknown runtime code uses translated generic copy.
- [ ] Test public/admin/platform: envelope retention, warning/partial states, transport failures,
      ownership and dedupe, field mapping, focus/recovery, support references, and mobile/desktop
      light/dark rendering.

## Phase 9 — Flutter structured model, envelope retention, and ownership

- [ ] Replace `ApiException.message` as the visible contract with structured code/category/origin/
      status/retryability/correlation and diagnostic-only detail; map Dio/network/client failures to
      stable client codes.
- [ ] Parse backend violations and all correlation headers independently; test malformed payloads and
      matching header/body precedence.
- [ ] Introduce/complete typed `ApiResponse<T>` retention in repositories for data/status/notices/
      services/trace. Keep a deliberate data-only helper only for endpoints guaranteed feedback-free.
- [ ] Stop globally displaying every 2xx notice in `ApiNoticeInterceptor`; it parses/attaches response
      feedback, while shell/page/section/form/field/POS routes it to exactly one owner.
- [ ] Use exact local translation first and never raw notice/server/client exception prose. Define
      queue priority/duration so unrelated information cannot displace a POS block.

## Phase 10 — Flutter forms, POS, recovery, and uncaught failures

- [ ] Provide reusable form summary/field mapping with first-invalid focus and scroll on narrow
      screens; route unknown server fields to summary.
- [ ] Add POS recovery for limits/draw/terminal/PIN/idempotency/replay, network retry, print/reprint.
      Sale success must survive print failure; retry keeps idempotency key only for the same intent.
- [ ] Add mobile-first blocking page/section error widgets with localized actions and support
      reference; do not replace a usable page for optional degradation.
- [ ] Review Flutter framework/zone error boundaries, diagnostics/telemetry redaction, and duplicate
      reporting. Add narrow-screen and text-scale widget tests.

## Phase 11 — End-to-end migration and enforcement

- [ ] Migrate critical mobile/POS: login, PIN, prepare, confirm, sell, print/reprint.
- [ ] Migrate critical web: login/session, cashier sale, tenant dashboard, seller-terminal forms,
      limits/pricing, platform dashboard/Ops; then reports, CRUD, and lower-risk legacy screens.
- [ ] Maintain an inventory for each endpoint/screen: envelope retained, stable code, translations,
      owner, recovery, and test coverage.
- [ ] Add static/CI gates against raw prose in UI, inline external codes, data-only BFF unwrap,
      global notice interception, incomplete locales, and missing BFF slice matrix.
- [ ] Remove migration bridges only when the inventory reaches zero: legacy service health, detail
      inference, raw French fallbacks, `ApiException.message` rendering, blanket mobile notices, and
      obsolete suppression option.

## Phase 12 — Documentation and Definition of Done

- [ ] Update server API-response conventions, BFF feature rules, web HTTP/state/error conventions,
      mobile API/error conventions, and the PR checklist with blocking/partial/warning/validation/
      network/print examples.
- [ ] Add a troubleshooting guide for missing translations, duplicate feedback, and safe support
      diagnostics.
- [ ] Verify Definition of Done: stable codes; retained envelopes; complete `ht/fr/en` client copy;
      exactly one rendering owner; no raw prose; deterministic field/page/section recovery and focus;
      redacted correlation; and BFF slice/recovery tests required for new endpoints.
