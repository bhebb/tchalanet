package com.tchalanet.server.core.limitpolicy.internal.application.port.out.exposure;

import com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent;

public interface ExposureProjectorPort {
    void applyTicketSold(TicketPlacedEvent event);
}
