package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.catalog.game.api.model.BetType;
import java.math.BigDecimal;

public record PublicTicketVerificationLineRecord(
    String gameCode,
    BetType betType,
    String selection,
    BigDecimal stake,
    BigDecimal potentialPayout
) {}
