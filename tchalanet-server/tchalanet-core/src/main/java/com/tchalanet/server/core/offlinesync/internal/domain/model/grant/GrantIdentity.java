package com.tchalanet.server.core.offlinesync.internal.domain.model.grant;

import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

/** Stable identity / operational scope of a grant. */
public record GrantIdentity(
    OfflineGrantId id,
    TenantId tenantId,
    UserId sellerUserId,
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId
) {
    public GrantIdentity {
        if (id == null) throw new IllegalArgumentException("GrantIdentity.id required");
        if (tenantId == null) throw new IllegalArgumentException("tenantId required");
        if (sellerUserId == null) throw new IllegalArgumentException("sellerUserId required");
        if (terminalId == null) throw new IllegalArgumentException("terminalId required");
        if (outletId == null) throw new IllegalArgumentException("outletId required");
        if (salesSessionId == null) throw new IllegalArgumentException("salesSessionId required");
    }
}
