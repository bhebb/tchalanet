package com.tchalanet.server.core.offlinesync.infra.web.mapper;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.offlinesync.application.command.model.OfflineSaleSubmissionInput;
import com.tchalanet.server.core.offlinesync.application.command.model.ReceiveOfflineBatchCommand;
import com.tchalanet.server.core.offlinesync.application.command.model.ReceiveOfflineBatchResult;
import com.tchalanet.server.core.offlinesync.infra.web.model.ReceiveOfflineBatchRequest;
import com.tchalanet.server.core.offlinesync.infra.web.model.ReceiveOfflineBatchResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OfflineSyncWebMapper {

  default ReceiveOfflineBatchCommand toCommand(TchRequestContext ctx, ReceiveOfflineBatchRequest request) {
    return new ReceiveOfflineBatchCommand(
        ctx.effectiveTenantIdRequired(),
        TerminalId.parse(request.terminalId()),
        OfflineSalesGrantId.parse(request.grantId()),
        OfflineCodeBatchId.parse(request.codeBatchId()),
        request.clientBatchId(),
        request.submissions().stream()
            .map(s -> new OfflineSaleSubmissionInput(
                s.offlineCode(),
                s.clientTicketId(),
                s.localSequence(),
                s.createdAtDevice(),
                s.payloadJson(),
                s.payloadHash(),
                s.signature()))
            .toList());
  }

  default ReceiveOfflineBatchResponse toResponse(ReceiveOfflineBatchResult r) {
    return new ReceiveOfflineBatchResponse(
        r.batchId().value().toString(),
        r.status().name(),
        r.receivedCount(),
        r.readyForSalesCount(),
        r.technicalRejectCount());
  }
}
