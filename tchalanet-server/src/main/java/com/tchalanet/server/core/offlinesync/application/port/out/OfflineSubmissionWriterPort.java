package com.tchalanet.server.core.offlinesync.application.port.out;

import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.offlinesync.domain.model.SalesOfflineDecision;
import com.tchalanet.server.core.offlinesync.domain.model.SalesOfflineRejectReason;

public interface OfflineSubmissionWriterPort {
  void recordSalesDecision(OfflineSaleSubmissionId submissionId, SalesOfflineDecision decision, SalesOfflineRejectReason rejectReason, TicketId salesTicketId);
}
