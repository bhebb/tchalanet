package com.tchalanet.server.core.terminal.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalCapability;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "terminal_capability")
@Getter
@Setter
public class TerminalCapabilityJpaEntity extends BaseTenantEntity {

    @Column(name = "terminal_id", nullable = false)
    private UUID terminalId;

    @Column(name = "capability", nullable = false, length = 64)
    @Enumerated(EnumType.STRING)
    private TerminalCapability capability;
}
