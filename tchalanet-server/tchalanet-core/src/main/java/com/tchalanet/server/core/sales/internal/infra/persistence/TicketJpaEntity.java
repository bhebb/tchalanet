package com.tchalanet.server.core.sales.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.common.types.enums.TicketResultStatus;
import com.tchalanet.server.common.types.enums.TicketSaleStatus;
import com.tchalanet.server.common.types.enums.TicketSettlementStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ticket")
@Getter
@Setter
@Audited
public class TicketJpaEntity extends BaseTenantEntity {

    @Column(name = "terminal_id", nullable = false)
    private UUID terminalId;

    @Column(name = "draw_id", nullable = false)
    private UUID drawId;

    @Column(name = "ticket_code", nullable = false)
    private String ticketCode;

    @Column(name = "public_code", nullable = false, length = 32)
    private String publicCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_status", nullable = false, length = 24)
    private TicketSaleStatus saleStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 24)
    private TicketResultStatus resultStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", nullable = false, length = 24)
    private TicketSettlementStatus settlementStatus;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "winning_amount", precision = 14, scale = 2)
    private BigDecimal winningAmount;

    @Column(name = "resulted_at")
    private Instant resultedAt;

    @Column(name = "approval_request_id")
    private UUID approvalRequestId;

    @Column(name = "outlet_id", nullable = false)
    private UUID outletId;

    @Column(name = "seller_user_id", nullable = false)
    private UUID sellerUserId;

    @Column(name = "sales_session_id", nullable = false)
    private UUID salesSessionId;

    @Column(name = "draw_channel_id")
    private UUID drawChannelId;

    @Column(name = "verification_code", nullable = false)
    private String verificationCode;

    @Column(name = "stake_amount", nullable = false)
    private BigDecimal stakeAmount;

    @Column(name = "fee_amount", nullable = false)
    private BigDecimal feeAmount;

    @Column(name = "potential_payout_amount", nullable = false)
    private BigDecimal potentialPayoutAmount;

    @Column(name = "sale_origin", nullable = false)
    private String saleOrigin;

    @Column(name = "sync_status", nullable = false)
    private String syncStatus;

    @Column(name = "offline_submission_id")
    private UUID offlineSubmissionId;

    @Column(name = "offline_batch_id")
    private UUID offlineBatchId;

    @Column(name = "offline_code_batch_id")
    private UUID offlineCodeBatchId;

    @Column(name = "offline_code")
    private String offlineCode;

    @Column(name = "client_ticket_id")
    private String clientTicketId;

    @Column(name = "local_sequence")
    private Long localSequence;

    @Column(name = "created_at_device")
    private Instant createdAtDevice;

    @Column(name = "synced_at")
    private Instant syncedAt;

    @Column(name = "sold_at", nullable = false)
    private Instant soldAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "paid_by")
    private UUID paidBy;

    // Ticket is the aggregate root and TicketLine has no independent lifecycle.
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TicketLineJpaEntity> lines = new ArrayList<>();

    public void addLine(TicketLineJpaEntity line) {
        lines.add(line);
        line.setTicket(this);
    }

    public void clearAndAddLines(List<TicketLineJpaEntity> newLines) {
        lines.clear();
        for (var l : newLines) addLine(l);
    }

}
