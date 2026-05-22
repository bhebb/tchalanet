package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Technical outbox table for offline-sync domain events.
 *
 * <p>Intentionally does <strong>not</strong> extend {@code BaseTenantEntity} /
 * {@code AuditableEntity}: this table has no user-audit columns ({@code created_by},
 * {@code updated_by}, {@code deleted_at} …) and no soft-delete lifecycle — it is written
 * once, drained once, and expires. Inheriting the audit superclass would require
 * spurious DDL columns and pollute Hibernate's SQL.
 */
@Getter
@Setter
@Entity
@Table(name = "offline_event_outbox")
public class OfflineEventOutboxJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = "event_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID eventId;

    @Column(name = "event_class", nullable = false, updatable = false)
    private String eventClass;

    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    private String payloadJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
