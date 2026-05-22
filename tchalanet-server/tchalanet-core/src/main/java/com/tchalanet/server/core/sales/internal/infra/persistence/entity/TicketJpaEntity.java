package com.tchalanet.server.core.sales.internal.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintStateStatus;
import com.tchalanet.server.core.sales.api.model.status.OfflineSyncStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketPrintStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for the {@link Ticket}
 * aggregate root.
 *
 * <p>Inherits from {@link BaseTenantEntity} which provides {@code id},
 * {@code tenant_id}, {@code version}, technical audit columns, and {@code deleted_at}.
 *
 * <p>Soft-delete is enabled at the ticket level via {@link SQLDelete} +
 * {@link SQLRestriction}. The DELETE statement is guarded by {@code version}
 * to interact correctly with optimistic locking.
 *
 * <p>Tenant isolation is enforced by the RLS policy on {@code sales_ticket}
 * (see {@code V20260515__create_sales_ticket.sql}). No application-side tenant
 * filtering is required.
 *
 * <p>Envers is enabled via {@link Audited} for business audit history.
 */
@Entity
@Audited
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "sales_ticket",
    indexes = {
        @Index(name = "idx_sales_ticket_tenant",
            columnList = "tenant_id"),
        @Index(name = "idx_sales_ticket_tenant_draw",
            columnList = "tenant_id, draw_id"),
        @Index(name = "idx_sales_ticket_tenant_session",
            columnList = "tenant_id, sales_session_id"),
        @Index(name = "idx_sales_ticket_tenant_seller",
            columnList = "tenant_id, seller_user_id"),
        @Index(name = "idx_sales_ticket_tenant_outlet",
            columnList = "tenant_id, outlet_id"),
        @Index(name = "idx_sales_ticket_tenant_terminal",
            columnList = "tenant_id, terminal_id"),
        @Index(name = "idx_sales_ticket_tenant_sale_status",
            columnList = "tenant_id, sale_status"),
        @Index(name = "idx_sales_ticket_tenant_result_status",
            columnList = "tenant_id, result_status"),
        @Index(name = "idx_sales_ticket_tenant_settlement_status",
            columnList = "tenant_id, settlement_status"),
        @Index(name = "idx_sales_ticket_offline_submission",
            columnList = "tenant_id, offline_submission_id"),
        @Index(name = "idx_sales_ticket_sold_at_desc",
            columnList = "tenant_id, sold_at DESC")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_sales_ticket_tenant_code",
            columnNames = {"tenant_id", "ticket_code"}
        ),
        @UniqueConstraint(
            name = "uk_sales_ticket_public_code",
            columnNames = {"tenant_id", "public_code"}
        ),
        @UniqueConstraint(
            name = "uk_sales_ticket_verification_code",
            columnNames = {"tenant_id", "verification_code"}
        )
    }
)
public class TicketJpaEntity extends BaseTenantEntity {

    // -------------------- Context --------------------

    @Column(name = "outlet_id", nullable = false, columnDefinition = "uuid")
    private UUID outletId;

    @Column(name = "terminal_id", nullable = false, columnDefinition = "uuid")
    private UUID terminalId;

    @Column(name = "seller_user_id", nullable = false, columnDefinition = "uuid")
    private UUID sellerUserId;

    @Column(name = "sales_session_id", nullable = false, columnDefinition = "uuid")
    private UUID salesSessionId;

    @Column(name = "draw_id", nullable = false, columnDefinition = "uuid")
    private UUID drawId;

    @Column(name = "draw_channel_id", nullable = false, columnDefinition = "uuid")
    private UUID drawChannelId;

    // -------------------- Codes --------------------

    @Column(name = "ticket_code", nullable = false, length = 64)
    private String ticketCode;

    @Column(name = "public_code", nullable = false, length = 32)
    private String publicCode;

    @Column(name = "verification_code", nullable = false, length = 32)
    private String verificationCode;

    // -------------------- Money --------------------

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "stake_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal stakeAmount;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "potential_payout_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal potentialPayoutAmount;

