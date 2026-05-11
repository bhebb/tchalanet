package com.tchalanet.server.core.sales.application.command.model.offline;

import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.offlinesync.domain.model.SalesOfflineDecision;
import com.tchalanet.server.core.offlinesync.domain.model.SalesOfflineRejectReason;

public record SyncOfflineTicketDecision(
    OfflineSaleSubmissionId submissionId,
    SalesOfflineDecision decision,
    SalesOfflineRejectReason rejectReason,
    TicketId ticketId
) {}

