package com.tchalanet.server.core.sales.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Audited
@Table(name = "ticket")
public class TicketJpaEntity extends BaseTenantEntity {

    @Column(name = "outlet_id", nullable = false)
    private UUID outletId;

    @Column(name = "terminal_id", nullable = false)
    private UUID terminalId;

    @Column(name = "seller_user_id", nullable = false)
    private UUID sellerUserId;

    @Column(name = "sales_session_id", nullable = false)
    private UUID salesSessionId;

    @Column(name = "draw_id", nullable = false)
    private UUID drawId;

    @Column(name = "draw_channel_id")
    private UUID drawChannelId;

    @Column(name = "ticket_code", nullable = false)
    private String ticketCode;

    @Column(name = "public_code", nullable = false)
    private String publicCode;

    @Column(name = "verification_code", nullable = false)
    private String verificationCode;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "stake_amount", nullable = false)
    private BigDecimal stakeAmount;

    @Column(name = "fee_amount", nullable = false)
    private BigDecimal feeAmount;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "potential_payout_amount", nullable = false)
    private BigDecimal potentialPayoutAmount;

    @Column(name = "winning_amount")
    private BigDecimal winningAmount;

    @Column(name = "sale_status", nullable = false)
    private String saleStatus;

    @Column(name = "result_status", nullable = false)
    private String resultStatus;

    @Column(name = "settlement_status", nullable = false)
    private String settlementStatus;

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
}
