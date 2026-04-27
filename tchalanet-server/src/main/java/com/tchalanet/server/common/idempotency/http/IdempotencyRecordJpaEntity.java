package com.tchalanet.server.common.idempotency.http;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.common.types.enums.IdempotencyScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "idempotency_record",
    uniqueConstraints = {
        @UniqueConstraint(name = "ux_idem_tenant_scope_key", columnNames = {"tenant_id", "scope", "idem_key"})
    })
@Getter
@Setter
public class IdempotencyRecordJpaEntity extends BaseTenantEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 80)
    private IdempotencyScope scope;

    @Column(name = "idem_key", nullable = false, length = 200)
    private String key;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "response_json", columnDefinition = "jsonb")
    private String responseJson;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public enum Status {
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

}


