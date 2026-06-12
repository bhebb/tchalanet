# Ticket Archive — Design Document

> **Status**: DESIGN — implementation must not start until this document is reviewed.
> **Phase**: 7G (data-lifecycle-archive-v1 follow-up)
> **Prerequisite**: audit_log archive E2E (Phase 7C) must be proven first.

---

## 1. Complexity

A ticket is a multi-table entity:

| Table | Role |
|---|---|
| `ticket` | Header: public_code, sold_at, status, totals |
| `ticket_line` | One per selection (game, bet type, option, stake) |
| `ticket_charge` | Fees and adjustments |
| `ticket_promotion_snapshot` | Applied promotion rules at time of sale |
| (future) settlement/result summary | Win/loss outcome |

A single ticket_id may span rows across all four tables.
Ticket archive retrieval must reassemble these into an `ArchivedTicketView` without requiring a scan of an entire monthly archive object.

---

## 2. Archive object grouping strategy — V1 recommendation

**Chosen: normalized table exports + lookup-based assembly**

One archive object per (table, tenant, month). No denormalization.

```
archive/prod/ticket/tenant-{id}/2026/01/{segment}.jsonl.gz          ← ticket headers
archive/prod/ticket_line/tenant-{id}/2026/01/{segment}.jsonl.gz     ← lines
archive/prod/ticket_charge/tenant-{id}/2026/01/{segment}.jsonl.gz   ← charges
```

Retrieval: look up `ticket_id` in `archive_lookup_index`, get the header object,
then derive line/charge object IDs from a manifest or by secondary lookup.

**Why not denormalized documents?**

- Monthly objects for tickets alone can reach hundreds of millions of rows.
- Denormalized documents duplicate promotion/charge data per ticket.
- Normalized export reuses the existing `streamByPeriod` pattern already proven for `audit_log`.

---

## 3. Lookup index strategy

| Lookup key | archive_lookup_index entry |
|---|---|
| `ticket_id` | `entity_type=TICKET`, `entity_id=ticket_id`, points to ticket header object |
| `public_code` | `public_code=...`, `entity_type=TICKET`, same object |
| `business_date` | `business_date=...`, `entity_type=TICKET`, object covering that date |

One lookup entry per ticket per archive object. A monthly tenant shard of 5M tickets
generates 5M lookup rows — acceptable given `archive_lookup_index` uses RLS and is indexed
on `(tenant_id, entity_type, entity_id, occurred_at DESC)`.

For `ticket_line`, `ticket_charge`: these do NOT get individual lookup entries in V1.
Lines are fetched by scanning the ticket_line archive object for rows where `ticket_id`
matches. This requires reading the entire monthly object, but:
- Lines-per-ticket is bounded (typically 1-6).
- The object is already fetched for the header.
- A secondary lookup index on `ticket_id` within ticket_line objects is a V2 optimization.

---

## 4. Archive object manifest

To avoid scanning ticket_line and ticket_charge archives when only the header is needed,
archive objects carry an `archive_object_group` concept (V2):

```
archive_object.group_id   (nullable UUID, links related objects for the same period+tenant)
```

V1 relies on the lookup index only. No group_id in V1.

---

## 5. Archived ticket DTO

```java
public record ArchivedTicketView(
    boolean archived,
    String source,              // "ARCHIVE"
    UUID ticketId,
    String publicCode,
    UUID tenantId,
    LocalDate businessDate,
    Instant soldAt,
    String status,
    long grossStakeCents,
    long netStakeCents,
    long potentialWinCents,
    List<ArchivedTicketLineView> lines,
    List<ArchivedTicketChargeView> charges,
    ArchiveMetaView archiveMeta  // objectId, periodStart, periodEnd, schemaVersion
) {}
```

Line view includes: `gameCode`, `betType`, `betOption`, `selectionKey`, `stakeCents`, `odds`, `result`.
Charge view includes: `chargeType`, `amountCents`.

Object URI is **never** included in any view returned to tenant/admin.

---

## 6. Retrieval path

```
1. Query hot ticket table by (tenantId, ticketId) or (tenantId, publicCode).
2. If found → return normal TicketView.
3. If not found:
   a. Query archive_lookup_index WHERE entity_type='TICKET' AND entity_id=ticketId
      (or public_code=publicCode) AND tenant_id=tenantId.
   b. Fetch ticket header object from storage.
   c. Scan header object for matching ticket row.
   d. Fetch ticket_line object for same tenant+period.
   e. Scan ticket_line object for rows WHERE ticket_id = ticketId.
   f. Fetch ticket_charge object for same tenant+period.
   g. Scan ticket_charge object for rows WHERE ticket_id = ticketId.
   h. Assemble ArchivedTicketView.
   i. Return with archived=true, source="ARCHIVE".
```

Step 3d-g requires knowing which ticket_line/ticket_charge objects cover the same period.
In V1 this is derived from the header object's `period_start` / `period_end` + tenant_id
by querying `archive_object` (not via lookup_index).

---

## 7. SalesTicketArchiveDatasetProvider implementation plan

The provider lives in `core.sales.internal.infra.archive`.

`plan()`:
- Count `ticket` rows for (tenantId, period) — single SQL count on hot table.
- Count `ticket_line` rows — approximate, not required for plan.

`export()`:
- Stream `ticket` rows via `RowSink`, one object per table.
- A separate export call for `ticket_line`, `ticket_charge` etc.

`generateLookupRows()`:
- One `ArchiveLookupEntry` per distinct `ticket_id`:
  - `entityType = "TICKET"`
  - `entityId = ticket_id`
  - `publicCode = public_code`
  - `businessDate = business_date`
  - `occurredAt = sold_at`

---

## 8. Tenant isolation requirements

- `archive_lookup_index` RLS ensures tenantId scoping at lookup.
- Archive object URIs never exposed to tenant/admin callers.
- Assembly service must validate that every row retrieved from the object has
  `tenant_id` matching the request context.
- Platform restores into `platform_archive_restore_ticket` require SUPER_ADMIN + reason.

---

## 9. Object sizing

Target: 256 MB – 512 MB compressed per object.

For a high-volume tenant (>5M tickets/month), segmented export is needed.
`ArchiveRunExecutor` already supports `segmentNo` in `ArchiveExportRequest` and
`ArchiveObjectJdbcRepository.insert()` stores `segment_no`.

V1: one segment per table per tenant per month (no mid-month splits).
V2: segment splitting when `targetCompressedObjectBytes` is exceeded.

---

## 10. Acceptance criteria before implementation starts

- [ ] `ArchivedTicketView` record defined and reviewed.
- [ ] Object grouping strategy reviewed (normalized, no denormalization).
- [ ] Lookup index entry design reviewed (per ticket, not per line).
- [ ] Object URI leak prevention verified in tests.
- [ ] audit_log archive E2E confirmed working in staging.
