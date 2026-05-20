package com.tchalanet.server.core.sales.api.model.print;

import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;

public record TicketPrintMetadata(
    Instant placedAt,
    Locale locale,
    ZoneId timezone,
    TicketSaleChannel saleChannel,
    String currency,
    Map<String, String> disclaimers
) {
    public TicketPrintMetadata {
        disclaimers = Map.copyOf(disclaimers);
    }
}
