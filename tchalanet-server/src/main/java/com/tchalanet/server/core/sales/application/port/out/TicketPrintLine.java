package com.tchalanet.server.core.sales.application.port.out;

import com.tchalanet.server.common.types.enums.BetType;
import java.math.BigDecimal;

public record TicketPrintLine(
    String gameCode,
    BetType betType,
    Short betOption,
    String selection,
    BigDecimal stake,
    BigDecimal potentialPayout
) {}
