package com.tchalanet.server.core.offlinesync.application.command.model;

import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.TicketId;

public record ApproveOfflineSubmissionResult(
    OfflineSaleSubmissionId submissionId,
    TicketId ticketId
) {}
