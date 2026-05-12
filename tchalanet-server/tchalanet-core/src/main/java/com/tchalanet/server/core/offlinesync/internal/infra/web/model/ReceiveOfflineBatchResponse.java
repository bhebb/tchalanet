package com.tchalanet.server.core.offlinesync.internal.infra.web.model;

public record ReceiveOfflineBatchResponse(
    String batchId,
    String status,
    int receivedCount,
    int readyForSalesCount,
    int technicalRejectCount
) {}
