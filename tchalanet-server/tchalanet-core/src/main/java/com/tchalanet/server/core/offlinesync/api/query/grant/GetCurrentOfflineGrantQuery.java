package com.tchalanet.server.core.offlinesync.api.query.grant;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

import java.util.UUID;

public record GetCurrentOfflineGrantQuery(
    TenantId tenantId,
    UserId sellerUserId,
    TerminalId terminalId,
    UUID deviceId
) implements Query<OfflineGrantView> {
}
