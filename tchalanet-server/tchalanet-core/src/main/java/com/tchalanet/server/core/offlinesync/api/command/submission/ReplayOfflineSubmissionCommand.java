package com.tchalanet.server.core.offlinesync.api.command.submission;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;

/**
 * Admin dry-run replay: re-evaluates the technical policy without persisting state change.
 * Returns a report describing what would happen, useful before a real {@code Approve}.
 */
public record ReplayOfflineSubmissionCommand(
    @NotNull TenantId tenantId,
    @NotNull OfflineSubmissionId submissionId
) implements Command<ReplayOfflineSubmissionResult> {}
