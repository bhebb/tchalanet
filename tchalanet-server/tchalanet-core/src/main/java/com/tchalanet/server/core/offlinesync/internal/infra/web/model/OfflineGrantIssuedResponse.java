package com.tchalanet.server.core.offlinesync.internal.infra.web.model;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.core.offlinesync.api.command.grant.OfflineUpcomingDrawSnapshot;

import java.time.Instant;
import java.util.List;

public record OfflineGrantIssuedResponse(
    OfflineGrantId grantId,
    OfflineCodeBatchId codeBatchId,
    Instant validFrom,
    Instant validUntil,
    Instant syncAcceptedUntil,
    int maxTicketCount,
    String currency,
    List<String> offlineCodes,
    String grantSignature,
    String serverPublicKey,
    List<OfflineUpcomingDrawSnapshot> upcomingDraws
) {}
