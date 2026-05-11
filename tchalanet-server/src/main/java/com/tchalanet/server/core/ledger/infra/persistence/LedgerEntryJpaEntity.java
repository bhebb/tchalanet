package com.tchalanet.server.core.ledger.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.ledger.domain.model.LedgerDirection;
import com.tchalanet.server.core.ledger.domain.model.LedgerOperationType;
import com.tchalanet.server.core.ledger.domain.model.LedgerRefType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "ledger_entry",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_ledger_entry_tenant_ref_op",
            columnNames = {"tenant_id", "ref_type", "ref_id", "operation_type"})
    },
    indexes = {
        @Index(
            name = "ix_ledger_entry_tenant_occurred",
            columnList = "tenant_id, occurred_at"),
        @Index(
            name = "ix_ledger_entry_tenant_ref",
            columnList = "tenant_id, ref_type, ref_id")
    })
@Getter
@Setter
public class LedgerEntryJpaEntity extends BaseTenantEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", nullable = false, length = 64)
    private LedgerRefType refType;

    @Column(name = "ref_id", nullable = false)
    private UUID refId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 64)
    private LedgerOperationType operationType;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 8)
    private LedgerDirection direction;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "reversal_of_entry_id")
    private UUID reversalOfEntryId;

    @Column(name = "reason", length = 255)
    private String reason;
}
