package com.tchalanet.server.core.sales.internal.infra.persistence.view;

import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.model.status.TicketPrintStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import org.hibernate.annotations.Immutable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-only JPA mapping over sales_ticket_print_header_v.
 *
 * The underlying view is created with security_invoker=true and relies on source-table RLS.
 * This entity is projection-only and must never be used for updates.
 */
@Entity
@Immutable
@Getter
@NoArgsConstructor
@Table(name = "sales_ticket_print_header_v")
public class TicketPrintHeaderViewEntity {

    @Id
    @Column(name = "ticket_id")
    private UUID ticketId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "ticket_code", nullable = false)
    private String ticketCode;

    @Column(name = "public_code", nullable = false)
    private String publicCode;

    @Column(name = "verification_code", nullable = false)
    private String verificationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_status", nullable = false)
    private TicketSaleStatus saleStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false)
    private TicketResultStatus resultStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", nullable = false)
    private TicketSettlementStatus settlementStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "print_status", nullable = false)
    private TicketPrintStatus printStatus;

    @Column(name = "print_count", nullable = false)
    private int printCount;

    @Column(name = "first_printed_at")
    private Instant firstPrintedAt;

    @Column(name = "last_printed_at")
    private Instant lastPrintedAt;

    @Column(name = "draw_id", nullable = false)
    private UUID drawId;

    @Column(name = "draw_date", nullable = false)
    private LocalDate drawDate;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "cutoff_at", nullable = false)
    private Instant cutoffAt;

    @Column(name = "draw_channel_id")
    private UUID drawChannelId;

    @Column(name = "draw_channel_name")
    private String drawChannelName;

    @Column(name = "draw_channel_display_name")
    private String drawChannelDisplayName;

    @Column(name = "outlet_id")
    private UUID outletId;

    @Column(name = "outlet_code")
    private String outletCode;

    @Column(name = "outlet_name")
    private String outletName;

    @Column(name = "outlet_receipt_header")
    private String outletReceiptHeader;

    @Column(name = "outlet_receipt_footer")
    private String outletReceiptFooter;

    @Column(name = "terminal_id")
    private UUID terminalId;

    @Column(name = "terminal_code")
    private String terminalCode;

    @Column(name = "terminal_label")
    private String terminalLabel;

    @Column(name = "sales_session_id")
    private UUID salesSessionId;

    @Column(name = "session_code")
    private String sessionCode;

    @Column(name = "seller_user_id", nullable = false)
    private UUID sellerUserId;

    @Column(name = "seller_display_name")
    private String sellerDisplayName;

    @Column(name = "tenant_display_name")
    private String tenantDisplayName;

    @Column(name = "tenant_receipt_header")
    private String tenantReceiptHeader;

    @Column(name = "tenant_receipt_footer")
    private String tenantReceiptFooter;

    @Column(name = "stake_amount", nullable = false)
    private BigDecimal stakeAmount;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "potential_payout_amount", nullable = false)
    private BigDecimal potentialPayoutAmount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_origin", nullable = false)
    private TicketSaleChannel saleOrigin;

    @Column(name = "locale")
    private String locale;

    @Column(name = "timezone")
    private String timezone;
}

