package com.tchalanet.server.core.offlinesync.api.query.grant;

import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.offlinesync.api.model.grant.OfflineGrantStatus;

import java.time.Instant;
import java.util.UUID;

/** Read-side view of an offline grant — exposed by tenant/admin endpoints. */
public record OfflineGrantView(
    OfflineGrantId id,
    UserId sellerUserId,
    TerminalId terminalId,
    UUID deviceId,
    OfflineGrantStatus status,
    Instant validFrom,
    Instant validUntil,
    Instant syncAcceptedUntil,
    int maxTicketCount,
    Money maxTotalAmount,
    int consumedTicketCount,
    Money consumedTotalAmount,
    Instant issuedAt
) {}
