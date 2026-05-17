package com.tchalanet.server.core.terminal.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalKind;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "terminal")
@Getter
@Setter
@Audited
public class TerminalJpaEntity extends BaseTenantEntity {

    @Column(name = "outlet_id", nullable = false)
    private UUID outletId;

    @Column(name = "assigned_user_id")
    private UUID assignedUserId;

    @Column(name = "kind", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private TerminalKind kind = TerminalKind.PHYSICAL;

    @Column(name = "state", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private TerminalState state = TerminalState.REGISTERED;

    @Column(name = "auto_session_enabled", nullable = false)
    private boolean autoSessionEnabled = false;

    @Column(name = "sync_state", nullable = false, length = 32)
    private String syncState = "ONLINE";

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Column(name = "label", length = 128)
    private String label;

    @Column(name = "inventory_tag", length = 64)
    private String inventoryTag;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    private JsonNode metadataJson;

    @Column(name = "registered_at")
    private Instant registeredAt;

    @Column(name = "unregistered_at")
    private Instant unregisteredAt;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "locked_by")
    private UUID lockedBy;

    @Column(name = "lock_reason")
    private String lockReason;

    @Column(name = "value", length = 80)
    private String code;

    @Column(name = "sales_blocked", nullable = false)
    private boolean salesBlocked = false;

    @Column(name = "sales_block_reason")
    private String salesBlockReason;

    @Column(name = "sales_blocked_at")
    private Instant salesBlockedAt;

    @Column(name = "sales_blocked_by")
    private UUID salesBlockedBy;

    @Column(name = "payout_blocked", nullable = false)
    private boolean payoutBlocked = false;

    @Column(name = "payout_block_reason")
    private String payoutBlockReason;

    @Column(name = "payout_blocked_at")
    private Instant payoutBlockedAt;

    @Column(name = "payout_blocked_by")
    private UUID payoutBlockedBy;

    @Column(name = "offline_blocked", nullable = false)
    private boolean offlineBlocked = false;

    @Column(name = "offline_block_reason")
    private String offlineBlockReason;

    @Column(name = "offline_blocked_at")
    private Instant offlineBlockedAt;

    @Column(name = "offline_blocked_by")
    private UUID offlineBlockedBy;
}
