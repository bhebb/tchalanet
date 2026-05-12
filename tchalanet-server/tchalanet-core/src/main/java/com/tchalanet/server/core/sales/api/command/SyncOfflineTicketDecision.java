package com.tchalanet.server.core.sales.api.command;

import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.offlinesync.internal.domain.model.SalesOfflineDecision;
import com.tchalanet.server.core.offlinesync.internal.domain.model.SalesOfflineRejectReason;

public record SyncOfflineTicketDecision(
    OfflineSaleSubmissionId submissionId,
    SalesOfflineDecision decision,
    SalesOfflineRejectReason rejectReason,
    TicketId ticketId
) {}

