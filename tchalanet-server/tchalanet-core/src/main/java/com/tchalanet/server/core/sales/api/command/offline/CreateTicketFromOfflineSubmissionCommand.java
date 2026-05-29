package com.tchalanet.server.core.sales.api.command.offline;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OfflineCodeId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.PromotionAttemptId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.offlinesync.api.event.OfflineSubmissionTicketDraft;
import jakarta.validation.constraints.NotNull;

/**
 * Internal sales command issued by the offline promotionDecision listener to materialise a ticket
 * from a self-contained {@link OfflineSubmissionTicketDraft}.
 *
 * <p>The unique constraint {@code (tenant_id, offline_submission_id)} on {@code sales_ticket}
 * is the ultimate protection against double promotionDecision.
 */
public record CreateTicketFromOfflineSubmissionCommand(
    @NotNull TenantId tenantId,
    @NotNull OfflineSubmissionId submissionId,
    @NotNull OfflineGrantId grantId,
    @NotNull OfflineCodeId codeId,
    @NotNull String offlineCode,
    @NotNull PromotionAttemptId promotionAttemptId,
    @NotNull EventId sourceEventId,
    @NotNull OfflineSubmissionTicketDraft draft
) implements Command<CreateTicketFromOfflineSubmissionResult> {}
