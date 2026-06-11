package com.tchalanet.server.core.sales.api.model.preparation;

import com.tchalanet.server.core.sales.api.command.sell.SellTicketResult;
import java.util.UUID;

/**
 * {@code sale} is null on an idempotent replay ({@code alreadyConfirmed} =
 * true): the same ticketId is returned and the POS can reload/print by id.
 */
public record ConfirmPreparedSaleResult(
    UUID preparationId,
    UUID ticketId,
    boolean alreadyConfirmed,
    SellTicketResult sale
) {}
