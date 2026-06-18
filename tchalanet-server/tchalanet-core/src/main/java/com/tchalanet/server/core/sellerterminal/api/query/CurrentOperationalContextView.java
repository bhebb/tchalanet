package com.tchalanet.server.core.sellerterminal.api.query;

import com.tchalanet.server.common.context.operational.OperationalContextSource;
import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;

public record CurrentOperationalContextView(
    SellerTerminalId sellerTerminalId,
    String terminalCode,
    String displayName,
    SellerTerminalStatus status,
    OperationalContextSource source,
    OperationalContextTrust trust,
    boolean present,
    boolean trustedForSensitiveOperation
) {}
