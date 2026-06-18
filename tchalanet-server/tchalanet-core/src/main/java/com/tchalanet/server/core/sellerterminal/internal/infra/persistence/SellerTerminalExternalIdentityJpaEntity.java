package com.tchalanet.server.core.sellerterminal.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

// Class B audit — provisioning events tracked in audit_log, not Envers
@Entity
@Table(
    name = "seller_terminal_external_identity",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_seller_terminal_ext_identity",
            columnNames = {"provider", "issuer", "external_subject"}
        )
    }
)
@Getter
@Setter
public class SellerTerminalExternalIdentityJpaEntity extends BaseEntity {

    @Column(name = "seller_terminal_id", nullable = false, columnDefinition = "uuid")
    private UUID sellerTerminalId;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "issuer", nullable = false, length = 512)
    private String issuer;

    @Column(name = "external_subject", nullable = false, length = 255)
    private String externalSubject;
}
