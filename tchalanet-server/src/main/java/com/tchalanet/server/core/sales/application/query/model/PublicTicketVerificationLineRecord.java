package com.tchalanet.server.core.sales.application.query.model;

import com.tchalanet.server.common.types.enums.BetType;
import java.math.BigDecimal;

public record PublicTicketVerificationLineRecord(
    String gameCode,
    BetType betType,
    String selection,
    BigDecimal stake,
    BigDecimal potentialPayout
) {}
