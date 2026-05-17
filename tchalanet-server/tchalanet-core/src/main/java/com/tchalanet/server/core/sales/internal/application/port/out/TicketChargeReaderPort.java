package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.money.TicketCharge;

import java.util.List;

public interface TicketChargeReaderPort {
    List<TicketCharge> findByTicketId(TicketId ticketId);
}
