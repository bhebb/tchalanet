# Tasks — Consolidate Platform Cross-cutting Services

## 1. OpenSpec and Documentation

- [x] Rewrite the change as consolidation/guardrails instead of package extraction.
- [x] Create or update canonical spec `openspec/specs/platform-accesscontrol/spec.md`.
- [x] Create or update canonical spec `openspec/specs/platform-audit/spec.md`.
- [x] Create or update canonical spec `openspec/specs/platform-idempotence/spec.md`.
- [x] Update near-code docs:
  - `platform/accesscontrol/PLATFORM_ACCESSCONTROL.md`
  - `platform/audit/PLATFORM_AUDIT.md`
  - `platform/idempotence/PLATFORM_IDEMPOTENCY.md`

## 2. Architecture Guardrails

- [x] Confirm ArchUnit blocks imports of `platform.*.internal..` outside the owning capability.
- [x] Add/confirm rule: `platform` must not depend on `core` or `features`.
- [x] Add/confirm rule: `common` must not contain functional audit/access-control/idempotency persistence.
- [x] Add/confirm rule: accesscontrol does not import core/features domain packages.
- [x] Add/confirm rule: idempotence handler keys are code-owned constants, not client-provided.

## 3. Access Control Hardening

- [x] Verify `AccessControlApi` is the only public Java surface used outside `platform.accesscontrol`.
- [x] Verify `TchPermissionEvaluator` denies safely when actor, tenant or permission facts are missing.
- [x] Verify method-security coverage for sensitive tenant/admin/platform write endpoints.
- [x] Verify role/permission write endpoints are functionally audited.
- [x] Add focused tests for allow/deny and tenant isolation.
- [x] Document that accesscontrol never validates business resource state.

## 4. Audit Separation

- [x] Document functional audit vs Envers/technical revision audit.
- [x] Verify functional audit writes use the platform audit service/API, not common persistence.
- [x] Verify success audit is emitted after commit where applicable.
- [x] Verify failure/denied audit can be persisted in a separate transaction.
- [x] Verify audit failures do not rollback successful business operations.
- [x] Add focused tests for success-after-commit and failure isolation.

## 5. Idempotence Reliability

- [x] Verify `@RequireIdempotency` rejects missing keys on required endpoints.
- [x] Verify same key + same payload replay does not execute the command twice.
- [x] Verify same key + different payload returns `idempotency.payload_mismatch`.
- [x] Verify in-progress duplicate returns `idempotency.in_progress`.
- [x] Verify processed-event duplicate delivery skips side effects.
- [x] Apply/confirm sell-ticket idempotency requirement.
- [x] Add cleanup/expiry behavior documentation and tests.

## 6. Migration and Acceptance

- [x] Inventory remaining legacy/common cross-cutting classes and decide keep/move/remove.
- [ ] Run OpenSpec validation for this change.
- [ ] Run focused tests for accesscontrol/audit/idempotence where compile state allows.
- [ ] Mark tasks complete only when the guardrail or test is actually present.
