package com.tchalanet.server.core.offlinesync.api.command.submission;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;

/**
 * Ops/job-invoked: bumps {@code promotionAttemptId} and re-publishes the
 * TechValidated event for a submission that didn't get a return event within the SLA.
 */
public record RecoverStuckOfflineSubmissionCommand(
    @NotNull TenantId tenantId,
    @NotNull OfflineSubmissionId submissionId
) implements Command<Void> {}
