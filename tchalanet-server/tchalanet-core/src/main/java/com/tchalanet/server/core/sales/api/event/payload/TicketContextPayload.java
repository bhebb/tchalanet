package com.tchalanet.server.core.sales.api.event.payload;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import java.math.BigDecimal;

public record TicketContextPayload(
    DrawId drawId,
    DrawChannelId drawChannelId,
    SellerTerminalId sellerTerminalId,
    BigDecimal sellerCommissionRate,
    BigDecimal sellerCommissionAmount
) {
}
