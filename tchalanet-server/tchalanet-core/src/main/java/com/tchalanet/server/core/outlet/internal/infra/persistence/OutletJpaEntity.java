package com.tchalanet.server.core.outlet.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.outlet.internal.domain.model.OutletStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "outlet")
@Getter
@Setter
@Audited
public class OutletJpaEntity extends BaseTenantEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false)
    private String slug;

    @Column(name = "day_closed", nullable = false)
    private boolean dayClosed = false;

    @Column(name = "sales_blocked", nullable = false)
    private boolean salesBlocked = false;

    @Column(name = "sales_block_reason")
    private String salesBlockReason;

    @Column(name = "sales_blocked_at")
    private Instant salesBlockedAt;

    @Column(name = "timezone", nullable = false)
    private String timezone = "America/Port-au-Prince";

    @Column(name = "receipt_printing_enabled", nullable = false)
    private boolean receiptPrintingEnabled = true;

    @Column(name = "receipt_header_message")
    private String receiptHeaderMessage;

    @Column(name = "receipt_footer_message")
    private String receiptFooterMessage;

    @Column(name = "require_opening_float", nullable = false)
    private boolean requireOpeningFloat = true;

    @Column(name = "auto_session_open_enabled", nullable = false)
    private boolean autoSessionOpenEnabled = false;

    @Column(name = "auto_session_close_enabled", nullable = false)
    private boolean autoSessionCloseEnabled = false;

    @Column(name = "session_open_time")
    private LocalTime sessionOpenTime;

    @Column(name = "session_close_time")
    private LocalTime sessionCloseTime;

    @Column(name = "default_opening_float_cents")
    private Long defaultOpeningFloatCents;

    @Column(name = "status", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private OutletStatus status = OutletStatus.ACTIVE;

    @Column(name = "payout_blocked", nullable = false)
    private boolean payoutBlocked = false;

    @Column(name = "payout_block_reason")
    private String payoutBlockReason;

    @Column(name = "payout_blocked_at")
    private Instant payoutBlockedAt;

    @Column(name = "payout_blocked_by")
    private UUID payoutBlockedBy;

    @Column(name = "offline_sales_blocked", nullable = false)
    private boolean offlineSalesBlocked = false;

    @Column(name = "offline_sales_block_reason")
    private String offlineSalesBlockReason;

    @Column(name = "offline_sales_blocked_at")
    private Instant offlineSalesBlockedAt;

    @Column(name = "offline_sales_blocked_by")
    private UUID offlineSalesBlockedBy;

    @Column(name = "address_id")
    private UUID addressId;
}
