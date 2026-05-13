package com.tchalanet.server.common.context.operational;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;

public record OperationalContextHint(
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    TenantId tenantOverrideId,
    String overrideReason,
    OperationalContextSource source
) {

    public boolean hasPosFrame() {
        return terminalId != null || outletId != null || salesSessionId != null;
    }

    public boolean hasTenantOverride() {
        return tenantOverrideId != null;
    }
}
