package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "offline_submission_line")
public class OfflineSubmissionLineJpaEntity extends BaseTenantEntity {

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "game_code", nullable = false)
    private String gameCode;

    @Column(name = "bet_type", nullable = false)
    private String betType;

    @Column(name = "bet_option", nullable = false)
    private String betOption;

    @Column(name = "selection_key", nullable = false)
    private String selectionKey;

    @Column(name = "stake_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal stakeAmount;

    @Column(name = "potential_payout", precision = 18, scale = 2)
    private BigDecimal potentialPayout;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "rejection_code")
    private String rejectionCode;

    @Column(name = "rejection_reason")
    private String rejectionReason;
}
