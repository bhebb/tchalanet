package com.tchalanet.server.core.terminal.internal.infra.persistence;

import com.tchalanet.server.core.terminal.api.query.TerminalProofPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "terminal_device_nonce",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_terminal_device_nonce",
        columnNames = {"tenant_id", "binding_id", "purpose", "nonce"}
    )
)
@Getter
@Setter
public class TerminalDeviceNonceJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "binding_id", nullable = false, updatable = false)
    private UUID bindingId;

    @Column(name = "nonce", nullable = false, updatable = false)
    private String nonce;

    @Column(name = "purpose", nullable = false, length = 32, updatable = false)
    @Enumerated(EnumType.STRING)
    private TerminalProofPurpose purpose;

    @Column(name = "signed_at", nullable = false, updatable = false)
    private Instant signedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;
}
