package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.core.offlinesync.internal.domain.model.*;
import org.mapstruct.Mapper;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface OfflineSyncJpaMapper {

  default OfflineSaleSubmission toDomain(OfflineSaleSubmissionJpaEntity e) {
    return new OfflineSaleSubmission(
        OfflineSaleSubmissionId.of(e.getId()),
        TenantId.of(e.getTenantId()),
        OfflineBatchId.of(e.getBatchId()),
        OfflineSalesGrantId.of(e.getGrantId()),
        OfflineCodeBatchId.of(e.getCodeBatchId()),
        e.getOfflineCode(),
        TerminalId.of(e.getTerminalId()),
        OutletId.of(e.getOutletId()),
        UserId.of(e.getSellerUserId()),
        SalesSessionId.of(e.getSalesSessionId()),
        e.getClientTicketId(),
        e.getLocalSequence(),
        e.getCreatedAtDevice(),
        e.getReceivedAt(),
        e.getPayloadJson(),
        e.getPayloadHash(),
        e.getSignature(),
        OfflineSubmissionStatus.valueOf(e.getStatus()),
        e.getTechnicalRejectReason() == null ? null : OfflineTechnicalRejectReason.valueOf(e.getTechnicalRejectReason()),
        e.getSalesDecision() == null ? null : SalesOfflineDecision.valueOf(e.getSalesDecision()),
        e.getSalesRejectReason() == null ? null : SalesOfflineRejectReason.valueOf(e.getSalesRejectReason()),
        parseRiskFlags(e.getRiskFlagsJson()),
        TicketId.nullableOf(e.getSalesTicketId()));
  }

  private static Set<OfflineRiskFlag> parseRiskFlags(String json) {
    if (json == null || json.isBlank() || json.equals("[]")) return Set.of();
    return Arrays.stream(json.replaceAll("[\\[\\]\"\\s]", "").split(","))
        .filter(s -> !s.isEmpty())
        .map(OfflineRiskFlag::valueOf)
        .collect(Collectors.toUnmodifiableSet());
  }
}
