package com.tchalanet.server.core.sales.api.model.print;

import com.tchalanet.server.common.types.money.Money;
import java.math.BigDecimal;

public record TicketPrintLineCoverage(
    String pricingVariantCode,
    Money stake,
    BigDecimal odds,
    String winMode
) {}
