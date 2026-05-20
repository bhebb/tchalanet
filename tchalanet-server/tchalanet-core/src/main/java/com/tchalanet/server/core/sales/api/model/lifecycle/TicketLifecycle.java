package com.tchalanet.server.core.sales.api.model.lifecycle;

public record TicketLifecycle(SaleLifecycle sale, ResultLifecycle result, SettlementLifecycle settlement) {
}
