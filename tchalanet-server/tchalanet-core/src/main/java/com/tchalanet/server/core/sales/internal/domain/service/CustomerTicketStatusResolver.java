package com.tchalanet.server.core.sales.internal.domain.service;

import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.api.model.verification.CustomerTicketStatus;
import org.springframework.stereotype.Component;

@Component
public class CustomerTicketStatusResolver {

    public CustomerTicketStatus resolve(
        TicketSaleStatus saleStatus,
        TicketResultStatus resultStatus,
        TicketSettlementStatus settlementStatus
    ) {
        if (saleStatus == TicketSaleStatus.CANCELLED) return CustomerTicketStatus.CANCELLED;
        if (saleStatus == TicketSaleStatus.VOIDED || saleStatus == TicketSaleStatus.REJECTED) {
            return CustomerTicketStatus.VOIDED;
        }

        if (resultStatus == TicketResultStatus.OVERRIDDEN) return CustomerTicketStatus.CORRECTED;
        if (resultStatus == TicketResultStatus.NOT_RESULTED
                || resultStatus == TicketResultStatus.PENDING) {
            return CustomerTicketStatus.AWAITING_RESULT;
        }
        if (resultStatus == TicketResultStatus.LOST || resultStatus == TicketResultStatus.VOID) {
            return CustomerTicketStatus.LOST;
        }

        if (resultStatus == TicketResultStatus.WON) {
            return switch (settlementStatus) {
                case PAID, SETTLED -> CustomerTicketStatus.WON_PAID;
                case PAYOUT_PENDING, NOT_SETTLED, NO_PAYOUT -> CustomerTicketStatus.WON_CLAIMABLE;
                case REVERSED -> throw new IllegalStateException(
                    "Unexpected settlement status REVERSED for WON ticket");
            };
        }

        throw new IllegalStateException(
            "Unexpected ticket state: sale=" + saleStatus
                + " result=" + resultStatus
                + " settlement=" + settlementStatus);
    }
}
