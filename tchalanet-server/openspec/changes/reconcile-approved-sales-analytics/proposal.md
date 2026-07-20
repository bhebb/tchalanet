# Reconcile Approved Sales With Analytics

## Why

The POS home screen and reporting surface are allowed to read different models, but they must
agree on what constitutes an official sale. The POS live ticket reader previously included every
ticket status while analytics projects only `APPROVED` sales. V0 does not support an approval
workflow: a limit that would otherwise require approval must reject the sale before persistence.

The local database inspection on 2026-07-20 initially returned no source tickets because the
query did not set PostgreSQL RLS tenant context. Re-running the audit with the `family` tenant
context returned 54 directly `APPROVED` tickets and matched the tenant daily projection exactly.
This confirms that operational reconciliation and diagnostics MUST always be tenant-scoped. A
metric that cannot be verified for its requested scope MUST be unavailable to cashier and
administrator surfaces, rather than rendered as zero or as a plausible amount.

## What Changes

- Restrict seller-terminal live daily statistics to `APPROVED` tickets.
- Treat `REQUIRE_APPROVAL` from limit policy as the same blocking business decision as `BLOCK`.
- Ensure the V0 cashier flow can create only directly `APPROVED` sales or reject the request.
- Add reconciliation coverage for direct and approval-path tickets.
- Introduce a read-side analytics trust state so financial KPI consumers can distinguish usable
  data from data awaiting reconciliation.
- Define deterministic metric semantics so `stake`, `total paid`, and `seller commission` are not
  presented under the same ambiguous "sales" label.
- Make POS, tenant-admin dashboard, seller-terminal summary, and reports use `core.analytics`
  projections for KPI/reportable metrics. POS seller-terminal queries must use the same terminal
  dimension as reports and must not separately aggregate `sales_ticket`.

## Non-Goals

- Do not make the POS home query analytics synchronously; transactional POS feedback remains
  immediate while report projections remain after-commit.
- Do not remove historical persistence enum values or lifecycle administration endpoints in this
  change; they are unreachable from the V0 sale path.
- Do not add a Flyway migration.
- Do not silently delete or rewrite projections as a first response to a mismatch. A repair must
  be explicit, auditable, and reproducible from transactional records.

## Safety

- A `REQUIRE_APPROVAL` policy outcome never creates a ticket.
- Pending, rejected, and cancelled tickets do not contribute to seller daily totals or reports.
- When the source and projection cannot be reconciled, consumers receive an unavailable state and
  never substitute a zero-valued KPI.
