package com.tchalanet.server.core.session.internal.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Audited
@Table(name = "sales_session_offline_adjustment")
public class SalesSessionOfflineAdjustmentJpaEntity {
    @Id
    private UUID id;
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
    @Column(name = "sales_session_id", nullable = false)
    private UUID salesSessionId;
    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;
    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;
    @Column(name = "currency", nullable = false)
    private String currency;
    @Column(name = "source", nullable = false)
    private String source;
    @Column(name = "reason", nullable = false)
    private String reason;
    @Column(name = "occurred_at_device", nullable = false)
    private Instant occurredAtDevice;
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
    @Version
    private Long version;
}
