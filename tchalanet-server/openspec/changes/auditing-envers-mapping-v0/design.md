# Design: Auditing Envers Mapping V0

## Table mapping

| Table | Envers | Audited fields | Excluded |
|---|---|---|---|
| `seller_terminal` | Partial | `terminal_code`, `status`, `commission_rate`, `odds_profile_id`, `limit_profile_id`, `blocked_at`, `blocked_by`, `blocked_reason`, `disabled_at` | PII (`first_name`, `last_name`, `phone_number`, `address_*`), `last_seen_at`, timestamps, external identity fields |
| `draw_result` | Yes | result slot/date, result payloads, status, source, quality, source hash, override reason | high-volume ticket data |
| `limit_assignment` | Yes | assignment target, limits, status and lifecycle fields | unrelated policy definitions unless separately approved |
| `ticket` | **No** | — | immutable row; void/cancel tracked via `audit_event` |
| `ticket_line` | **No** | — | immutable snapshots |
| `payout` | **No** | — | high-volume; paid/void events tracked via `audit_event` |
| `audit_event` | **No** | — | append-only functional audit |
| `app_user` | **No** | — | PII; lock/disable events in `audit_event` |

## Annotation pattern

Use field-level `@Audited(withModifiedFlag = true)` for partially audited entities.
Never use class-level `@Audited` on entities with PII or high-churn fields. Class-level Envers is
acceptable only for the explicitly allowlisted entities whose fields have been reviewed.

### seller_terminal example

```java
@Entity
@Table(name = "seller_terminal")
public class SellerTerminalJpaEntity {

    @Id
    private UUID id;
    private UUID tenantId;

    // identity — not audited (PII)
    private String firstName;
    private String lastName;
    private String displayName;
    private String phoneNumber;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String region;
    private String country;

    // external identity — not audited (Class B events in audit_event)
    private String externalProvider;
    private String externalIssuer;
    private String externalSubject;

    // control and financial — audited
    @Audited(withModifiedFlag = true)
    private String terminalCode;

    @Audited(withModifiedFlag = true)
    @Enumerated(EnumType.STRING)
    private SellerTerminalStatus status;

    @Audited(withModifiedFlag = true)
    private BigDecimal commissionRate;

    @Audited(withModifiedFlag = true)
    private UUID oddsProfileId;

    @Audited(withModifiedFlag = true)
    private UUID limitProfileId;

    @Audited(withModifiedFlag = true)
    private Instant blockedAt;

    @Audited(withModifiedFlag = true)
    private UUID blockedBy;

    @Audited(withModifiedFlag = true)
    private String blockedReason;

    @Audited(withModifiedFlag = true)
    private Instant disabledAt;

    // activity — not audited (high-churn)
    private Instant lastSeenAt;
    private Instant activatedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
```

## Revision table

`revinfo` is mapped by `platform.entityhistory` with a custom revision entity/listener. Configure via:

```properties
spring.jpa.properties.hibernate.envers.revision_field_name=rev
spring.jpa.properties.hibernate.envers.revision_type_field_name=revtype
```

The revision listener belongs to `platform.entityhistory`, not `platform.audit`.

## Rules

1. No Envers on `ticket`, `ticket_line`, `payout` — performance and volume.
2. No Envers on PII fields by default — no compliance requirement yet.
3. No class-level `@Audited` on partially audited entities.
4. Use `withModifiedFlag = true` on all audited fields for fast change detection.
5. Business commands must still write `audit_event` entries even when Envers is active.
6. `last_seen_at` and activity timestamps are never audited.

## Business audit_event events (mandatory alongside Envers)

```text
SELLER_TERMINAL_CREATE / UPDATE / BLOCK / UNBLOCK / DISABLE / RESET_ACCESS / COMMISSION_CHANGE
ODDS_RULE_CREATE / UPDATE / DISABLE
LIMIT_RULE_CREATE / UPDATE / DISABLE
DRAW_RESULT_CREATE / CONFIRM / CORRECT
PAYOUT_MARK_PAID / VOID
TICKET_VOID
TENANT_OVERRIDE
```
