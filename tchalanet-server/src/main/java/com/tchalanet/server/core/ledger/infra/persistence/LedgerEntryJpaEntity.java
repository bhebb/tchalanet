package com.tchalanet.server.core.ledger.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.ledger.domain.model.LedgerDirection;
import com.tchalanet.server.core.ledger.domain.model.LedgerRefType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entry")
@Getter
@Setter
public class LedgerEntryJpaEntity extends BaseTenantEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", nullable = false, length = 64)
    private LedgerRefType refType;

    @Column(name = "ref_id", nullable = false)
    private UUID refId;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 8)
    private LedgerDirection direction;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
