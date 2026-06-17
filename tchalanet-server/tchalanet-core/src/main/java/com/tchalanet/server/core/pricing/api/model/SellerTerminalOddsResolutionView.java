package com.tchalanet.server.core.pricing.api.model;

import java.math.BigDecimal;

/**
 * Resolved odds for a (gameCode, betType, betOption) in the context of a seller_terminal.
 * Carries the source so callers know whether an override was applied.
 */
public record SellerTerminalOddsResolutionView(
    String gameCode,
    String betType,
    Short betOption,
    BigDecimal tenantDefaultOdds,
    BigDecimal sellerTerminalOdds,
    BigDecimal effectiveOdds,
    OddsSource source
) {}
