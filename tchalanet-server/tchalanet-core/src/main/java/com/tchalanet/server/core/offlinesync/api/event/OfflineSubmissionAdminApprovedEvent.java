package com.tchalanet.server.core.offlinesync.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OfflineCodeId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.PromotionAttemptId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

import java.time.Instant;

/**
 * Published after-commit by {@code ApproveOfflineSubmissionCommandHandler} when an admin
 * force-approves a previously rejected/business-rejected submission, asking {@code core.sales}
 * to retry the promotion.
 *
 * <p>Self-contained with a fresh {@link PromotionAttemptId} — any returning event from a
 * previous attempt is now stale and ignored.
 */
public record OfflineSubmissionAdminApprovedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    OfflineSubmissionId submissionId,
    OfflineGrantId grantId,
    OfflineCodeId codeId,
    String offlineCode,
    PromotionAttemptId promotionAttemptId,
    UserId approvedBy,
    String approvalReason,
    OfflineSubmissionTicketDraft draft
) implements DomainEvent {}
