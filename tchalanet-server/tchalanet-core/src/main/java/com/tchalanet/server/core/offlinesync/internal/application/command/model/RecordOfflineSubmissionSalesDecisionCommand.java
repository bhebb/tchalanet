package com.tchalanet.server.core.offlinesync.internal.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.offlinesync.api.model.OfflineSubmissionStatus;
import jakarta.validation.constraints.NotNull;

public record RecordOfflineSubmissionSalesDecisionCommand(
    @NotNull TenantId tenantId,
    @NotNull OfflineSaleSubmissionId submissionId,
    @NotNull OfflineSubmissionStatus decision,
    TicketId ticketId,
    String rejectionCode
) implements Command<Void> {}
