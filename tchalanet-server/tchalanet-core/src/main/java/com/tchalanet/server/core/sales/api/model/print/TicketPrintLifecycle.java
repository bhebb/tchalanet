package com.tchalanet.server.core.sales.api.model.print;

import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import java.util.Objects;

public record TicketPrintLifecycle(
    TicketSaleStatus saleStatus,
    TicketResultStatus resultStatus,
    TicketSettlementStatus settlementStatus
) {
    public TicketPrintLifecycle {
        Objects.requireNonNull(saleStatus, "saleStatus is required");
        Objects.requireNonNull(resultStatus, "resultStatus is required");
        Objects.requireNonNull(settlementStatus, "settlementStatus is required");
    }
}
