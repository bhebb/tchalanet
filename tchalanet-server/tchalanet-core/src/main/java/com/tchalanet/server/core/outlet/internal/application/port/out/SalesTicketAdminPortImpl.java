package com.tchalanet.server.core.outlet.internal.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import org.springframework.stereotype.Service;

import java.time.Instant;


@Service
public class SalesTicketAdminPortImpl implements SalesTicketAdminPort {
    @Override
    public TicketCloseStats getCloseStats(OutletId outletId, Instant from, Instant to) {
        return null;
    }

    @Override
    public void refuseNewTickets(OutletId outletId) {

    }

    @Override
    public void allowNewTickets(OutletId outletId) {

    }
}
