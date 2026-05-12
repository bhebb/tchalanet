package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineRiskFlag;
import com.tchalanet.server.core.offlinesync.domain.model.SalesOfflineDecision;
import com.tchalanet.server.core.offlinesync.domain.model.SalesOfflineRejectReason;
import java.util.Set;

public interface OfflineSubmissionWriterPort {
  void recordSalesDecision(OfflineSaleSubmissionId submissionId, SalesOfflineDecision decision, SalesOfflineRejectReason rejectReason, TicketId salesTicketId);
  void markTechnicalReject(OfflineSaleSubmissionId submissionId);
  void updateRiskFlags(OfflineSaleSubmissionId submissionId, Set<OfflineRiskFlag> flags);
}
