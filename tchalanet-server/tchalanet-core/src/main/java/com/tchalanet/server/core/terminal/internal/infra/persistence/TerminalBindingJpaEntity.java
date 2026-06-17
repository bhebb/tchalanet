package com.tchalanet.server.core.terminal.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalBindingStatus;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalBindingType;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalPublicKeyAlgorithm;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "terminal_binding")
@Getter
@Setter
public class TerminalBindingJpaEntity extends BaseTenantEntity {

    @Column(name = "terminal_id", nullable = false)
    private UUID terminalId;

    @Column(name = "binding_type", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private TerminalBindingType bindingType;

    @Column(name = "status", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private TerminalBindingStatus status;

    @Column(name = "binding_public_key")
    private String bindingPublicKey;

    @Column(name = "public_key_algorithm", length = 32)
    @Enumerated(EnumType.STRING)
    private TerminalPublicKeyAlgorithm publicKeyAlgorithm;

    @Column(name = "public_key_hash")
    private String publicKeyHash;

    @Column(name = "credential_hash", nullable = false)
    private String credentialHash;

    @Column(name = "device_fingerprint_hash")
    private String deviceFingerprintHash;

    @Column(name = "bound_by")
    private UUID boundBy;

    @Column(name = "bound_at", nullable = false)
    private Instant boundAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
}
