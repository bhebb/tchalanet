package com.tchalanet.server.core.session.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.session.domain.model.PosSessionStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;


@Entity
@Table(name = "pos_session")
@Audited
@Getter
@Setter
public class PosSessionJpaEntity extends BaseTenantEntity {

    @Column(name = "outlet_id", nullable = false)
    private UUID outletId;

    @Column(name = "terminal_id", nullable = false)
    private UUID terminalId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PosSessionStatus status;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "opening_float", precision = 14, scale = 2)
    private BigDecimal openingFloat;

    @Column(name = "closing_amount", precision = 14, scale = 2)
    private BigDecimal closingAmount;

    @Column(name = "meta", columnDefinition = "jsonb", nullable = false)
    private String metaJson = "{}";
}
