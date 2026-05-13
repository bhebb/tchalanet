package com.tchalanet.server.features.cashier.operationalcontext;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.TerminalId;

public record ResolveSellerOperationalContextRequest(
    TchRequestContext requestContext,
    TerminalId terminalId,
    SellerOperation operation
) {}
