package com.tchalanet.server.common.context.operational;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

public record SellerOperationalContext(
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    UserId sellerUserId,
    OperationalContextSource source,
    TrustLevel trustLevel
) {

    public OperationalContextRole role() {
        return OperationalContextRole.SELLER;
    }
}