    @Column(name = "winning_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal winningAmount;

    // -------------------- Sale lifecycle --------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_status", nullable = false, length = 32)
    private TicketSaleStatus saleStatus;

    @Column(name = "sold_at", nullable = false)
    private Instant soldAt;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    @Column(name = "approval_request_id", columnDefinition = "uuid")
    private UUID approvalRequestId;

    @Column(name = "approval_requested_by", columnDefinition = "uuid")
    private UUID approvalRequestedBy;

    @Column(name = "approval_requested_at")
    private Instant approvalRequestedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by", columnDefinition = "uuid")
    private UUID approvedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejected_by", columnDefinition = "uuid")
    private UUID rejectedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by", columnDefinition = "uuid")
    private UUID cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "voided_by", columnDefinition = "uuid")
    private UUID voidedBy;

    @Column(name = "void_reason", length = 500)
    private String voidReason;

    // -------------------- Result lifecycle --------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 32)
    private TicketResultStatus resultStatus;

    @Column(name = "resulted_at")
    private Instant resultedAt;

    @Column(name = "resulted_by", columnDefinition = "uuid")
    private UUID resultedBy;

    @Column(name = "result_override_reason", length = 500)
    private String resultOverrideReason;

    // -------------------- Settlement lifecycle --------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", nullable = false, length = 32)
    private TicketSettlementStatus settlementStatus;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "settled_by", columnDefinition = "uuid")
    private UUID settledBy;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "paid_by", columnDefinition = "uuid")
    private UUID paidBy;

    @Column(name = "payout_id", columnDefinition = "uuid")
    private UUID payoutId;

    // -------------------- Origin --------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_channel", nullable = false, length = 32)
    private TicketSaleChannel saleChannel;

    // Offline sale ref — all null unless saleChannel == POS_OFFLINE_SYNCED
    @Column(name = "offline_submission_id", columnDefinition = "uuid")
    private UUID offlineSubmissionId;

    @Column(name = "offline_batch_id", columnDefinition = "uuid")
    private UUID offlineBatchId;

    @Column(name = "offline_code_batch_id", columnDefinition = "uuid")
    private UUID offlineCodeBatchId;

    @Column(name = "offline_code", length = 64)
    private String offlineCode;

    @Column(name = "offline_client_sale_id", length = 64)
    private String offlineClientSaleId;

    @Column(name = "offline_local_sequence")
    private Long offlineLocalSequence;

    @Column(name = "offline_sold_at_device")
    private Instant offlineSoldAtDevice;

    @Enumerated(EnumType.STRING)
    @Column(name = "offline_sync_status", length = 48)
    private OfflineSyncStatus offlineSyncStatus;

    // -------------------- Print --------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "print_status", nullable = false, length = 16)
    private TicketPrintStateStatus printStatus;

    @Column(name = "print_count", nullable = false)
    private int printCount;

    @Column(name = "first_printed_at")
    private Instant firstPrintedAt;

    @Column(name = "last_printed_at")
    private Instant lastPrintedAt;

    // -------------------- Lines --------------------

    @OneToMany(
        mappedBy = "ticket",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @OrderBy("lineNumber ASC")
    private List<TicketLineJpaEntity> lines = new ArrayList<>();

    @OneToMany(
        mappedBy = "ticket",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @OrderBy("chargeType ASC")
    private List<TicketChargeJpaEntity> charges = new ArrayList<>();

    public void addCharge(TicketChargeJpaEntity charge) {
        charges.add(charge);
        charge.setTicket(this);
    }

    public void removeCharge(TicketChargeJpaEntity charge) {
        charges.remove(charge);
        charge.setTicket(null);
    }

    public void replaceCharges(List<TicketChargeJpaEntity> newCharges) {
        charges.clear();
        if (newCharges != null) {
            newCharges.forEach(this::addCharge);
        }
    }

    public void addLine(TicketLineJpaEntity line) {
        lines.add(line);
        line.setTicket(this);
    }

    public void removeLine(TicketLineJpaEntity line) {
        lines.remove(line);
        line.setTicket(null);
    }

    public void replaceLines(List<TicketLineJpaEntity> newLines) {
        lines.clear();
        if (newLines != null) {
            newLines.forEach(this::addLine);
        }
    }
}
