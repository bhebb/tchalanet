package com.tchalanet.server.core.offlinesync.api.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record OfflineSubmissionForSalesView(
    OfflineSaleSubmissionId id,
    TenantId tenantId,
    OfflineSalesGrantId grantId,
    DrawId drawId,
    SalesSessionId sessionId,
    OutletId outletId,
    TerminalId terminalId,
    UserId soldBy,
    long totalAmountCents,
    String currency,
    boolean cutoffExceeded,
    Instant submittedAt
) {}
