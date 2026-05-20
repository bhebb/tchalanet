package com.tchalanet.server.core.sales.api.model.money;

import com.tchalanet.server.platform.communication.api.model.request.CommunicationCostBearer;

/**
 * Which actor bears the cost of a {@link TicketCharge}.
 *
 * <ul>
 *   <li>{@link #BUYER}: included in the ticket's buyer-facing {@code total}.</li>
 *   <li>{@link #SELLER}: deducted from the seller's commission / margin (out of band).</li>
 *   <li>{@link #TENANT}: tenant absorbs the cost as an operating expense.</li>
 * </ul>
 */
public enum ChargePaidBy {
    BUYER,
    SELLER,
    TENANT;

    public static ChargePaidBy fromCommunicationFee(CommunicationCostBearer paidBy) {
        return switch (paidBy) {
            case BUYER -> ChargePaidBy.BUYER;
            case SELLER -> ChargePaidBy.SELLER;
            case TENANT -> ChargePaidBy.TENANT;
        };
    }
}
