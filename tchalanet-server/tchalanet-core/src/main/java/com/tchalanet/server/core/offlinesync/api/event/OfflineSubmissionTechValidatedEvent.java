package com.tchalanet.server.core.offlinesync.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OfflineCodeId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.PromotionAttemptId;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;

/**
 * Published after-commit by {@code SyncOfflineSalesCommandHandler} when a submission passes
 * the 15-check {@code OfflineSubmissionTechnicalPolicy}.
 *
 * <p>Self-contained — {@code core.sales} must NOT query back into {@code core.offlinesync}
 * to materialise the ticket; everything is in {@link #draft()}.
 */
public record OfflineSubmissionTechValidatedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    OfflineSubmissionId submissionId,
    OfflineGrantId grantId,
    OfflineCodeId codeId,
    String offlineCode,
    PromotionAttemptId promotionAttemptId,
    OfflineSubmissionTicketDraft draft
) implements DomainEvent {}
